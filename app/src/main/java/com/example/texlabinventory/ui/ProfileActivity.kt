package com.example.texlabinventory.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.texlabinventory.R
import com.example.texlabinventory.databinding.ActivityProfileBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadUserData()
        setupLogoutButton()
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser ?: return

        // 1. Tarik Data Utama dari Google / Firebase Auth
        val googleName = currentUser.displayName
        val googleEmail = currentUser.email
        val photoUrl = currentUser.photoUrl

        binding.tvEmail.text = googleEmail ?: "Tidak ada email"
        binding.tvName.text = googleName ?: "User TexLab"

        // Muat Foto Profil menggunakan Glide
        if (photoUrl != null) {
            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(binding.imgProfile)
        }

        // 2. Tarik Data Tambahan dari Firestore jika ada
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val firestoreName = document.getString("name")
                    if (!firestoreName.isNullOrEmpty()) {
                        binding.tvName.text = firestoreName
                    }
                }
            }
    }

    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
            // Logout dari Firebase
            auth.signOut()

            // Logout dari Client Google Sign-In
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            val googleSignInClient = GoogleSignIn.getClient(this, gso)
            googleSignInClient.signOut().addOnCompleteListener {
                Toast.makeText(this, "Berhasil keluar", Toast.LENGTH_SHORT).show()

                // Navigasi kembali ke LoginActivity
                val intent = Intent(this, com.example.texlabinventory.LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
        }
    }
}