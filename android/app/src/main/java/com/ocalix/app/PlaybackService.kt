package com.ocalix.app

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * MODE PUTAR DI LATAR BELAKANG.
 * ExoPlayer hidup di dalam foreground service + MediaSession, jadi musik terus jalan
 * saat layar mati / aplikasi ditutup dari recent / pindah aplikasi, dan ada notifikasi
 * kontrol (play, pause, next, prev, seekbar) di shade & lock screen.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        // Server Pterodactyl kadang butuh beberapa detik untuk mencari link audio -> timeout dilonggarkan
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent("Ocalix/1.3.0")
            .setConnectTimeoutMs(30000)
            .setReadTimeoutMs(40000)
            .setAllowCrossProtocolRedirects(true)

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(http))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)   // headset dicabut -> pause
            .setWakeMode(C.WAKE_MODE_NETWORK)    // CPU + WiFi tetap hidup saat layar mati
            .build()

        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        session = MediaSession.Builder(this, player).setSessionActivity(open).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Aplikasi di-swipe dari recent: kalau lagi memutar, TETAP lanjut (mode latar belakang); kalau tidak, matikan service
        val p = session?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }
}
