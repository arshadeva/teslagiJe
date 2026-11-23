package com.ocalix.app

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Jembatan UI <-> PlaybackService (lewat MediaController). Semua state pemutar dipantau dari sini. */
class PlayerManager(
    private val ctx: Context,
    private val store: Store,
    private val toasts: MutableSharedFlow<String>
) {
    val current = MutableStateFlow<Track?>(null)
    val playing = MutableStateFlow(false)
    val buffering = MutableStateFlow(false)
    val shuffle = MutableStateFlow(false)
    val repeat = MutableStateFlow(0)          // 0 = mati, 1 = ulang semua, 2 = ulang satu
    val position = MutableStateFlow(0L)       // ms
    val duration = MutableStateFlow(0L)       // ms
    val sleepMin = MutableStateFlow(0)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controller: MediaController? = null
    private val pending = ArrayList<(MediaController) -> Unit>()
    private var queue: List<Track> = emptyList()
    private val retried = HashSet<String>()
    private var sleepJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val t = trackOf(mediaItem)
            current.value = t
            position.value = 0
            if (t != null && (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK)) {
                store.recordPlay(t)
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            playing.value = playWhenReady
        }

        override fun onPlaybackStateChanged(state: Int) {
            buffering.value = state == Player.STATE_BUFFERING
            val c = controller ?: return
            playing.value = c.playWhenReady && state != Player.STATE_ENDED
            if (c.duration > 0) duration.value = c.duration
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            shuffle.value = shuffleModeEnabled
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            repeat.value = when (repeatMode) {
                Player.REPEAT_MODE_ALL -> 1
                Player.REPEAT_MODE_ONE -> 2
                else -> 0
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            handleError()
        }
    }

    init {
        val token = SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
        val future = MediaController.Builder(ctx, token).buildAsync()
        future.addListener({
            try {
                val c = future.get()
                controller = c
                c.addListener(listener)
                pending.forEach { it(c) }
                pending.clear()
                syncFrom(c)
            } catch (e: Exception) {
                toasts.tryEmit("Gagal menghubungkan pemutar: " + (e.message ?: ""))
            }
        }, ContextCompat.getMainExecutor(ctx))

        scope.launch {
            while (true) {
                delay(500)
                tick()
            }
        }
    }

    // ---------- helper ----------
    private fun withC(f: (MediaController) -> Unit) {
        val c = controller
        if (c != null) f(c) else pending.add(f)
    }

    private fun toItem(t: Track): MediaItem =
        MediaItem.Builder()
            .setMediaId(t.id)
            .setUri(Api.audioUrl(t))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(t.title)
                    .setArtist(t.artist)
                    .setArtworkUri(Uri.parse(t.cover))
                    .build()
            )
            .build()

    private fun trackOf(item: MediaItem?): Track? {
        if (item == null) return null
        val known = queue.firstOrNull { it.id == item.mediaId }
        if (known != null) return known
        val md = item.mediaMetadata
        return Track(
            id = item.mediaId,
            title = md.title?.toString() ?: "",
            artist = md.artist?.toString() ?: "",
            dur = 0,
            cover = md.artworkUri?.toString() ?: ""
        )
    }

    /** Dipanggil saat aplikasi dibuka lagi sementara service masih memutar lagu. */
    private fun syncFrom(c: MediaController) {
        current.value = trackOf(c.currentMediaItem)
        playing.value = c.playWhenReady && c.playbackState != Player.STATE_ENDED && c.playbackState != Player.STATE_IDLE
        shuffle.value = c.shuffleModeEnabled
        repeat.value = when (c.repeatMode) {
            Player.REPEAT_MODE_ALL -> 1
            Player.REPEAT_MODE_ONE -> 2
            else -> 0
        }
        tick()
    }

    private fun tick() {
        val c = controller ?: return
        position.value = c.currentPosition.coerceAtLeast(0L)
        val d = c.duration
        if (d > 0) duration.value = d
    }

    private fun handleError() {
        val c = controller ?: return
        val item = c.currentMediaItem ?: return
        val uri = item.localConfiguration?.uri
        val viaServer = uri != null && uri.toString().contains("/api/audio")
        if (viaServer && retried.add(item.mediaId)) {
            // percobaan 1: minta server cari link audio baru (&fresh=1)
            toasts.tryEmit("Mencoba ulang lagu…")
            val u = uri!!.buildUpon().appendQueryParameter("fresh", "1").build()
            c.replaceMediaItem(c.currentMediaItemIndex, item.buildUpon().setUri(u).build())
            c.prepare()
            c.play()
        } else if (c.hasNextMediaItem()) {
            toasts.tryEmit("Lagu tidak bisa diputar, lanjut…")
            c.seekToNextMediaItem()
            c.prepare()
            c.play()
        } else {
            playing.value = false
            toasts.tryEmit("Lagu tidak bisa diputar. Cek server / internet (Kamu > Tes audio server)")
        }
    }

    // ---------- perintah dari UI ----------
    fun playQueue(list: List<Track>, index: Int, shuffleOn: Boolean? = null) {
        if (list.isEmpty()) return
        val i = index.coerceIn(0, list.size - 1)
        queue = list
        retried.clear()
        current.value = list[i]
        position.value = 0
        duration.value = list[i].dur * 1000L
        playing.value = true
        store.recordPlay(list[i])
        val items = list.map { toItem(it) }
        withC { c ->
            if (shuffleOn != null) c.shuffleModeEnabled = shuffleOn
            c.setMediaItems(items, i, 0L)
            c.prepare()
            c.play()
        }
    }

    fun playShuffled(list: List<Track>) {
        if (list.isEmpty()) return
        playQueue(list, Random.nextInt(list.size), true)
    }

    fun toggle() = withC { c ->
        if (c.playWhenReady && c.playbackState != Player.STATE_ENDED) c.pause()
        else {
            if (c.playbackState == Player.STATE_ENDED) c.seekToDefaultPosition(0)
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    fun next() = withC { c ->
        if (c.hasNextMediaItem()) c.seekToNextMediaItem() else c.seekTo(0, 0L)
        c.play()
    }

    fun prev() = withC { c ->
        c.seekToPrevious() // >3 dtk: ulang dari awal, selain itu lagu sebelumnya
    }

    fun seekTo(ms: Long) = withC { c -> c.seekTo(ms.coerceAtLeast(0L)); position.value = ms }

    fun toggleShuffle() = withC { c -> c.shuffleModeEnabled = !c.shuffleModeEnabled }

    fun cycleRepeat() = withC { c ->
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun cycleSleep() {
        val o = listOf(0, 15, 30, 60)
        val n = o[(o.indexOf(sleepMin.value) + 1) % o.size]
        sleepMin.value = n
        sleepJob?.cancel()
        if (n > 0) {
            sleepJob = scope.launch {
                delay(n * 60_000L)
                controller?.pause()
                sleepMin.value = 0
                toasts.tryEmit("Timer tidur: musik dihentikan")
            }
        }
    }

    /** Dipanggil saat seluruh aplikasi pindah ke latar belakang. Kalau "Putar di latar belakang" dimatikan -> pause. */
    fun onAppBackground() {
        if (!store.bg.value) controller?.pause()
    }
}
