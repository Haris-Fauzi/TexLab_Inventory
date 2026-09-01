package com.example.texlabinventory.ui

import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
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

    private val selectedImageUris = ArrayList<Uri>()
    private var isEditMode = false
    private var existingLaptop: Laptop? = null
    private var tempCameraUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedImageUris.addAll(uris)
            updateImagePreview()
        }
    }

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

        // Setup dropdown Lokasi, Kondisi Laptop, dan Charger Status
        setupDropdowns()

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

    private fun setupDropdowns() = with(binding) {
        // 1. Dropdown Lokasi
        val locationOptions = arrayOf("LAB CAD", "LAB Pemrograman")
        val locationAdapter = ArrayAdapter(
            this@AddLaptopActivity,
            android.R.layout.simple_dropdown_item_1line,
            locationOptions
        )
        actvLocation.setAdapter(locationAdapter)

        // 2. Dropdown Kondisi Laptop
        val laptopConditionOptions = arrayOf("BAIK", "RUSAK")
        val conditionAdapter = ArrayAdapter(
            this@AddLaptopActivity,
            android.R.layout.simple_dropdown_item_1line,
            laptopConditionOptions
        )
        actvCondition.setAdapter(conditionAdapter)

        // Event listener ketika kondisi dipilih
        actvCondition.setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position).toString()
            if (selected == "RUSAK") {
                tilDamageNotes.visibility = View.VISIBLE
            } else {
                tilDamageNotes.visibility = View.GONE
                etDamageNotes.text?.clear()
            }
        }

        // 3. Dropdown Charger Status & Condition
        val chargerStatusOptions = arrayOf("Ada Charger", "Tanpa Charger")
        val chargerConditionOptions = arrayOf("Baik/Normal", "Rusak")

        val statusAdapter = ArrayAdapter(
            this@AddLaptopActivity,
            android.R.layout.simple_dropdown_item_1line,
            chargerStatusOptions
        )
        actvChargerStatus.setAdapter(statusAdapter)

        val chargerCondAdapter = ArrayAdapter(
            this@AddLaptopActivity,
            android.R.layout.simple_dropdown_item_1line,
            chargerConditionOptions
        )
        actvChargerCondition.setAdapter(chargerCondAdapter)
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

        // Set Lokasi
        actvLocation.setText(laptop.location, false)

        // Memecah kembali data condition jika tersimpan dalam format RUSAK: "..."
        val conditionValue = laptop.condition
        if (conditionValue.startsWith("RUSAK")) {
            actvCondition.setText("RUSAK", false)
            tilDamageNotes.visibility = View.VISIBLE

            // Ekstrak catatan rusak setelah teks "RUSAK: "
            val note = conditionValue.substringAfter("RUSAK: ", "").removeSurrounding("\"")
            etDamageNotes.setText(note)
        } else {
            actvCondition.setText("BAIK", false)
            tilDamageNotes.visibility = View.GONE
        }

        etPicLab.setText(laptop.pic_lab)
        etProcurementYear.setText(if (laptop.procurement_year != 0L) laptop.procurement_year.toString() else "")

        actvChargerStatus.setText(laptop.charger_status, false)
        actvChargerCondition.setText(laptop.charger_condition, false)

        etProcessor.setText(laptop.specs.processor)
        etRam.setText(laptop.specs.ram)
        etStorage.setText(laptop.specs.storage)

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
        val location = actvLocation.text.toString().trim()

        val selectedCondition = actvCondition.text.toString().trim()
        val damageNote = etDamageNotes.text.toString().trim()

        if (inventoryId.isEmpty() || brand.isEmpty() || model.isEmpty()) {
            Toast.makeText(this@AddLaptopActivity, "ID, Brand, dan Model wajib diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedCondition == "RUSAK" && damageNote.isEmpty()) {
            Toast.makeText(this@AddLaptopActivity, "Catatan kerusakan wajib diisi jika kondisi RUSAK!", Toast.LENGTH_SHORT).show()
            return
        }

        // Format data kondisi ke Firestore
        val finalCondition = if (selectedCondition == "RUSAK") {
            "RUSAK: \"$damageNote\""
        } else {
            "BAIK"
        }

        val picLab = etPicLab.text.toString().trim()
        val procurementYearStr = etProcurementYear.text.toString().trim()
        val procurementYear = procurementYearStr.toLongOrNull() ?: 0L

        val chargerStatus = actvChargerStatus.text.toString().trim()
        val chargerCondition = actvChargerCondition.text.toString().trim()

        val processor = etProcessor.text.toString().trim()
        val ram = etRam.text.toString().trim()
        val storage = etStorage.text.toString().trim()

        setLoading(true)

        if (selectedImageUris.isNotEmpty()) {
            uploadMultipleImages(0, ArrayList()) { uploadedUrls ->
                val finalUrls = if (isEditMode) {
                    (existingLaptop?.image_url ?: emptyList()) + uploadedUrls
                } else {
                    uploadedUrls
                }

                saveToFirestore(
                    inventoryId, brand, model, sn, location, finalCondition, picLab,
                    procurementYear, chargerStatus, chargerCondition,
                    processor, ram, storage, finalUrls
                )
            }
        } else {
            val existingUrls = if (isEditMode) existingLaptop?.image_url ?: emptyList() else emptyList()
            saveToFirestore(
                inventoryId, brand, model, sn, location, finalCondition, picLab,
                procurementYear, chargerStatus, chargerCondition,
                processor, ram, storage, existingUrls
            )
        }
    }

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
                uploadMultipleImages(index + 1, uploadedUrls, onComplete)
            },
            onError = { error ->
                setLoading(false)
                Toast.makeText(this@AddLaptopActivity, "Upload Gambar ke-${index + 1} Gagal: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun saveToFirestore(
        id: String, brand: String, model: String, sn: String, location: String,
        condition: String, picLab: String, procurementYear: Long,
        chargerStatus: String, chargerCondition: String,
        processor: String, ram: String, storage: String, imageUrls: List<String>
    ) {
        val laptop = Laptop(
            inventory_id = id,
            brand = brand,
            model = model,
            serial_number = sn,
            condition = condition, // Akan berisi "BAIK" atau "RUSAK: \"...\""
            status = existingLaptop?.status ?: "TERSEDIA",
            location = location,
            pic_lab = picLab,
            procurement_year = procurementYear,
            charger_status = chargerStatus,
            charger_condition = chargerCondition,
            rawImageUrl = imageUrls,
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