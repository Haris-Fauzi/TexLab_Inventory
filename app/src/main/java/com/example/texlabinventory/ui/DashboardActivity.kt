package com.example.texlabinventory

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.texlabinventory.databinding.ActivityDashboardBinding

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            Toast.makeText(this, "Fitur History Peminjaman", Toast.LENGTH_SHORT).show()
        }

        // Klik Logout
        binding.cardLogout.setOnClickListener {
            // 1. Sign out dari Firebase Auth
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()

            // 2. Tampilkan pesan
            Toast.makeText(this, "Berhasil Logout", Toast.LENGTH_SHORT).show()

            // 3. Kembali ke LoginActivity & bersihkan backstack
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}