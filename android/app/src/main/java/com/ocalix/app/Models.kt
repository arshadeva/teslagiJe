package com.ocalix.app

import org.json.JSONObject

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val dur: Int,
    val cover: String,
    val src: String? = null // diisi untuk preview iTunes (URL langsung); YouTube = null -> lewat server
) {
    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id); o.put("title", title); o.put("artist", artist); o.put("dur", dur); o.put("cover", cover)
        if (src != null) o.put("src", src)
        return o
    }

    companion object {
        fun fromJson(o: JSONObject): Track = Track(
            id = o.getString("id"),
            title = o.optString("title"),
            artist = o.optString("artist"),
            dur = o.optInt("dur"),
            cover = o.optString("cover"),
            src = if (o.has("src") && !o.isNull("src")) o.getString("src") else null
        )
    }
}

data class Playlist(val id: String, val name: String, val tracks: List<Track>)

class HistItem(val track: Track, val n: Int)

data class LyricLine(val t: Double, val text: String)

data class Lyrics(val synced: List<LyricLine>, val plain: String) {
    val found: Boolean get() = synced.isNotEmpty() || plain.isNotBlank()
}

class SearchResult(val items: List<Track>, val preview: Boolean)
