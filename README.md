# Marmut — aplikasi Android untuk situs utama

Proyek Android sederhana yang membuka situs utama di WebView dan menyediakan tab terpisah untuk jendela pop-up/tautan luar.

**Situs utama:** https://jembot0mlungkertempekmambu.netlify.app/

## Yang sudah diatur

- Ikon aplikasi menggunakan gambar marmut yang diberikan.
- Tab **Utama** dibuat otomatis dan tidak mempunyai tombol tutup.
- `window.open()` dan tautan `target="_blank"` ditangani sebagai tab baru.
- Navigasi tingkat halaman dari situs utama ke domain lain diarahkan ke tab sementara, sehingga situs utama tetap ada.
- Tab sementara punya tombol `×` untuk ditutup.
- Tombol Kembali Android dari tab sementara langsung kembali ke tab Utama. Tab sementara tetap tersimpan sampai ditutup dengan `×`.
- Maksimal 4 tab sekaligus (1 utama + 3 sementara) untuk membatasi penggunaan memori.
- Jika renderer WebView berhenti, tab sementara ditutup atau halaman utama dimuat ulang.
- Tidak menggunakan JavaScript bridge ke Android, dan error sertifikat HTTPS tidak dilewati.

## Build APK dari GitHub (cara yang disiapkan)

1. Ekstrak ZIP ini di komputer.
2. Buat repository baru di GitHub.
3. Unggah **isi folder** `MarmutWebApp` ke repository (file `.github/workflows/build-apk.yml` harus berada pada jalur itu, bukan di dalam folder bertingkat tambahan).
4. Buka tab **Actions** di repository GitHub. Jalankan workflow **Build Marmut APK** dengan tombol **Run workflow**, atau lakukan push ke branch `main`/`master`.
5. Setelah workflow selesai hijau, buka proses build paling baru, gulir ke **Artifacts**, lalu unduh **Marmut-APK**. Di dalam ZIP artifact itu ada `Marmut.apk`.
6. Pindahkan `Marmut.apk` ke Android dan instal. Android mungkin meminta izin “Install unknown apps” untuk aplikasi/file manager yang dipakai.

Workflow menggunakan Gradle 8.9, Java 17, Android SDK 35, dan menghasilkan **debug APK** yang dapat dipasang untuk pengujian. Ini bukan APK release yang ditandatangani dengan kunci distribusi Play Store.

## Catatan tentang tab dan iklan

Tab baru akan bekerja untuk pop-up atau link yang dikirim browser sebagai jendela baru, serta tautan tingkat halaman yang membawa pengguna keluar dari domain utama. Situs/iklan yang mengubah halaman utama melalui skrip dengan cara yang tidak terdeteksi sebagai navigasi baru masih dapat mengganti URL pada tab asal; perilaku itu dikendalikan oleh kode situs/iklan. Jangan memasukkan sandi atau data sensitif ke halaman iklan yang tidak dikenal.

## Jika mau mengganti situs utama

Edit `HOME_URL` dan `HOME_HOST` di:

`app/src/main/java/com/rkdesain/marmut/MainActivity.java`

Pastikan `HOME_HOST` hanya berisi hostname, tanpa `https://` atau garis miring.

## Proyek

Proyek ini tidak menyertakan `gradlew`/Gradle wrapper JAR; workflow GitHub menginstal Gradle 8.9 secara langsung sebelum membangun. Untuk build lokal, buka di Android Studio dan pilih instalasi Gradle lokal yang kompatibel atau tambahkan wrapper dari Android Studio.
