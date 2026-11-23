package com.ocalix.app

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/** Penyimpanan lokal (SharedPreferences): playlist, riwayat, pengaturan. Tanpa login. */
class Store(ctx: Context) {
    private val sp = ctx.getSharedPreferences("ocalix", Context.MODE_PRIVATE)

    val needSetup = MutableStateFlow(!sp.contains("server"))
    val server = MutableStateFlow(sp.getString("server", null) ?: BuildConfig.DEFAULT_SERVER)
    val bg = MutableStateFlow(sp.getBoolean("bg", true))
    val name = MutableStateFlow(sp.getString("name", "") ?: "")
    val totalPlays = MutableStateFlow(sp.getInt("plays", 0))
    val playlists = MutableStateFlow(loadPlaylists())
    val history = MutableStateFlow(loadHistory())
    val days = MutableStateFlow(loadDays())
    val recent = MutableStateFlow(loadRecent())

    // ---------- load ----------
    private fun loadPlaylists(): List<Playlist> {
        val out = ArrayList<Playlist>()
        try {
            val a = JSONArray(sp.getString("pl", "[]"))
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                val ta = o.getJSONArray("t")
                val ts = ArrayList<Track>()
                for (k in 0 until ta.length()) {
                    try { ts.add(Track.fromJson(ta.getJSONObject(k))) } catch (_: Exception) { }
                }
                out.add(Playlist(o.getString("id"), o.getString("name"), ts))
            }
        } catch (_: Exception) { }
        if (out.none { it.id == "fav" }) out.add(0, Playlist("fav", "Favorit", emptyList()))
        return out
    }

    private fun loadHistory(): List<HistItem> {
        val out = ArrayList<HistItem>()
        try {
            val a = JSONArray(sp.getString("hist", "[]"))
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                out.add(HistItem(Track.fromJson(o.getJSONObject("t")), o.optInt("n", 1)))
            }
        } catch (_: Exception) { }
        return out
    }

    private fun loadDays(): List<String> {
        val out = ArrayList<String>()
        try {
            val a = JSONArray(sp.getString("days", "[]"))
            for (i in 0 until a.length()) out.add(a.getString(i))
        } catch (_: Exception) { }
        return out
    }

    private fun loadRecent(): List<String> {
        val out = ArrayList<String>()
        try {
            val a = JSONArray(sp.getString("recent", "[]"))
            for (i in 0 until a.length()) out.add(a.getString(i))
        } catch (_: Exception) { }
        return out
    }

    // ---------- save ----------
    private fun savePlaylists() {
        val a = JSONArray()
        playlists.value.forEach { p ->
            val ta = JSONArray()
            p.tracks.forEach { ta.put(it.toJson()) }
            a.put(JSONObject().put("id", p.id).put("name", p.name).put("t", ta))
        }
        sp.edit().putString("pl", a.toString()).apply()
    }

    private fun saveHistory() {
        val a = JSONArray()
        history.value.forEach { a.put(JSONObject().put("t", it.track.toJson()).put("n", it.n)) }
        val d = JSONArray()
        days.value.forEach { d.put(it) }
        sp.edit().putString("hist", a.toString()).putString("days", d.toString()).putInt("plays", totalPlays.value).apply()
    }

    // ---------- pengaturan ----------
    fun saveServer(raw: String) {
        val s = Api.normalize(raw)
        server.value = s
        Api.base = s
        needSetup.value = false
        sp.edit().putString("server", s).apply()
    }

    fun setBg(v: Boolean) { bg.value = v; sp.edit().putBoolean("bg", v).apply() }
    fun setName(v: String) { name.value = v.trim().take(20); sp.edit().putString("name", name.value).apply() }

    fun addRecent(q: String) {
        recent.value = (listOf(q) + recent.value.filter { it != q }).take(8)
        val a = JSONArray()
        recent.value.forEach { a.put(it) }
        sp.edit().putString("recent", a.toString()).apply()
    }

    // ---------- playlist ----------
    fun isFav(id: String): Boolean = playlists.value.first().tracks.any { it.id == id }

    fun toggleFav(t: Track) {
        playlists.value = playlists.value.mapIndexed { i, p ->
            if (i != 0) p
            else if (p.tracks.any { it.id == t.id }) p.copy(tracks = p.tracks.filter { it.id != t.id })
            else p.copy(tracks = p.tracks + t)
        }
        savePlaylists()
    }

    fun addToPlaylist(pid: String, t: Track) {
        playlists.value = playlists.value.map { p ->
            if (p.id == pid && p.tracks.none { it.id == t.id }) p.copy(tracks = p.tracks + t) else p
        }
        savePlaylists()
    }

    fun removeFromPlaylist(pid: String, trackId: String) {
        playlists.value = playlists.value.map { p ->
            if (p.id == pid) p.copy(tracks = p.tracks.filter { it.id != trackId }) else p
        }
        savePlaylists()
    }

    fun newPlaylist(name: String, first: Track? = null): String {
        val id = "p" + System.currentTimeMillis()
        playlists.value = playlists.value + Playlist(id, name.trim().take(40), if (first != null) listOf(first) else emptyList())
        savePlaylists()
        return id
    }

    fun renamePlaylist(pid: String, name: String) {
        if (pid == "fav") return
        playlists.value = playlists.value.map { if (it.id == pid) it.copy(name = name.trim().take(40)) else it }
        savePlaylists()
    }

    fun deletePlaylist(pid: String) {
        if (pid == "fav") return
        playlists.value = playlists.value.filter { it.id != pid }
        savePlaylists()
    }

    // ---------- riwayat & streak ----------
    fun recordPlay(t: Track) {
        val old = history.value.firstOrNull { it.track.id == t.id }
        history.value = (listOf(HistItem(t, (old?.n ?: 0) + 1)) + history.value.filter { it.track.id != t.id }).take(80)
        totalPlays.value = totalPlays.value + 1
        val k = dayKey(Calendar.getInstance())
        if (!days.value.contains(k)) days.value = (days.value + k).takeLast(90)
        saveHistory()
    }

    fun resetAll() {
        playlists.value = listOf(Playlist("fav", "Favorit", emptyList()))
        history.value = emptyList()
        days.value = emptyList()
        totalPlays.value = 0
        recent.value = emptyList()
        savePlaylists(); saveHistory()
        sp.edit().putString("recent", "[]").apply()
    }

    companion object {
        fun dayKey(c: Calendar): String =
            c.get(Calendar.YEAR).toString() + "-" + (c.get(Calendar.MONTH) + 1) + "-" + c.get(Calendar.DAY_OF_MONTH)

        fun streak(days: List<String>): Int {
            val s = days.toSet()
            val c = Calendar.getInstance()
            var n = 0
            if (!s.contains(dayKey(c))) c.add(Calendar.DAY_OF_YEAR, -1)
            while (s.contains(dayKey(c))) { n++; c.add(Calendar.DAY_OF_YEAR, -1) }
            return n
        }
    }
}
