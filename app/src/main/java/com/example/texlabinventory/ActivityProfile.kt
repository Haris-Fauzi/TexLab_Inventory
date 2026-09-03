package com.example.texlabinventory.ui // Sesuaikan jika tidak ada di folder ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.texlabinventory.LoginActivity
import com.example.texlabinventory.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth

class ActivityProfile : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()

        // Konfigurasi Google Sign-In untuk logout
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val imgProfile = findViewById<ShapeableImageView>(R.id.imgProfile)
        val tvName = findViewById<TextView>(R.id.tvName)
        val tvEmail = findViewById<TextView>(R.id.tvEmail)
        val btnLogout = findViewById<MaterialButton>(R.id.btnLogout)

        // Tarik Data User dari Firebase / Google
        val user = auth.currentUser
        if (user != null) {
            tvName.text = user.displayName ?: "Nama Tidak Ada"
            tvEmail.text = user.email ?: "Email Tidak Ada"

            user.photoUrl?.let { photoUri ->
                Glide.with(this)
                    .load(photoUri)
                    .into(imgProfile)
            }
        }

        // Tombol Logout Merah
        btnLogout.setOnClickListener {
            auth.signOut()
            googleSignInClient.signOut().addOnCompleteListener {
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }
}