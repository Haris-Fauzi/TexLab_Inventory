package com.example.texlabinventory.ui.detail

import android.app.Dialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.texlabinventory.R
import com.example.texlabinventory.data.model.Laptop
import com.example.texlabinventory.data.utils.Resource
import com.example.texlabinventory.databinding.ActivityDetailBinding
import com.example.texlabinventory.ui.AddLaptopActivity
import com.example.texlabinventory.ui.adapter.ImageSliderAdapter
import com.example.texlabinventory.ui.viewModel.LaptopViewModel
import com.google.firebase.firestore.FirebaseFirestore

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private val viewModel: LaptopViewModel by viewModels()

    companion object {
        const val EXTRA_LAPTOP = "extra_laptop"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Status Bar transparan & responsif terhadap tema
        setupStatusBarTheme()

        // 2. Padding Top menyesuaikan Status Bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(0, statusBarHeight, 0, 0)
            insets
        }

        val laptop = intent.getSerializableExtra(EXTRA_LAPTOP) as? Laptop
        if (laptop != null) {
            setupUI(laptop)
            setupActionButtons(laptop)
        }
    }

    private fun setupStatusBarTheme() {
        window.statusBarColor = Color.TRANSPARENT
        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !isDarkMode
    }

    private fun setupUI(laptop: Laptop) = with(binding) {
        // Identitas Laptop
        tvDetailId.text = "ID: ${laptop.inventory_id}"

        val brandModelText = if (laptop.brand.isNotEmpty()) {
            "${laptop.brand} - ${laptop.model}"
        } else {
            laptop.model
        }
        tvDetailBrandModel.text = brandModelText

        tvDetailSN.text = "Serial Number: ${laptop.serial_number}"

        // Lokasi & Aset
        tvDetailLocation.text = "📍 Lokasi: ${laptop.location}"
        tvDetailPicLab.text = "👤 PIC Lab: ${laptop.pic_lab.ifEmpty { "-" }}"
        tvDetailYear.text = "📅 Tahun Pengadaan: ${if (laptop.procurement_year != 0L) laptop.procurement_year else "-"}"
        tvDetailCondition.text = "🛠️ Kondisi Laptop: ${laptop.condition.ifEmpty { "-" }}"

        // Info Charger
        tvDetailChargerStatus.text = "🔌 Status Charger: ${laptop.charger_status.ifEmpty { "-" }}"
        tvDetailChargerCondition.text = "⚡ Kondisi Charger: ${laptop.charger_condition.ifEmpty { "-" }}"

        // Spesifikasi
        tvDetailProcessor.text = "Processor: ${laptop.specs.processor.ifEmpty { "-" }}"
        tvDetailRam.text = "RAM: ${laptop.specs.ram.ifEmpty { "-" }}"
        tvDetailStorage.text = "Penyimpanan: ${laptop.specs.storage.ifEmpty { "-" }}"

        // Badge Status Peminjaman
        val isDipinjam = laptop.status.equals("DIPINJAM", ignoreCase = true)
        tvDetailStatus.text = if (isDipinjam) "DIPINJAM" else "TERSEDIA"

        val statusColorRes = if (isDipinjam) R.color.status_dipinjam else R.color.status_tersedia
        tvDetailStatus.backgroundTintList = ContextCompat.getColorStateList(this@DetailActivity, statusColorRes)

        val status = laptop.status // Misal: "TERSEDIA" atau "DIPINJAM"

        if (status.equals("TERSEDIA", ignoreCase = true)) {
            btnBorrow.visibility = View.VISIBLE
            btnReturn.visibility = View.GONE
        } else {
            btnBorrow.visibility = View.GONE
            btnReturn.visibility = View.VISIBLE
        }

        // Panggil Slider Gambar
        setupImageSlider(laptop.image_url)
    }

    private fun setupImageSlider(imageUrls: List<String>) {
        if (imageUrls.isEmpty()) {
            binding.tvImageIndicator.text = "0/0"
            return
        }

        val adapter = ImageSliderAdapter(imageUrls) { selectedUrl ->
            val initialPosition = imageUrls.indexOf(selectedUrl)
            showZoomImageDialog(imageUrls, if (initialPosition != -1) initialPosition else 0)
        }

        binding.vpImageSlider.adapter = adapter
        binding.vpImageSlider.isUserInputEnabled = true
        binding.tvImageIndicator.text = "1/${imageUrls.size}"

        binding.vpImageSlider.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.tvImageIndicator.text = "${position + 1}/${imageUrls.size}"
            }
        })
    }

    private fun showZoomImageDialog(imageUrls: List<String>, startPosition: Int) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.full_image)

        dialog.window?.let { window ->
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        val vpFullImage = dialog.findViewById<ViewPager2>(R.id.vpFullImage)
        val tvZoomIndicator = dialog.findViewById<TextView>(R.id.tvZoomIndicator)
        val btnClose = dialog.findViewById<ImageButton>(R.id.btnClose)

        val adapter = ImageSliderAdapter(imageUrls) { }
        vpFullImage.adapter = adapter
        vpFullImage.setCurrentItem(startPosition, false)
        tvZoomIndicator.text = "${startPosition + 1}/${imageUrls.size}"

        vpFullImage.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                tvZoomIndicator.text = "${position + 1}/${imageUrls.size}"
            }
        })

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupActionButtons(laptop: Laptop) = with(binding) {
        val isDipinjam = laptop.status.equals("DIPINJAM", ignoreCase = true)

        btnBorrow.isEnabled = !isDipinjam
        btnReturn.isEnabled = isDipinjam

        btnDelete.setOnClickListener {
            showDeleteDialog(laptop.inventory_id)
        }

        btnEdit.setOnClickListener {
            val intent = Intent(this@DetailActivity, AddLaptopActivity::class.java).apply {
                putExtra("EXTRA_LAPTOP_EDIT", laptop)
                putExtra("IS_EDIT_MODE", true)
            }
            startActivity(intent)
        }

        btnBorrow.setOnClickListener {
            showBorrowConfirmationDialog(laptop, "DIPINJAM")
        }

        btnReturn.setOnClickListener {
            showBorrowConfirmationDialog(laptop, "TERSEDIA")
        }
    }

    private fun showBorrowConfirmationDialog(laptop: Laptop, newStatus: String) {
        val actionText = if (newStatus == "DIPINJAM") "meminjam" else "mengembalikan"

        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Status")
            .setMessage("Apakah kamu yakin ingin $actionText laptop ${laptop.inventory_id} ${laptop.brand} ${laptop.model}?")
            .setPositiveButton("Ya") { _, _ ->
                updateBorrowStatus(laptop.inventory_id, newStatus)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun updateBorrowStatus(inventoryId: String, newStatus: String) {
        Toast.makeText(this, "Memproses perubahan status...", Toast.LENGTH_SHORT).show()

        FirebaseFirestore.getInstance().collection("items")
            .document(inventoryId)
            .update("status", newStatus)
            .addOnSuccessListener {
                val message = if (newStatus == "DIPINJAM") {
                    "Berhasil dipinjam!"
                } else {
                    "Berhasil dikembalikan!"
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                refreshDetailData(inventoryId)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Gagal mengubah status: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showDeleteDialog(inventoryId: String) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Laptop")
            .setMessage("Apakah kamu yakin ingin menghapus data aset ini?")
            .setPositiveButton("Hapus") { _, _ -> executeDelete(inventoryId) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun executeDelete(inventoryId: String) {
        viewModel.deleteLaptop(inventoryId) { result ->
            when (result) {
                is Resource.Loading -> Toast.makeText(this, "Menghapus data...", Toast.LENGTH_SHORT).show()
                is Resource.Success -> {
                    Toast.makeText(this, "Berhasil menghapus aset!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is Resource.Error -> Toast.makeText(this, "Gagal: ${result.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val laptop = intent.getSerializableExtra(EXTRA_LAPTOP) as? Laptop
        laptop?.let {
            refreshDetailData(it.inventory_id)
        }
    }

    private fun refreshDetailData(inventoryId: String) {
        FirebaseFirestore.getInstance().collection("items")
            .document(inventoryId)
            .get()
            .addOnSuccessListener { document ->
                val updatedLaptop = document.toObject(Laptop::class.java)
                updatedLaptop?.let { setupUI(it) }
            }
    }
}