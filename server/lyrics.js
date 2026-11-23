// Ocalix · pencari lirik (LRCLIB utama + lyrics.ovh cadangan) — tanpa dependency
// Dipakai oleh index.js:  const LY = require('./lyrics.js')(get)
module.exports = function (get) {
  const UA = 'Ocalix/1.3.0 (music player)';
  const cache = new Map(), inflight = new Map();
  const enc = encodeURIComponent;
  const json = r => { if (r.code !== 200) throw new Error('http ' + r.code); return JSON.parse(r.buf.toString()); };

  // buang sampah judul YouTube: (Official Video), [Lyrics], dll
  function clean(s) {
    return String(s || '')
      .replace(/\([^)]*\)|\[[^\]]*\]|【[^】]*】/g, ' ')
      .replace(/\b(official|music video|lyric video|video klip|klip resmi|lyrics?|lirik|audio|visualizer|mv|hd|4k|remastered|ft\.?|feat\.?)\b.*$/i, ' ')
      .replace(/\s+/g, ' ').trim();
  }
  const tok = s => clean(s).toLowerCase().replace(/[^\p{L}\p{N}\s]/gu, ' ').split(/\s+/).filter(Boolean);

  // "[01:23.45] teks"  ->  {t:83.45, l:'teks'}
  function parseLrc(txt) {
    const out = [];
    String(txt || '').split(/\r?\n/).forEach(line => {
      const m = line.match(/^((?:\s*\[\d{1,3}:\d{1,2}(?:[.:]\d{1,3})?\])+)(.*)$/);
      if (!m) return;
      const text = m[2].replace(/<\d+:\d+(?:[.:]\d+)?>/g, '').trim();
      Array.from(m[1].matchAll(/\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?\]/g)).forEach(x => {
        const frac = x[3] ? parseInt(x[3], 10) / Math.pow(10, x[3].length) : 0;
        out.push({ t: +(parseInt(x[1], 10) * 60 + parseInt(x[2], 10) + frac).toFixed(2), l: text });
      });
    });
    out.sort((a, b) => a.t - b.t);
    return out;
  }

  function shape(o, src) {
    const synced = parseLrc(o && o.syncedLyrics);
    let plain = String((o && o.plainLyrics) || '').trim();
    if (!plain && synced.length) plain = synced.map(x => x.l).join('\n');
    return (synced.length || plain) ? { synced, plain, src } : null;
  }

  async function lrcGet(track, artist, dur) {
    if (!track) return null;
    const r = await get('https://lrclib.net/api/get?track_name=' + enc(track) + '&artist_name=' + enc(artist || '') +
      (dur > 40 ? '&duration=' + Math.round(dur) : ''), { 'User-Agent': UA });
    if (r.code === 404) return null;
    return shape(json(r), 'lrclib');
  }

  async function lrcSearch(q, title, dur) {
    if (!q) return null;
    const list = json(await get('https://lrclib.net/api/search?q=' + enc(q), { 'User-Agent': UA }));
    const want = tok(title);
    let best = null, bs = 1e9;
    for (const x of list || []) {
      if (!x || x.instrumental || !(x.syncedLyrics || x.plainLyrics)) continue;
      const have = tok(x.trackName);
      const hit = want.length ? want.filter(w => have.includes(w)).length / want.length : 1;
      if (hit < 0.5) continue;                                   // judul tidak mirip -> lewati
      const dd = dur > 40 && x.duration ? Math.abs(x.duration - dur) : 0;
      if (dd > 40) continue;                                      // durasi beda jauh -> lewati
      const s = (x.syncedLyrics ? 0 : 500) + dd + (1 - hit) * 100;
      if (s < bs) { bs = s; best = x; }
    }
    return best ? shape(best, 'lrclib') : null;
  }

  async function ovh(artist, title) {
    if (!artist || !title) return null;
    const j = json(await get('https://api.lyrics.ovh/v1/' + enc(artist) + '/' + enc(title)));
    const p = String(j.lyrics || '').replace(/\r/g, '').trim();
    return p ? { synced: [], plain: p, src: 'lyrics.ovh' } : null;
  }

  async function search(title, artist, dur) {
    const c = clean(title), a = clean(artist);
    const m = String(title).match(/^(.+?)\s[-–—]\s(.+)$/);        // "Artis - Judul"
    const sp = m ? { a: clean(m[1]), t: clean(m[2]) } : null;
    const steps = [
      () => lrcGet(c, a, dur),
      () => sp && lrcGet(sp.t, sp.a, dur),
      () => lrcSearch(sp ? sp.a + ' ' + sp.t : a + ' ' + c, sp ? sp.t : c, dur),
      () => lrcSearch(c, c, dur),
      () => sp && lrcSearch(sp.t, sp.t, dur),
      () => ovh(a, c),
      () => sp && ovh(sp.a, sp.t)
    ];
    for (const st of steps) {
      try { const r = await st(); if (r) return r; } catch (e) { /* lanjut ke cara berikutnya */ }
    }
    return { synced: [], plain: '', src: '' };
  }

  function find(title, artist, dur, fresh) {
    const k = (clean(title) + '|' + clean(artist)).toLowerCase();
    const c = cache.get(k);
    if (!fresh && c && Date.now() < c.exp) return Promise.resolve(c.d);
    if (inflight.has(k)) return inflight.get(k);
    const p = search(title, artist, dur).then(d => {
      const found = d.synced.length || d.plain;
      cache.set(k, { d, exp: Date.now() + (found ? 864e5 : 6e5) });
      if (cache.size > 500) cache.delete(cache.keys().next().value);
      return d;
    }).finally(() => inflight.delete(k));
    inflight.set(k, p);
    return p;
  }

  return { find, parseLrc, clean };
};
