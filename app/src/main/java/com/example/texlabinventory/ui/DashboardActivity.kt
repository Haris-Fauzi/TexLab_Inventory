package com.example.texlabinventory

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.texlabinventory.databinding.ActivityDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val db = FirebaseFirestore.getInstance()

    // Simpan listener agar bisa dilepas saat activity di-destroy (mencegah memory leak)
    private var laptopListener: ListenerRegistration? = null
    private var siswaListener: ListenerRegistration? = null
    private var historyListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        setupWindowInsets()
        observeDashboardData()
    }

    private fun setupClickListeners() {
        // Klik Data Inventaris -> Masuk ke MainActivity (Katalog Laptop)
        binding.cardInventaris.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Klik Data Siswa
        binding.cardSiswa.setOnClickListener {
            val intent = Intent(this, com.example.texlabinventory.ui.SiswaActivity::class.java)
            startActivity(intent)
        }

        // Klik History
        binding.cardHistory.setOnClickListener {
            val intent = Intent(this, com.example.texlabinventory.ui.HistoryPeminjamanActivity::class.java)
            startActivity(intent)
        }

        // Klik Logout
        binding.cardLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(this, "Berhasil Logout", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun observeDashboardData() {
        // 1. Ambil Total Inventaris & Barang Dipinjam dari koleksi "laptops"
        // (Ganti nama koleksi "laptops" jika di Firestore kamu menggunakan nama lain seperti "inventaris")
        laptopListener = db.collection("items").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener

            // Total Seluruh Laptop
            val totalInventaris = snapshot.size()
            binding.tvTotalInventaris.text = totalInventaris.toString()

            // Total Laptop yang Sedang Dipinjam
            val totalDipinjam = snapshot.documents.count { doc ->
                val status = doc.getString("status")
                status.equals("DIPINJAM", ignoreCase = true)
            }
            binding.tvTotalDipinjam.text = totalDipinjam.toString()
        }

        // 2. Ambil Total Siswa dari koleksi "siswa"
        siswaListener = db.collection("siswa").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            binding.tvTotalSiswa.text = snapshot.size().toString()
        }

        // 3. Ambil Total Riwayat Selesai dari koleksi "history" atau "peminjaman"
        historyListener = db.collection("peminjaman").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            binding.tvTotalHistory.text = snapshot.size().toString()
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            binding.root.setPadding(0, statusBarHeight, 0, 0)
            insets
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Hapus listener saat activity ditutup
        laptopListener?.remove()
        siswaListener?.remove()
        historyListener?.remove()
    }
}