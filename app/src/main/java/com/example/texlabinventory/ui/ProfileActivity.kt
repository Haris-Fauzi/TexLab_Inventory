package com.example.texlabinventory.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.texlabinventory.LoginActivity
import com.example.texlabinventory.R
import com.example.texlabinventory.data.utils.CloudinaryHelper
import com.example.texlabinventory.databinding.ActivityProfileBinding
import com.example.texlabinventory.databinding.DialogEditProfileBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Variable untuk menyimpan data user lokal saat ini
    private var currentName: String = ""
    private var currentPhone: String = ""
    private var currentMajor: String = ""

    // Launcher untuk memilih foto dari galeri HP
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val selectedImageUri: Uri? = result.data?.data
            if (selectedImageUri != null) {
                uploadImageToCloudinary(selectedImageUri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Inisialisasi CloudinaryHelper
        CloudinaryHelper.init(this)

        loadUserData()
        setupListeners()
        setupLogoutButton()
    }

    private fun setupListeners() {
        // Klik tombol Ubah Foto (FloatingActionButton kamera)
        binding.btnChangePhoto.setOnClickListener {
            openImagePicker()
        }

        // Klik tombol Ubah Data Profil (Memicu BottomSheetDialog)
        binding.btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser ?: return

        val googleName = currentUser.displayName
        val googleEmail = currentUser.email
        val photoUrl = currentUser.photoUrl

        binding.tvEmail.text = googleEmail ?: "-"
        binding.tvName.text = googleName ?: "User TexLab"
        currentName = googleName ?: ""

        // Muat Foto Profil awal
        if (photoUrl != null) {
            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(binding.imgProfile)
        }

        // Tarik Data Profil Detail dari Firestore
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val name = document.getString("name") ?: googleName ?: ""
                    val phone = document.getString("phone") ?: "-"
                    val major = document.getString("major") ?: "-"
                    val profilePhotoUrl = document.getString("photoUrl")

                    currentName = name
                    currentPhone = phone
                    currentMajor = major

                    binding.tvName.text = name
                    binding.tvPhone.text = phone
                    binding.tvMajor.text = major
                    binding.tvDetailMajor.text = major

                    if (!profilePhotoUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(profilePhotoUrl)
                            .placeholder(R.drawable.ic_launcher_background)
                            .error(R.drawable.ic_launcher_background)
                            .into(binding.imgProfile)
                    }

                    // Setelah nama pengajar didapat, hitung statistik peminjaman barangnya
                    calculateBorrowingStats(name)
                } else {
                    calculateBorrowingStats(currentName)
                }
            }
            .addOnFailureListener {
                calculateBorrowingStats(currentName)
            }
    }

    /**
     * Menghitung total unit/pcs barang yang dipinjam & dikembalikan
     * berdasarkan transaksi pengguna yang sedang login.
     */
    private fun calculateBorrowingStats(teacherName: String) {
        val currentUser = auth.currentUser ?: return
        val userEmail = currentUser.email ?: ""

        // Mengambil transaksi peminjaman berdasarkan ID user, email, atau nama pengajar
        db.collection("peminjaman")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null && !snapshot.isEmpty) {
                    var totalBorrowedPcs = 0
                    var totalReturnedPcs = 0

                    for (doc in snapshot.documents) {
                        val docUserId = doc.getString("userId") ?: doc.getString("idUser") ?: ""
                        val docEmail = doc.getString("email") ?: doc.getString("userEmail") ?: ""
                        val docGuru = doc.getString("guruPengajar") ?: doc.getString("namaGuru") ?: ""

                        // Cek apakah dokumen ini milik user yang sedang login
                        val isUserTransaction = (docUserId.isNotEmpty() && docUserId == currentUser.uid) ||
                                (docEmail.isNotEmpty() && docEmail.equals(userEmail, ignoreCase = true)) ||
                                (teacherName.isNotEmpty() && docGuru.contains(teacherName, ignoreCase = true))

                        if (isUserTransaction) {
                            val status = doc.getString("status")?.lowercase()?.trim() ?: ""

                            // Mengambil jumlah barang/pcs dari field 'jumlah', 'quantity', atau default 1
                            val qty = doc.getLong("jumlah")?.toInt()
                                ?: doc.getLong("quantity")?.toInt()
                                ?: doc.getLong("jumlahPinjam")?.toInt()
                                ?: 1

                            when (status) {
                                "dipinjam", "active", "borrowed", "pending" -> {
                                    totalBorrowedPcs += qty
                                }
                                "dikembalikan", "returned", "finished", "semua dikembalikan", "selesai" -> {
                                    totalReturnedPcs += qty
                                }
                            }
                        }
                    }

                    // Tampilkan ke UI
                    binding.tvBorrowedCount.text = "$totalBorrowedPcs pcs"
                    binding.tvReturnedCount.text = "$totalReturnedPcs pcs"
                } else {
                    binding.tvBorrowedCount.text = "0 pcs"
                    binding.tvReturnedCount.text = "0 pcs"
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat statistik barang", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Membuka Intent untuk memilih gambar dari galeri HP
     */
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    /**
     * Mengunggah gambar yang dipilih ke Cloudinary menggunakan CloudinaryHelper
     */
    private fun uploadImageToCloudinary(imageUri: Uri) {
        Toast.makeText(this, "Mengunggah foto profil...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Memanggil CloudinaryHelper dengan menargetkan folder "profile_photos"
                val uploadedUrl = CloudinaryHelper.uploadImage(imageUri, folder = "profile_photos")

                withContext(Dispatchers.Main) {
                    if (!uploadedUrl.isNullOrEmpty()) {
                        saveProfilePhotoUrlToFirestore(uploadedUrl)
                    } else {
                        Toast.makeText(this@ProfileActivity, "Gagal mengunggah foto ke Cloudinary", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Simpan URL Cloudinary ke Firestore
     */
    private fun saveProfilePhotoUrlToFirestore(photoUrl: String) {
        val currentUser = auth.currentUser ?: return

        val updateData = mapOf("photoUrl" to photoUrl)

        db.collection("users").document(currentUser.uid)
            .set(updateData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Foto profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                Glide.with(this).load(photoUrl).into(binding.imgProfile)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memperbarui Firestore", Toast.LENGTH_SHORT).show()
            }
    }
     /*
     * Menampilkan BottomSheetDialog untuk mengubah Phone, Jurusan, dan Password.
     * Nama dan Email diset read-only (tidak dapat diubah).
     */
     private fun showEditProfileDialog() {
         val dialogBinding = DialogEditProfileBinding.inflate(layoutInflater)
         val dialog = BottomSheetDialog(this)
         dialog.setContentView(dialogBinding.root)

         val currentUser = auth.currentUser

         // Pre-fill data awal
         dialogBinding.etEditName.setText(currentName)
         dialogBinding.etEditPhone.setText(currentPhone)
         dialogBinding.etEditMajor.setText(currentMajor)

         // Fitur batal
         dialogBinding.btnCancel.setOnClickListener {
             dialog.dismiss()
         }

         // Fitur simpan
         dialogBinding.btnSave.setOnClickListener {
             val newPhone = dialogBinding.etEditPhone.text.toString().trim()
             val newMajor = dialogBinding.etEditMajor.text.toString().trim()
             val newPassword = dialogBinding.etEditPassword.text.toString().trim()

             if (currentUser != null) {
                 val updatedData = hashMapOf<String, Any>(
                     "phone" to newPhone,
                     "major" to newMajor
                 )

                 db.collection("users").document(currentUser.uid)
                     .set(updatedData, com.google.firebase.firestore.SetOptions.merge())
                     .addOnSuccessListener {
                         binding.tvPhone.text = newPhone
                         binding.tvMajor.text = newMajor
                         binding.tvDetailMajor.text = newMajor

                         currentPhone = newPhone
                         currentMajor = newMajor

                         Toast.makeText(this, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()

                         if (newPassword.isNotEmpty()) {
                             if (newPassword.length < 6) {
                                 Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
                             } else {
                                 currentUser.updatePassword(newPassword)
                                     .addOnSuccessListener {
                                         Toast.makeText(this, "Password berhasil diperbarui", Toast.LENGTH_SHORT).show()
                                     }
                                     .addOnFailureListener { e ->
                                         Toast.makeText(this, "Gagal ubah password: ${e.message}", Toast.LENGTH_LONG).show()
                                     }
                             }
                         }

                         dialog.dismiss()
                     }
                     .addOnFailureListener { e ->
                         Toast.makeText(this, "Gagal memperbarui profil: ${e.message}", Toast.LENGTH_SHORT).show()
                     }
             }
         }

         dialog.show()
     }

    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
            auth.signOut()

            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            val googleSignInClient = GoogleSignIn.getClient(this, gso)
            googleSignInClient.signOut().addOnCompleteListener {
                Toast.makeText(this, "Berhasil keluar", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
        }
    }
}