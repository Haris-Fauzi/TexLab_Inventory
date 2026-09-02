package com.example.texlabinventory

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var btnGoogleLogin: MaterialButton
    private lateinit var progressBarLogin: ProgressBar

    // Callback untuk menerima hasil login Google
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            setLoading(false)
            Toast.makeText(this, "Google Sign-In Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 1. Cek Sesi Autologin & Status Approval
        val currentUser = auth.currentUser
        if (currentUser != null) {
            checkUserApprovalStatus(currentUser.uid)
            return
        }

        setContentView(R.layout.activity_login)

        // Konfigurasi Google Sign-In menggunakan Web Client ID dari strings.xml
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin)
        progressBarLogin = findViewById(R.id.progressBarLogin)

        btnLogin.setOnClickListener { performLogin() }
        btnGoogleLogin.setOnClickListener { performGoogleSignIn() }
    }

    private fun performGoogleSignIn() {
        setLoading(true)
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let { handleUserDatabaseAndApproval(it.uid, it.displayName, it.email) }
                } else {
                    setLoading(false)
                    Toast.makeText(this, "Autentikasi Firebase Gagal: ${task.exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun handleUserDatabaseAndApproval(uid: String, name: String?, email: String?) {
        val userRef = db.collection("users").document(uid)

        userRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                // pendaftaran pertama kali: simpan data ke Firestore dengan status isApproved = false
                val userData = hashMapOf(
                    "uid" to uid,
                    "name" to (name ?: "User TexLab"),
                    "email" to (email ?: ""),
                    "isApproved" to false, // Perlu konfirmasi admin di Firebase Console
                    "createdAt" to FieldValue.serverTimestamp()
                )
                userRef.set(userData).addOnSuccessListener {
                    setLoading(false)
                    navigateToPendingApproval()
                }.addOnFailureListener { e ->
                    setLoading(false)
                    Toast.makeText(this, "Gagal menyimpan data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                // user sudah terdaftar sebelumnya: cek nilai field isApproved
                val isApproved = document.getBoolean("isApproved") ?: false
                setLoading(false)
                if (isApproved) {
                    navigateToMain()
                } else {
                    navigateToPendingApproval()
                }
            }
        }.addOnFailureListener { e ->
            setLoading(false)
            Toast.makeText(this, "Gagal mengambil data user: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkUserApprovalStatus(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val isApproved = document.getBoolean("isApproved") ?: false
                if (isApproved) {
                    navigateToMain()
                } else {
                    navigateToPendingApproval()
                }
            }
            .addOnFailureListener {
                auth.signOut()
            }
    }

    private fun performLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty()) {
            etEmail.error = "Email tidak boleh kosong"
            etEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Format email tidak valid"
            etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etPassword.error = "Password tidak boleh kosong"
            etPassword.requestFocus()
            return
        }

        setLoading(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: ""
                    checkUserApprovalStatus(uid)
                } else {
                    setLoading(false)
                    Toast.makeText(this, "Login Gagal: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBarLogin.visibility = View.VISIBLE
            btnLogin.isEnabled = false
            btnGoogleLogin.isEnabled = false
        } else {
            progressBarLogin.visibility = View.GONE
            btnLogin.isEnabled = true
            btnGoogleLogin.isEnabled = true
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToPendingApproval() {
        val intent = Intent(this, PendingApprovalActivity::class.java)
        startActivity(intent)
        finish()
    }
}