package com.example.texlabinventory.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.texlabinventory.R
import com.example.texlabinventory.data.model.Laptop
import com.example.texlabinventory.data.model.Specs
import com.example.texlabinventory.data.repository.LaptopRepository
import com.example.texlabinventory.data.utils.Resource
import com.example.texlabinventory.ui.viewmodel.LaptopViewModel
import com.google.android.material.textfield.TextInputEditText

class AddLaptopActivity : AppCompatActivity() {

    private val viewModel: LaptopViewModel by viewModels()
    private val repository = LaptopRepository()
    private var selectedImageUri: Uri? = null

    private lateinit var ivPreview: ImageView
    private lateinit var pbLoading: ProgressBar
    private lateinit var btnSave: Button

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            ivPreview.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_laptop)

        ivPreview = findViewById(R.id.ivPreview)
        pbLoading = findViewById(R.id.pbLoading)
        btnSave = findViewById(R.id.btnSave)

        findViewById<Button>(R.id.btnSelectImage).setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        btnSave.setOnClickListener {
            saveLaptopData()
        }
    }

    private fun saveLaptopData() {
        val inventoryId = findViewById<TextInputEditText>(R.id.etInventoryId).text.toString().trim()
        val brand = findViewById<TextInputEditText>(R.id.etBrand).text.toString().trim()
        val model = findViewById<TextInputEditText>(R.id.etModel).text.toString().trim()
        val sn = findViewById<TextInputEditText>(R.id.etSN).text.toString().trim()
        val location = findViewById<TextInputEditText>(R.id.etLocation).text.toString().trim()
        val processor = findViewById<TextInputEditText>(R.id.etProcessor).text.toString().trim()
        val ram = findViewById<TextInputEditText>(R.id.etRam).text.toString().trim()
        val storage = findViewById<TextInputEditText>(R.id.etStorage).text.toString().trim()

        if (inventoryId.isEmpty() || brand.isEmpty() || model.isEmpty()) {
            Toast.makeText(this, "ID, Brand, dan Model wajib diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        // Upload Gambar dulu ke Cloudinary jika ada gambar yang dipilih
        if (selectedImageUri != null) {
            repository.uploadImageToCloudinary(
                selectedImageUri!!,
                onSuccess = { imageUrl ->
                    saveToFirestore(inventoryId, brand, model, sn, location, processor, ram, storage, imageUrl)
                },
                onError = { error ->
                    setLoading(false)
                    Toast.makeText(this, "Upload Gambar Gagal: $error", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            // Tanpa gambar
            saveToFirestore(inventoryId, brand, model, sn, location, processor, ram, storage, "")
        }
    }

    private fun saveToFirestore(
        id: String, brand: String, model: String, sn: String,
        location: String, processor: String, ram: String, storage: String, imageUrl: String
    ) {
        val laptop = Laptop(
            inventory_id = id,
            brand = brand,
            model = model,
            serial_number = sn,
            location = location,
            condition = "NORMAL",
            image_url = imageUrl,
            specs = Specs(processor = processor, ram = ram, storage = storage)
        )

        viewModel.addLaptop(laptop) { resource ->
            setLoading(false)
            when (resource) {
                is Resource.Success -> {
                    Toast.makeText(this, "Laptop Berhasil Ditambahkan!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        pbLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnSave.isEnabled = !isLoading
    }
}