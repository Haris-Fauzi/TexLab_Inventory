package com.example.texlabinventory.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.texlabinventory.data.model.Laptop
import com.example.texlabinventory.data.model.Specs
import com.example.texlabinventory.data.repository.LaptopRepository
import com.example.texlabinventory.data.utils.Resource
import com.example.texlabinventory.databinding.ActivityAddLaptopBinding
import com.example.texlabinventory.ui.viewModel.LaptopViewModel
import java.io.File

class AddLaptopActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddLaptopBinding
    private val viewModel: LaptopViewModel by viewModels()
    private val repository = LaptopRepository()

    // Penampung banyak URI foto lokal yang baru dipilih
    private val selectedImageUris = ArrayList<Uri>()

    private var isEditMode = false
    private var existingLaptop: Laptop? = null
    private var tempCameraUri: Uri? = null

    // 1. Launcher Galeri (Multi Select)
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedImageUris.addAll(uris)
            updateImagePreview()
        }
    }

    // 2. Launcher Kamera (Menambahkan 1 Foto HD ke List)
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess && tempCameraUri != null) {
            tempCameraUri?.let { uri ->
                selectedImageUris.add(uri)
                updateImagePreview()
            }
        }
    }

    private fun updateImagePreview() {
        if (selectedImageUris.isNotEmpty()) {
            // Menampilkan foto paling baru yang ditambahkan ke preview
            binding.ivPreview.setImageURI(selectedImageUris.last())
        }
    }

    private fun createImageUri(): Uri {
        val imageFile = File(
            getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES),
            "laptop_hd_${System.currentTimeMillis()}.jpg"
        )
        return FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.fileprovider",
            imageFile
        )
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Pilih dari Galeri", "Ambil Foto (Kamera)")
        AlertDialog.Builder(this)
            .setTitle("Pilih Sumber Foto")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> imagePickerLauncher.launch("image/*")
                    1 -> {
                        val uri = createImageUri()
                        tempCameraUri = uri
                        takePictureLauncher.launch(uri)
                    }
                }
            }
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddLaptopBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupStatusBarTheme()

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootView) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(0, statusBarHeight, 0, 0)
            insets
        }

        isEditMode = intent.getBooleanExtra("IS_EDIT_MODE", false)
        existingLaptop = intent.getSerializableExtra("EXTRA_LAPTOP_EDIT") as? Laptop

        if (isEditMode && existingLaptop != null) {
            setupEditMode(existingLaptop!!)
        }

        binding.btnSelectImage.setOnClickListener {
            showImageSourceDialog()
        }

        binding.btnSave.setOnClickListener {
            saveLaptopData()
        }
    }

    private fun setupStatusBarTheme() {
        window.statusBarColor = Color.TRANSPARENT
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        windowInsetsController.isAppearanceLightStatusBars = !isDarkMode
    }

    private fun setupEditMode(laptop: Laptop) = with(binding) {
        tvPageTitle.text = "Edit Data Laptop"
        btnSave.text = "Update Data"

        etInventoryId.setText(laptop.inventory_id)
        etInventoryId.isEnabled = false

        etBrand.setText(laptop.brand)
        etModel.setText(laptop.model)
        etSN.setText(laptop.serial_number)
        etLocation.setText(laptop.location)
        etProcessor.setText(laptop.specs.processor)
        etRam.setText(laptop.specs.ram)
        etStorage.setText(laptop.specs.storage)

        // Tampilkan foto pertama jika ada data image_url
        if (laptop.image_url.isNotEmpty()) {
            Glide.with(this@AddLaptopActivity)
                .load(laptop.image_url.first())
                .into(ivPreview)
        }
    }

    private fun saveLaptopData() = with(binding) {
        val inventoryId = etInventoryId.text.toString().trim()
        val brand = etBrand.text.toString().trim()
        val model = etModel.text.toString().trim()
        val sn = etSN.text.toString().trim()
        val location = etLocation.text.toString().trim()
        val processor = etProcessor.text.toString().trim()
        val ram = etRam.text.toString().trim()
        val storage = etStorage.text.toString().trim()

        if (inventoryId.isEmpty() || brand.isEmpty() || model.isEmpty()) {
            Toast.makeText(this@AddLaptopActivity, "ID, Brand, dan Model wajib diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        if (selectedImageUris.isNotEmpty()) {
            // Upload seluruh list foto ke Cloudinary secara bertahap (Rekursif)
            uploadMultipleImages(0, ArrayList()) { uploadedUrls ->
                // Jika dalam mode edit, gabungkan foto lama dengan foto baru
                val finalUrls = if (isEditMode) {
                    (existingLaptop?.image_url ?: emptyList()) + uploadedUrls
                } else {
                    uploadedUrls
                }

                saveToFirestore(inventoryId, brand, model, sn, location, processor, ram, storage, finalUrls)
            }
        } else {
            // Jika tidak memilih foto baru
            val existingUrls = if (isEditMode) existingLaptop?.image_url ?: emptyList() else emptyList()
            saveToFirestore(inventoryId, brand, model, sn, location, processor, ram, storage, existingUrls)
        }
    }

    // Fungsi rekursif untuk mengunggah List Uri ke Cloudinary
    private fun uploadMultipleImages(
        index: Int,
        uploadedUrls: ArrayList<String>,
        onComplete: (List<String>) -> Unit
    ) {
        if (index >= selectedImageUris.size) {
            onComplete(uploadedUrls)
            return
        }

        repository.uploadImageToCloudinary(
            selectedImageUris[index],
            onSuccess = { imageUrl ->
                uploadedUrls.add(imageUrl)
                // Lanjut ke foto berikutnya
                uploadMultipleImages(index + 1, uploadedUrls, onComplete)
            },
            onError = { error ->
                setLoading(false)
                Toast.makeText(this@AddLaptopActivity, "Upload Gambar ke-${index + 1} Gagal: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun saveToFirestore(
        id: String, brand: String, model: String, sn: String,
        location: String, processor: String, ram: String, storage: String, imageUrls: List<String>
    ) {
        val laptop = Laptop(
            inventory_id = id,
            model = model,
            serial_number = sn,
            location = location,
            status = existingLaptop?.status ?: "TERSEDIA", // Menggunakan status peminjaman
            rawImageUrl = imageUrls, // PERBAIKAN: Gunakan rawImageUrl untuk menyimpan List foto
            specs = Specs(processor = processor, ram = ram, storage = storage)
        )

        val callback: (Resource<Boolean>) -> Unit = { resource ->
            setLoading(false)
            when (resource) {
                is Resource.Success -> {
                    val message = if (isEditMode) "Data Berhasil Diperbarui!" else "Laptop Berhasil Ditambahkan!"
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    finish()
                }
                is Resource.Error -> Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                else -> {}
            }
        }

        if (isEditMode) {
            viewModel.updateLaptop(laptop, callback)
        } else {
            viewModel.addLaptop(laptop, callback)
        }
    }

    private fun setLoading(isLoading: Boolean) = with(binding) {
        pbLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnSave.isEnabled = !isLoading
    }
}