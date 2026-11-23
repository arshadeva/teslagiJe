package com.ocalix.app

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException

@Composable
fun NowPlaying(a: Actions, onClose: () -> Unit) {
    val pm = a.pm
    val t by pm.current.collectAsState()
    val track = t ?: return
    val playing by pm.playing.collectAsState()
    val buffering by pm.buffering.collectAsState()
    val shuffle by pm.shuffle.collectAsState()
    val repeat by pm.repeat.collectAsState()
    val pos by pm.position.collectAsState()
    val dur by pm.duration.collectAsState()
    val sleep by pm.sleepMin.collectAsState()
    val pls by a.store.playlists.collectAsState()
    val bg by a.store.bg.collectAsState()

    val fav = pls.firstOrNull()?.tracks?.any { it.id == track.id } == true
    var tab by remember { mutableIntStateOf(0) }          // 0 = sampul, 1 = lirik
    var dragging by remember { mutableStateOf(false) }
    var dragV by remember { mutableFloatStateOf(0f) }
    val durMs = if (dur > 0) dur else track.dur * 1000L
    val frac = if (durMs > 0) (pos.toFloat() / durMs.toFloat()).coerceIn(0f, 1f) else 0f

    Box(Modifier.fillMaxSize().background(Color(0xFF12041C))) {
        // latar: sampul diburam (Android 12+), lainnya gradasi gelap
        if (Build.VERSION.SDK_INT >= 31) {
            AsyncImage(
                model = track.cover, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(80.dp).alpha(0.45f)
            )
        }
        Box(Modifier.fillMaxSize().background(AppBg).alpha(0.82f))

        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ---- bar atas
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Tutup", modifier = Modifier.size(30.dp)) }
                Spacer(Modifier.weight(1f))
                Row(Modifier.clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.12f)).padding(4.dp)) {
                    listOf("Sampul", "Lirik").forEachIndexed { i, label ->
                        Text(
                            label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (tab == i) Color.White.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable { tab = i }
                                .padding(horizontal = 18.dp, vertical = 8.dp)
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(48.dp))
            }

            // ---- isi: sampul / lirik
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (tab == 0) {
                    Cover(track, Modifier.fillMaxWidth(0.86f).aspectRatio(1f), 32.dp, big = true)
                } else {
                    LyricsView(track, pm, Modifier.fillMaxSize())
                }
            }

            // ---- judul
            Text(track.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
            Text(track.artist, color = Color.White.copy(alpha = 0.65f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            Text(
                if (track.src != null) "Preview 30 detik"
                else if (bg) "● Latar belakang aktif — lanjut saat layar mati"
                else "○ Berhenti saat aplikasi ditinggalkan (hidupkan di tab Kamu)",
                fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
            )

            // ---- seekbar
            Slider(
                value = if (dragging) dragV else frac,
                onValueChange = { dragging = true; dragV = it },
                onValueChangeFinished = { pm.seekTo((dragV * durMs).toLong()); dragging = false },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth()) {
                Text(fmt(if (dragging) (dragV * durMs).toLong() else pos), fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                Spacer(Modifier.weight(1f))
                Text(fmt(durMs), fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
            }

            // ---- kontrol
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { pm.toggleShuffle() }) {
                    Icon(Icons.Filled.Shuffle, "Acak", Modifier.size(24.dp).alpha(if (shuffle) 1f else 0.5f))
                }
                IconButton(onClick = { pm.prev() }, modifier = Modifier.size(54.dp)) { Icon(Icons.Filled.SkipPrevious, "Sebelumnya", Modifier.size(34.dp)) }
                Box(
                    Modifier.size(72.dp).clip(CircleShape).background(Color.White).clickable { pm.toggle() },
                    contentAlignment = Alignment.Center
                ) {
                    if (buffering && playing) CircularProgressIndicator(Modifier.size(34.dp), color = InkDark, strokeWidth = 3.dp)
                    else Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (playing) "Jeda" else "Putar", Modifier.size(38.dp), tint = InkDark)
                }
                IconButton(onClick = { pm.next() }, modifier = Modifier.size(54.dp)) { Icon(Icons.Filled.SkipNext, "Berikutnya", Modifier.size(34.dp)) }
                IconButton(onClick = { pm.cycleRepeat() }) {
                    Icon(if (repeat == 2) Icons.Filled.RepeatOne else Icons.Filled.Repeat, "Ulangi", Modifier.size(24.dp).alpha(if (repeat > 0) 1f else 0.5f))
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { a.store.toggleFav(track) }) {
                    Icon(if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "Favorit", Modifier.size(26.dp))
                }
                Spacer(Modifier.width(8.dp))
                GlassButton("Playlist", Icons.Filled.Add) { a.onAdd(track) }
                Spacer(Modifier.width(8.dp))
                GlassButton(if (sleep > 0) "$sleep mnt" else "Tidur", Icons.Filled.Bedtime) { pm.cycleSleep() }
            }
        }
    }
}

/** Lirik: tersinkron (disorot & auto-scroll mengikuti lagu, ketuk baris untuk loncat) atau teks biasa. */
@Composable
fun LyricsView(t: Track, pm: PlayerManager, modifier: Modifier = Modifier) {
    var ly by remember(t.id) { mutableStateOf<Lyrics?>(null) }
    var failed by remember(t.id) { mutableStateOf(false) }
    var retry by remember(t.id) { mutableIntStateOf(0) }
    val pos by pm.position.collectAsState()

    LaunchedEffect(t.id, retry) {
        ly = null
        failed = false
        try {
            ly = Api.lyrics(t, retry > 0)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failed = true
        }
    }

    val l = ly
    Box(modifier, contentAlignment = Alignment.Center) {
        when {
            failed -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Gagal memuat lirik. Cek koneksi ke server.", color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                TextButton(onClick = { retry++ }) { Text("Coba lagi") }
            }
            l == null -> CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(34.dp))
            l.synced.isNotEmpty() -> {
                val lines = l.synced
                val sec = pos / 1000.0 + 0.3
                val idx = lines.indexOfLast { it.t <= sec }
                val state = rememberLazyListState()
                LaunchedEffect(idx) {
                    if (idx >= 0) state.animateScrollToItem((idx - 2).coerceAtLeast(0))
                }
                LazyColumn(Modifier.fillMaxSize(), state = state, contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 24.dp)) {
                    itemsIndexed(lines) { i, line ->
                        val on = i == idx
                        Text(
                            if (line.text.isBlank()) "♪" else line.text,
                            fontSize = if (on) 24.sp else 20.sp,
                            lineHeight = if (on) 31.sp else 26.sp,
                            fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = Color.White.copy(alpha = if (on) 1f else 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { pm.seekTo((line.t * 1000).toLong()) }
                                .padding(horizontal = 4.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            l.plain.isNotBlank() -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 16.dp)) {
                Text(l.plain, fontSize = 18.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.92f))
                Text("Lirik tanpa sinkronisasi waktu", fontSize = 11.sp, color = Color.White.copy(alpha = 0.45f), modifier = Modifier.padding(top = 18.dp))
            }
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Lirik untuk lagu ini belum ditemukan.", color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                TextButton(onClick = { retry++ }) { Text("Cari ulang") }
            }
        }
    }
}
