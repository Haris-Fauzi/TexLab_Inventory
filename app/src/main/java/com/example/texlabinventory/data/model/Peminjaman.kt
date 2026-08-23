package com.example.texlabinventory.data.model

import com.google.firebase.Timestamp

data class Peminjaman(
    val id: String = "",
    val itemId: String = "",              // Contoh: "C001"
    val namaItem: String = "",            // Contoh: "Laptop Asus"
    val siswaId: String = "",             // NIS Siswa, contoh: "247550"
    val namaSiswa: String = "",           // Contoh: "Andara"
    val kelasSiswa: String = "",          // Contoh: "XI RPL 1"
    val ruangan: String = "",             // Contoh: "Teori 1"
    val guruPengajar: String = "",        // Contoh: "Pak Galih"
    val waktuPinjam: Timestamp = Timestamp.now(),
    val waktuKembali: Timestamp? = null,  // null saat dipinjam, terisi saat dikembalikan
    val status: String = "DIPINJAM"       // DIPINJAM / DIKEMBALIKAN
)