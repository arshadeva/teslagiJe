# Ocalix — versi Kotlin (Android) + backend Node.js

```
ocalix/
├─ server/    ← backend Node.js (upload ke Pterodactyl)   — v1.3.0, sudah ada endpoint LIRIK
└─ android/   ← aplikasi Kotlin (Jetpack Compose + Media3) — buka di Android Studio
```

## 1. Backend di Pterodactyl
1. Upload isi folder `server/` (index.js, lyrics.js, config.js, package.json, folder public) ke server panel.
2. Startup: `node index.js` (Node 18+ disarankan). Tidak perlu `npm install` (tanpa dependency).
3. Port otomatis ikut Allocation panel (`SERVER_PORT`). Kalau mau manual, ubah `PORT` di `config.js`.
4. Tes dari browser: `http://DOMAIN:PORT/api/config` harus menjawab `{"name":"Ocalix","version":"1.3.0",...}`.
   Web lamanya (`http://DOMAIN:PORT/`) tetap jalan seperti dulu.

Endpoint baru: `GET /api/lyrics?title=&artist=&dur=` → `{synced:[{t,l}], plain, src}` (LRCLIB, cadangan lyrics.ovh).
`/api/audio` kini menerima `&fresh=1` (dipakai aplikasi untuk coba ulang otomatis).

## 2. Aplikasi Android
**Android Studio:** buka folder `android/` → edit `DEFAULT_SERVER` di `app/build.gradle.kts`
(atau biarkan, aplikasi akan menanyakan alamat server saat pertama dibuka) → Build > Build APK.

**Tanpa PC:** upload folder `android/` ke repo GitHub → tab Actions → "Build APK" → unduh artifact `Ocalix-debug-apk`.

Alamat server diisi seperti `http://domain.com:7777` (HTTP biasa diizinkan).

## Fitur
- Cari, Beranda (rak lagu, mood, streak), Pustaka (playlist, Favorit, Riwayat), Kamu — logic sama dengan versi web.
- **Lirik** di setiap lagu: tab "Lirik" di pemutar; tersinkron & auto-scroll, ketuk baris untuk loncat; jika hanya ada teks biasa ditampilkan biasa.
- **Putar di latar belakang** (native): foreground service + MediaSession → lanjut saat layar mati / app di-swipe,
  kontrol di notifikasi & lock screen, auto-pause saat headset dicabut, timer tidur.
  Saklar "Putar di latar belakang" dan tombol optimasi baterai ada di tab Kamu.
- Tes audio server (diagnosa) di tab Kamu.

## Belum dipindah dari versi web
Poster Studio (canvas) belum dibuat ulang di Kotlin; Cadangkan/Pulihkan file JSON juga belum.
