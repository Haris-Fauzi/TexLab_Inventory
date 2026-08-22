package com.example.texlabinventory.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.texlabinventory.data.model.Laptop
import com.example.texlabinventory.data.model.Specs
import com.example.texlabinventory.data.repository.LaptopRepository
import com.example.texlabinventory.data.utils.Resource
import com.example.texlabinventory.databinding.ActivityAddLaptopBinding
import com.example.texlabinventory.ui.viewModel.LaptopViewModel

class AddLaptopActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddLaptopBinding
    private val viewModel: LaptopViewModel by viewModels()
    private val repository = LaptopRepository()
    private var selectedImageUri: Uri? = null

    private var isEditMode = false
    private var existingLaptop: Laptop? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.ivPreview.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddLaptopBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isEditMode = intent.getBooleanExtra("IS_EDIT_MODE", false)
        existingLaptop = intent.getSerializableExtra("EXTRA_LAPTOP_EDIT") as? Laptop

        if (isEditMode && existingLaptop != null) {
            setupEditMode(existingLaptop!!)
        }

        binding.btnSelectImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.btnSave.setOnClickListener {
            saveLaptopData()
        }
    }

    private fun setupEditMode(laptop: Laptop) = with(binding) {
        title = "Edit Data Laptop"
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

        if (laptop.image_url.isNotEmpty()) {
            Glide.with(this@AddLaptopActivity)
                .load(laptop.image_url)
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

        if (selectedImageUri != null) {
            repository.uploadImageToCloudinary(
                selectedImageUri!!,
                onSuccess = { imageUrl ->
                    saveToFirestore(inventoryId, brand, model, sn, location, processor, ram, storage, imageUrl)
                },
                onError = { error ->
                    setLoading(false)
                    Toast.makeText(this@AddLaptopActivity, "Upload Gambar Gagal: $error", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            val existingUrl = if (isEditMode) existingLaptop?.image_url ?: "" else ""
            saveToFirestore(inventoryId, brand, model, sn, location, processor, ram, storage, existingUrl)
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
            condition = existingLaptop?.condition ?: "NORMAL",
            image_url = imageUrl,
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