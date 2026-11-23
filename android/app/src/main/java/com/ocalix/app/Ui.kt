package com.ocalix.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

val Violet = Color(0xFFD9B3FF)
val InkDark = Color(0xFF1A0626)
val AppBg = Brush.verticalGradient(listOf(Color(0xFF3A0F55), Color(0xFF1A0626), Color(0xFF0D0214)))

/** Bantuan UI yang dioper ke semua layar. */
class Actions(
    val app: OcalixApp,
    val onAdd: (Track) -> Unit,       // buka dialog "Tambah ke playlist"
    val openPlayer: () -> Unit
) {
    val store: Store get() = app.store
    val pm: PlayerManager get() = app.player
}

fun Modifier.glass(radius: Dp = 22.dp): Modifier {
    val s = RoundedCornerShape(radius)
    return this.clip(s).background(Color.White.copy(alpha = 0.12f)).border(1.dp, Color.White.copy(alpha = 0.22f), s)
}

fun fmt(ms: Long): String {
    val s = (ms.coerceAtLeast(0L) / 1000L).toInt()
    return "" + (s / 60) + ":" + (s % 60).toString().padStart(2, '0')
}

/** Sampul lagu. big=true -> pakai gambar besar & potong bar hitam thumbnail YouTube (sama seperti versi web). */
@Composable
fun Cover(t: Track, modifier: Modifier = Modifier, radius: Dp = 14.dp, big: Boolean = false) {
    val url = if (big) t.cover else t.cover.replace("/hqdefault.", "/mqdefault.").replace("600x600", "200x200")
    val crop = big && t.cover.contains("ytimg")
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(Color.White.copy(alpha = 0.12f))
            .graphicsLayer { if (crop) { scaleX = 1.34f; scaleY = 1.34f } }
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(text, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 10.dp))
}

@Composable
fun TrackRow(t: Track, onClick: () -> Unit, onAdd: () -> Unit, onRemove: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .glass(22.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(t, Modifier.size(52.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
            Text(
                t.artist + " · " + fmt(t.dur * 1000L), maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = Color.White.copy(alpha = 0.6f), fontSize = 12.5.sp
            )
        }
        IconButton(onClick = onAdd) { Icon(Icons.Filled.Add, contentDescription = "Tambah ke playlist") }
        if (onRemove != null) IconButton(onClick = onRemove) { Icon(Icons.Filled.Close, contentDescription = "Hapus") }
    }
}

@Composable
fun SongCard(t: Track, onClick: () -> Unit, width: Dp = 140.dp) {
    Column(Modifier.width(width).clickable(onClick = onClick)) {
        Box {
            Cover(t, Modifier.size(width), 20.dp)
            Box(
                Modifier.align(Alignment.BottomEnd).padding(8.dp).size(32.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.28f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp)) }
        }
        Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
        Text(t.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.5.sp, color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
fun GlassButton(text: String, icon: ImageVector? = null, primary: Boolean = false, onClick: () -> Unit) {
    val s = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .clip(s)
            .background(if (primary) Color.White else Color.White.copy(alpha = 0.13f))
            .border(1.dp, Color.White.copy(alpha = 0.24f), s)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        val c = if (primary) InkDark else Color.White
        if (icon != null) Icon(icon, null, Modifier.size(18.dp), tint = c)
        if (text.isNotEmpty()) Text(text, color = c, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun Mosaic(tracks: List<Track>, modifier: Modifier = Modifier) {
    Box(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF7B2CBF), Color(0xFF3A0F55)))),
        contentAlignment = Alignment.Center
    ) {
        val c = tracks.take(4)
        when {
            c.isEmpty() -> Icon(Icons.Filled.LibraryMusic, null, Modifier.size(42.dp))
            c.size < 4 -> Cover(c[0], Modifier.fillMaxSize(), 0.dp)
            else -> Column(Modifier.fillMaxSize()) {
                Row(Modifier.weight(1f)) {
                    Cover(c[0], Modifier.weight(1f).fillMaxHeight(), 0.dp)
                    Cover(c[1], Modifier.weight(1f).fillMaxHeight(), 0.dp)
                }
                Row(Modifier.weight(1f)) {
                    Cover(c[2], Modifier.weight(1f).fillMaxHeight(), 0.dp)
                    Cover(c[3], Modifier.weight(1f).fillMaxHeight(), 0.dp)
                }
            }
        }
    }
}

@Composable
fun PromptDialog(
    title: String,
    initial: String,
    hint: String = "",
    confirm: String = "Simpan",
    onOk: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var v by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = v, onValueChange = { v = it }, singleLine = true, placeholder = { Text(hint) }, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(onClick = { onOk(v) }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
        containerColor = Color(0xFF2A0F3E),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

@Composable
fun EmptyText(text: String) {
    Text(text, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.fillMaxWidth().padding(14.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
}
