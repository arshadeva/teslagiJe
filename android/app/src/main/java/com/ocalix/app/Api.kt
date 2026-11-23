package com.ocalix.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Klien ke backend Node.js (Pterodactyl). Semua logika YouTube/lirik tetap di server. */
object Api {
    @Volatile var base: String = ""

    private val searchCache = HashMap<String, SearchResult>()
    private val lyricCache = HashMap<String, Lyrics>()

    fun normalize(raw: String): String {
        var s = raw.trim().trimEnd('/')
        if (s.isEmpty()) return ""
        if (!s.startsWith("http://") && !s.startsWith("https://")) s = "http://$s"
        return s
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    private fun fetch(path: String, readTimeoutMs: Int = 30000): String {
        if (base.isEmpty()) throw IOException("Alamat server belum diatur")
        val c = URL(base + path).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 15000
            c.readTimeout = readTimeoutMs
            c.setRequestProperty("Accept", "application/json")
            val code = c.responseCode
            if (code != 200) throw IOException("Server membalas HTTP $code")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    /** URL audio yang diputar ExoPlayer. */
    fun audioUrl(t: Track): String = t.src ?: (base + "/api/audio?id=" + enc(t.id))

    suspend fun search(q: String, force: Boolean = false): SearchResult = withContext(Dispatchers.IO) {
        if (!force) {
            val hit = synchronized(searchCache) { searchCache[q] }
            if (hit != null) return@withContext hit
        }
        val j = JSONObject(fetch("/api/search?q=" + enc(q)))
        val arr = j.optJSONArray("items")
        val list = ArrayList<Track>()
        if (arr != null) {
            for (i in 0 until arr.length()) {
                try { list.add(Track.fromJson(arr.getJSONObject(i))) } catch (_: Exception) { }
            }
        }
        val r = SearchResult(list, j.optString("src") == "preview")
        if (list.isNotEmpty()) synchronized(searchCache) { searchCache[q] = r }
        r
    }

    suspend fun lyrics(t: Track, force: Boolean = false): Lyrics = withContext(Dispatchers.IO) {
        if (!force) {
            val hit = synchronized(lyricCache) { lyricCache[t.id] }
            if (hit != null) return@withContext hit
        }
        val path = "/api/lyrics?title=" + enc(t.title) + "&artist=" + enc(t.artist) + "&dur=" + t.dur +
            (if (force) "&fresh=1" else "")
        val j = JSONObject(fetch(path, 45000))
        val synced = ArrayList<LyricLine>()
        val a = j.optJSONArray("synced")
        if (a != null) {
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                synced.add(LyricLine(o.getDouble("t"), o.optString("l")))
            }
        }
        val r = Lyrics(synced, j.optString("plain"))
        if (r.found) synchronized(lyricCache) { lyricCache[t.id] = r }
        r
    }

    /** Cek apakah alamat tsb benar-benar server Ocalix. */
    suspend fun test(raw: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val c = URL(normalize(raw) + "/api/config").openConnection() as HttpURLConnection
            try {
                c.connectTimeout = 8000
                c.readTimeout = 8000
                c.responseCode == 200 && c.inputStream.bufferedReader().use { it.readText() }.contains("Ocalix")
            } finally {
                c.disconnect()
            }
        } catch (e: Exception) {
            false
        }
    }

    /** Diagnosa jalur audio di server (sama seperti "Tes latar belakang" di versi web). */
    suspend fun debugAudio(id: String): Pair<Boolean, List<String>> = withContext(Dispatchers.IO) {
        val j = JSONObject(fetch("/api/audio?dbg=1&id=" + enc(id), 90000))
        val lines = ArrayList<String>()
        val a = j.optJSONArray("lines")
        if (a != null) for (i in 0 until a.length()) lines.add(a.getString(i))
        Pair(j.optBoolean("ok"), lines)
    }
}
