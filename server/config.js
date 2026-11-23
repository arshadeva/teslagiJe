// ===== KONFIGURASI OCALIX =====
// Ubah PORT sesuai port Allocation di panel Pterodactyl kamu.
module.exports = {
  HOST: '0.0.0.0',        // jangan diubah (wajib agar bisa diakses dari luar panel)
  PORT: Number(process.env.SERVER_PORT) || 20322,  // otomatis ikut port Allocation Pterodactyl (SERVER_PORT); kalau kosong pakai 7777
  DOMAIN: 'lunaris-node-free-5.hostkita.help',   // domain kamu (hanya untuk tampilan log)
  AUDIO: true,            // putar di latar belakang (musik lanjut saat layar mati). false = matikan
  PIPED: []               // opsional: alamat API Piped sendiri, mis. ['https://pipedapi.domainmu.com'] · false = matikan jalur cadangan
};
