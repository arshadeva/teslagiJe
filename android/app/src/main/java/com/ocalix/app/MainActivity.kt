package com.ocalix.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Violet, onPrimary = InkDark,
                    background = Color(0xFF0D0214), surface = Color(0xFF2A0F3E), onSurface = Color.White
                )
            ) {
                Surface(color = Color.Transparent, contentColor = Color.White) { OcalixRoot() }
            }
        }
    }
}

private class NavTab(val label: String, val icon: ImageVector)

@Composable
fun OcalixRoot() {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as OcalixApp
    val scope = rememberCoroutineScope()
    val cur by app.player.current.collectAsState()
    val needSetup by app.store.needSetup.collectAsState()

    var tab by remember { mutableIntStateOf(0) }
    var full by remember { mutableStateOf(false) }
    var addTrack by remember { mutableStateOf<Track?>(null) }
    var libOpen by remember { mutableStateOf<String?>(null) }
    var req by remember { mutableStateOf<SearchReq?>(null) }
    var skipSetup by remember { mutableStateOf(false) }
    val ss = remember { SearchState() }
    val actions = remember { Actions(app, onAdd = { addTrack = it }, openPlayer = { full = true }) }

    // izin notifikasi (Android 13+) agar kontrol musik muncul di shade / lock screen
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    LaunchedEffect(Unit) {
        app.toasts.collect { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() }
    }

    BackHandler(enabled = full) { full = false }
    BackHandler(enabled = !full && tab == 2 && libOpen != null) { libOpen = null }
    BackHandler(enabled = !full && tab != 0 && !(tab == 2 && libOpen != null)) { tab = 0 }

    Box(Modifier.fillMaxSize().background(AppBg)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    0 -> HomeScreen(actions) { q, label -> req = SearchReq(q, label); tab = 1 }
                    1 -> SearchScreen(actions, ss, req) { req = null }
                    2 -> LibraryScreen(actions, libOpen) { libOpen = it }
                    else -> MeScreen(actions)
                }
            }
            if (cur != null) MiniPlayer(actions)
            BottomBar(tab) { tab = it }
        }

        AnimatedVisibility(
            visible = full && cur != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            NowPlaying(actions) { full = false }
        }
    }

    val at = addTrack
    if (at != null) AddDialog(at, actions) { addTrack = null }

    if (needSetup && !skipSetup) {
        PromptDialog(
            title = "Alamat server Ocalix",
            initial = BuildConfig.DEFAULT_SERVER,
            hint = "http://domain.com:7777",
            confirm = "Hubungkan",
            onOk = { v ->
                app.store.saveServer(v)
                scope.launch {
                    val ok = Api.test(v)
                    app.toasts.tryEmit(if (ok) "Terhubung ke server Ocalix ✓" else "Tersimpan, tapi server belum menjawab. Cek alamat/port")
                }
            },
            onDismiss = { skipSetup = true }
        )
    }
}

@Composable
private fun MiniPlayer(a: Actions) {
    val t by a.pm.current.collectAsState()
    val playing by a.pm.playing.collectAsState()
    val pos by a.pm.position.collectAsState()
    val dur by a.pm.duration.collectAsState()
    val pls by a.store.playlists.collectAsState()
    val track = t ?: return
    val fav = pls.firstOrNull()?.tracks?.any { it.id == track.id } == true
    val d = if (dur > 0) dur else track.dur * 1000L
    val frac = if (d > 0) (pos.toFloat() / d.toFloat()).coerceIn(0f, 1f) else 0f

    Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)) {
        Row(
            Modifier.fillMaxWidth().glass(30.dp).background(Color(0xFF2A0F3E).copy(alpha = 0.85f)).clickable { a.openPlayer() }.padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Cover(track, Modifier.size(48.dp), 16.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.5.sp, color = Color.White.copy(alpha = 0.6f))
            }
            IconButton(onClick = { a.store.toggleFav(track) }) {
                Icon(if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "Favorit")
            }
            Box(Modifier.size(46.dp).clip(CircleShape).background(Color.White).clickable { a.pm.toggle() }, contentAlignment = Alignment.Center) {
                Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, Modifier.size(26.dp), tint = InkDark)
            }
            IconButton(onClick = { a.pm.next() }) { Icon(Icons.Filled.SkipNext, "Berikutnya", Modifier.size(28.dp)) }
        }
        // progres tipis
        Box(Modifier.align(Alignment.BottomCenter).padding(horizontal = 22.dp, vertical = 5.dp).fillMaxWidth().height(3.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.18f))) {
            Box(Modifier.fillMaxWidth(frac).height(3.dp).background(Color.White))
        }
    }
}

@Composable
private fun BottomBar(sel: Int, onSel: (Int) -> Unit) {
    val tabs = remember {
        listOf(
            NavTab("Beranda", Icons.Filled.Home), NavTab("Cari", Icons.Filled.Search),
            NavTab("Pustaka", Icons.Filled.LibraryMusic), NavTab("Kamu", Icons.Filled.Person)
        )
    }
    Row(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp)
            .glass(34.dp).background(Color(0xFF2A0F3E).copy(alpha = 0.85f)).padding(6.dp)
    ) {
        tabs.forEachIndexed { i, t ->
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(28.dp))
                    .background(if (i == sel) Color.White.copy(alpha = 0.2f) else Color.Transparent)
                    .clickable { onSel(i) }.padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(t.icon, t.label, Modifier.size(24.dp).alpha(if (i == sel) 1f else 0.6f))
                Text(t.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = if (i == sel) 1f else 0.6f))
            }
        }
    }
}

@Composable
private fun AddDialog(t: Track, a: Actions, onDone: () -> Unit) {
    val pls by a.store.playlists.collectAsState()
    var creating by remember { mutableStateOf(false) }
    if (creating) {
        PromptDialog("Playlist baru", "", "Nama playlist", "Buat",
            onOk = { v ->
                if (v.isNotBlank()) { a.store.newPlaylist(v, t); a.app.toasts.tryEmit("Playlist dibuat") }
                onDone()
            }, onDismiss = { creating = false })
    } else {
        AlertDialog(
            onDismissRequest = onDone,
            title = { Text("Tambah ke playlist") },
            text = {
                Column {
                    pls.forEach { p ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable {
                                a.store.addToPlaylist(p.id, t)
                                a.app.toasts.tryEmit("Ditambahkan ke " + p.name)
                                onDone()
                            }.padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(p.name, fontWeight = FontWeight.SemiBold)
                                Text("${p.tracks.size} lagu", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { creating = true }) { Text("+ Playlist baru") } },
            dismissButton = { TextButton(onClick = onDone) { Text("Batal") } },
            containerColor = Color(0xFF2A0F3E), titleContentColor = Color.White, textContentColor = Color.White
        )
    }
}
