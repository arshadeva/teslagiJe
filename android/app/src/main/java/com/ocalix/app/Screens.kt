package com.ocalix.app

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Calendar

// ------------------------------------------------------------------ BERANDA
private val SHELVES = listOf(
    "Lagi Tren" to "lagu indonesia terpopuler 2026",
    "Santai Sore" to "lagu santai akustik indonesia",
    "Hits Global" to "top global hits 2026"
)
private val MOODS = listOf(
    "Energize" to "lagu semangat energik", "Feel good" to "lagu feel good ceria",
    "Workout" to "workout gym music", "Sad" to "lagu sedih galau",
    "Chill" to "lagu chill santai", "Fokus" to "lofi study focus",
    "Party" to "party hits dance", "Romantis" to "lagu romantis cinta"
)

@Composable
fun HomeScreen(a: Actions, goSearch: (String, String) -> Unit) {
    val hist by a.store.history.collectAsState()
    val name by a.store.name.collectAsState()
    val days by a.store.days.collectAsState()
    val server by a.store.server.collectAsState()
    var trend by remember { mutableStateOf<List<Track>>(emptyList()) }

    val h = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greet = (if (h < 4) "Selamat malam" else if (h < 11) "Selamat pagi" else if (h < 15) "Selamat siang" else if (h < 19) "Selamat sore" else "Selamat malam") +
        (if (name.isNotBlank()) ", $name" else "")
    val quick = if (hist.size >= 2) hist.map { it.track } else trend
    val top = if (hist.isNotEmpty()) hist.sortedByDescending { it.n }.take(6).map { it.track } else trend.drop(6).take(6)
    val streak = Store.streak(days)
    val today = days.contains(Store.dayKey(Calendar.getInstance()))

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.width(10.dp))
                Text("Ocalix", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("by Oscar Mandala", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
            }
            Text(greet, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 14.dp, bottom = 14.dp, start = 4.dp))
        }
        if (quick.isNotEmpty()) {
            item {
                val six = quick.take(6)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    six.chunked(2).forEachIndexed { r, pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pair.forEachIndexed { c, t ->
                                Row(
                                    Modifier.weight(1f).height(60.dp).glass(18.dp)
                                        .clickable { a.pm.playQueue(six, r * 2 + c) },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Cover(t, Modifier.size(60.dp), 0.dp)
                                    Text(
                                        t.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold, lineHeight = 17.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp)
                                    )
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 14.dp).glass(26.dp).padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (streak > 0) "🔥" else "🕯️", fontSize = 40.sp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$streak", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                        Text("  hari berturut-turut", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                    }
                    Text(
                        if (today) "Mantap! Streak hari ini aman"
                        else if (streak > 0) "Putar satu lagu hari ini agar streak lanjut"
                        else "Putar satu lagu hari ini untuk mulai streak",
                        fontSize = 12.5.sp, color = Color.White.copy(alpha = 0.65f)
                    )
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MOODS) { (n, q) ->
                    Text(
                        n, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.glass(22.dp).clickable { goSearch(q, n) }.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            }
        }
        if (top.isNotEmpty()) {
            item {
                SectionTitle("Andalanmu")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(top) { i, t -> SongCard(t, onClick = { a.pm.playQueue(top, i) }) }
                }
            }
        }
        items(SHELVES) { (title, q) ->
            Shelf(title, q, server, a) { if (title == SHELVES[0].first) trend = it }
        }
        item {
            Text("Ocalix · Dibuat oleh Oscar Mandala", fontSize = 12.sp, color = Color.White.copy(alpha = 0.45f),
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 28.dp))
        }
    }
}

@Composable
private fun Shelf(title: String, query: String, server: String, a: Actions, onLoaded: (List<Track>) -> Unit) {
    var list by remember(query, server) { mutableStateOf<List<Track>?>(null) }
    var failed by remember(query, server) { mutableStateOf(false) }
    var reload by remember(query, server) { mutableIntStateOf(0) }

    LaunchedEffect(query, server, reload) {
        failed = false
        try {
            val r = Api.search(query, reload > 0)
            list = r.items
            if (r.items.isEmpty()) failed = true else onLoaded(r.items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failed = true
        }
    }

    SectionTitle(title)
    val l = list
    if (failed) {
        Column(Modifier.fillMaxWidth().glass(18.dp).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Tidak bisa memuat. Cek alamat server di tab Kamu & pastikan server Pterodactyl hidup.", fontSize = 13.sp, textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.7f))
            TextButton(onClick = { reload++ }) { Text("Coba lagi") }
        }
    } else if (l == null) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(4) { Box(Modifier.size(140.dp).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.1f))) }
        }
    } else {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(l.take(12)) { i, t -> SongCard(t, onClick = { a.pm.playQueue(l.take(12), i) }) }
        }
    }
}

// ------------------------------------------------------------------ CARI
class SearchState {
    var text by mutableStateOf("")
    var results by mutableStateOf<List<Track>>(emptyList())
    var loading by mutableStateOf(false)
    var msg by mutableStateOf("")
}

class SearchReq(val q: String, val label: String?, val nonce: Long = System.nanoTime())

@Composable
fun SearchScreen(a: Actions, ss: SearchState, req: SearchReq?, onConsumed: () -> Unit) {
    val scope = rememberCoroutineScope()
    val recent by a.store.recent.collectAsState()
    val focus = LocalFocusManager.current

    fun run(q: String, label: String? = null) {
        val t = q.trim()
        if (t.isEmpty()) return
        ss.text = label ?: t
        if (label == null) a.store.addRecent(t)
        ss.loading = true
        ss.msg = ""
        scope.launch {
            try {
                val r = Api.search(t)
                ss.results = r.items
                ss.msg = if (r.items.isEmpty()) "Tidak ada hasil / server tanpa akses internet"
                else if (r.preview) "YouTube tidak terjangkau, memakai preview 30 detik" else ""
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ss.results = emptyList()
                ss.msg = "Gagal: " + (e.message ?: "server tidak terjangkau")
            }
            ss.loading = false
        }
    }

    LaunchedEffect(req?.nonce) {
        if (req != null) {
            run(req.q, req.label)
            onConsumed()
        }
    }

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(16.dp)) {
        item {
            Text("Cari", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 4.dp, bottom = 14.dp, top = 6.dp))
            TextField(
                value = ss.text,
                onValueChange = { ss.text = it },
                modifier = Modifier.fillMaxWidth().glass(24.dp),
                singleLine = true,
                placeholder = { Text("Lagu, artis, album…", color = Color.White.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus(); run(ss.text) }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Color.White
                )
            )
            val chips = if (recent.isNotEmpty()) recent else listOf("Hits Indonesia", "Lo-fi", "Pop", "Rock", "Jazz", "Anime", "Akustik")
            LazyRow(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(chips) { c ->
                    Text(c, modifier = Modifier.glass(18.dp).clickable { focus.clearFocus(); run(c) }.padding(horizontal = 16.dp, vertical = 10.dp), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
            if (ss.loading) EmptyText("Mencari…")
            else if (ss.msg.isNotEmpty()) EmptyText(ss.msg)
        }
        itemsIndexed(ss.results) { i, t ->
            TrackRow(t, onClick = { a.pm.playQueue(ss.results, i) }, onAdd = { a.onAdd(t) })
        }
    }
}

// ------------------------------------------------------------------ PUSTAKA
@Composable
fun LibraryScreen(a: Actions, open: String?, setOpen: (String?) -> Unit) {
    val pls by a.store.playlists.collectAsState()
    val hist by a.store.history.collectAsState()
    var dlgNew by remember { mutableStateOf(false) }
    var dlgRename by remember { mutableStateOf(false) }
    var dlgDelete by remember { mutableStateOf(false) }

    val histPl = Playlist("hist", "Riwayat", hist.map { it.track })
    val cur: Playlist? = when (open) {
        null -> null
        "hist" -> histPl
        else -> pls.firstOrNull { it.id == open }
    }

    if (cur == null) {
        // daftar playlist
        val all = pls + histPl
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Text("Pustaka", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
            }
            items(all) { p ->
                Column(Modifier.glass(26.dp).clickable { setOpen(p.id) }.padding(12.dp)) {
                    Mosaic(p.tracks, Modifier.fillMaxWidth())
                    Text(p.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                    Text("${p.tracks.size} lagu", fontSize = 12.5.sp, color = Color.White.copy(alpha = 0.6f))
                }
            }
            item {
                Column(Modifier.glass(26.dp).clickable { dlgNew = true }.padding(12.dp)) {
                    Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Add, null, Modifier.size(42.dp)) }
                    Text("Playlist baru", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                    Text("Tanpa login", fontSize = 12.5.sp, color = Color.White.copy(alpha = 0.6f))
                }
            }
        }
        if (dlgNew) PromptDialog("Playlist baru", "", "Nama playlist", "Buat",
            onOk = { v -> if (v.isNotBlank()) a.store.newPlaylist(v); dlgNew = false }, onDismiss = { dlgNew = false })
        return
    }

    val ro = cur.id == "hist"
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(16.dp)) {
        item {
            Row(
                Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.13f)).clickable { setOpen(null) }.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Pustaka", fontWeight = FontWeight.SemiBold)
            }
            Column(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Mosaic(cur.tracks, Modifier.size(190.dp))
                Text(cur.name, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 14.dp), textAlign = TextAlign.Center)
                Text("${cur.tracks.size} lagu", color = Color.White.copy(alpha = 0.6f))
                Row(Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassButton("Putar", Icons.Filled.PlayArrow, primary = true) { if (cur.tracks.isNotEmpty()) a.pm.playQueue(cur.tracks, 0) }
                    GlassButton("Acak", Icons.Filled.Shuffle) { a.pm.playShuffled(cur.tracks) }
                    if (!ro && cur.id != "fav") {
                        GlassButton("", Icons.Filled.Edit) { dlgRename = true }
                        GlassButton("", Icons.Filled.Delete) { dlgDelete = true }
                    }
                }
            }
            if (cur.tracks.isEmpty()) EmptyText("Kosong. Cari lagu lalu tambahkan ke playlist")
        }
        itemsIndexed(cur.tracks) { i, t ->
            TrackRow(
                t, onClick = { a.pm.playQueue(cur.tracks, i) }, onAdd = { a.onAdd(t) },
                onRemove = if (ro) null else ({ a.store.removeFromPlaylist(cur.id, t.id) })
            )
        }
    }
    if (dlgRename) PromptDialog("Nama baru", cur.name, "", "Simpan",
        onOk = { v -> if (v.isNotBlank()) a.store.renamePlaylist(cur.id, v); dlgRename = false }, onDismiss = { dlgRename = false })
    if (dlgDelete) AlertDialog(
        onDismissRequest = { dlgDelete = false },
        title = { Text("Hapus playlist ini?") },
        confirmButton = { TextButton(onClick = { a.store.deletePlaylist(cur.id); dlgDelete = false; setOpen(null) }) { Text("Hapus") } },
        dismissButton = { TextButton(onClick = { dlgDelete = false }) { Text("Batal") } },
        containerColor = Color(0xFF2A0F3E), titleContentColor = Color.White
    )
}

// ------------------------------------------------------------------ KAMU
@Composable
fun MeScreen(a: Actions) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val name by a.store.name.collectAsState()
    val hist by a.store.history.collectAsState()
    val pls by a.store.playlists.collectAsState()
    val days by a.store.days.collectAsState()
    val plays by a.store.totalPlays.collectAsState()
    val server by a.store.server.collectAsState()
    val bg by a.store.bg.collectAsState()
    var dlg by remember { mutableStateOf("") }
    var diag by remember { mutableStateOf<String?>(null) }

    val artists = hist.groupBy { it.track.artist }.map { (k, v) -> k to v.sumOf { it.n } }.sortedByDescending { it.second }.take(3)

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(16.dp)) {
        item {
            Text("Kamu", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 14.dp))
            Row(Modifier.fillMaxWidth().glass(26.dp).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(60.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFC79DDB), Color(0xFF7B2CBF)))),
                    contentAlignment = Alignment.Center
                ) { Text((if (name.isNotBlank()) name.take(1) else "O").uppercase(), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (name.isNotBlank()) name else "Pendengar Ocalix", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Tanpa login · data tersimpan di perangkat", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                }
                IconButton(onClick = { dlg = "name" }) { Icon(Icons.Filled.Edit, null) }
            }
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("diputar" to plays, "streak" to Store.streak(days), "playlist" to pls.size).forEach { (l, v) ->
                    Column(Modifier.weight(1f).glass(22.dp).padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$v", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                        Text(l, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
            SectionTitle("Artis teratas")
            if (artists.isEmpty()) EmptyText("Putar beberapa lagu dulu")
        }
        itemsIndexed(artists) { i, (nm, n) ->
            SettingRow("${i + 1}.  $nm", "$n kali diputar") {}
        }
        item {
            SectionTitle("Server & latar belakang")
            SettingRow("Alamat server", server) { dlg = "server" }
            SettingRow("Putar di latar belakang", if (bg) "Hidup · musik lanjut saat layar mati / aplikasi ditutup" else "Mati · musik berhenti saat aplikasi ditinggalkan",
                trailing = { Switch(checked = bg, onCheckedChange = { a.store.setBg(it) }) }) { a.store.setBg(!bg) }
            SettingRow("Izinkan abaikan optimasi baterai", "Agar musik tidak dimatikan sistem saat layar lama mati") {
                try {
                    ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (e: Exception) {
                    a.app.toasts.tryEmit("Buka Pengaturan > Aplikasi > Ocalix > Baterai > Tanpa batasan")
                }
            }
            SettingRow("Tes audio server", "Cek kenapa lagu tidak mau diputar") {
                diag = "Menguji… (bisa sampai 1 menit)"
                scope.launch {
                    diag = try {
                        val id = a.pm.current.value?.takeIf { it.src == null && it.id.length == 11 }?.id ?: "dQw4w9WgXcQ"
                        val r = Api.debugAudio(id)
                        (if (r.first) "✅ Server BISA ambil audio" else "❌ Server TIDAK bisa ambil audio") + "\n\n" + r.second.joinToString("\n\n")
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        "Server tidak menjawab / belum pakai backend terbaru (v1.3.0).\n" + (e.message ?: "")
                    }
                }
            }
            SectionTitle("Data")
            SettingRow("Reset semua data", "Hapus semua playlist & riwayat") { dlg = "reset" }
            Text("Ocalix v1.3.0 · Dibuat oleh Oscar Mandala", fontSize = 12.sp, color = Color.White.copy(alpha = 0.45f),
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 28.dp))
        }
    }

    when (dlg) {
        "name" -> PromptDialog("Namamu", name, "Nama panggilan", "Simpan",
            onOk = { v -> a.store.setName(v); dlg = "" }, onDismiss = { dlg = "" })
        "server" -> PromptDialog("Alamat server", server, "http://domain.com:7777", "Simpan & tes",
            onOk = { v ->
                dlg = ""
                a.store.saveServer(v)
                scope.launch {
                    val ok = Api.test(v)
                    a.app.toasts.tryEmit(if (ok) "Terhubung ke server Ocalix ✓" else "Tersimpan, tapi server belum menjawab. Cek alamat/port & server hidup")
                }
            }, onDismiss = { dlg = "" })
        "reset" -> AlertDialog(
            onDismissRequest = { dlg = "" },
            title = { Text("Hapus semua playlist & riwayat?") },
            confirmButton = { TextButton(onClick = { a.store.resetAll(); dlg = ""; a.app.toasts.tryEmit("Data direset") }) { Text("Hapus") } },
            dismissButton = { TextButton(onClick = { dlg = "" }) { Text("Batal") } },
            containerColor = Color(0xFF2A0F3E), titleContentColor = Color.White
        )
    }
    val d = diag
    if (d != null) AlertDialog(
        onDismissRequest = { diag = null },
        title = { Text("Tes audio server") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text(d, fontSize = 12.5.sp, lineHeight = 18.sp) } },
        confirmButton = { TextButton(onClick = { diag = null }) { Text("Tutup") } },
        containerColor = Color(0xFF2A0F3E), titleContentColor = Color.White, textContentColor = Color.White
    )
}

@Composable
private fun SettingRow(title: String, sub: String, trailing: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 10.dp).glass(22.dp).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(sub, fontSize = 12.5.sp, color = Color.White.copy(alpha = 0.6f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) trailing()
    }
}
