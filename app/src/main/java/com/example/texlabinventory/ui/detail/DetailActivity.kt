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
import android.widget.ArrayAdapter
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
import com.example.texlabinventory.data.model.Siswa
import com.example.texlabinventory.data.utils.Resource
import com.example.texlabinventory.databinding.ActivityDetailBinding
import com.example.texlabinventory.databinding.DialogPinjamItemBinding
import com.example.texlabinventory.ui.AddLaptopActivity
import com.example.texlabinventory.ui.adapter.ImageSliderAdapter
import com.example.texlabinventory.ui.viewModel.GuruViewModel
import com.example.texlabinventory.ui.viewModel.LaptopViewModel
import com.example.texlabinventory.ui.viewModel.RuangViewModel
import com.example.texlabinventory.ui.viewModel.SiswaViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private val viewModel: LaptopViewModel by viewModels()

    private val siswaViewModel: SiswaViewModel by viewModels()

    private val ruangViewModel: RuangViewModel by viewModels()

    private val guruViewModel: GuruViewModel by viewModels()

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
            showBorrowDialog(laptop)
        }

        btnReturn.setOnClickListener {
            Toast.makeText(
                this@DetailActivity, // Ganti 'this' dengan 'this@NamaActivityKamu'
                "Pengembalian barang hanya dapat dilakukan melalui menu History Peminjaman!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showBorrowDialog(laptop: Laptop) {
        val dialog = BottomSheetDialog(this)
        val bindingDialog = DialogPinjamItemBinding.inflate(layoutInflater)
        dialog.setContentView(bindingDialog.root)

        // Set info item
        val itemInfo = "${laptop.inventory_id} - ${laptop.brand} ${laptop.model}".trim()
        bindingDialog.etNamaItemPinjam.setText(itemInfo)

        var selectedSiswa: Siswa? = null

        // Set threshold pencarian minimal 3 karakter
        bindingDialog.actvSiswa.threshold = 3

        // Load data siswa
        siswaViewModel.siswaState.observe(this) { resource ->
            when (resource) {
                is Resource.Success -> {
                    val listSiswa = resource.data ?: emptyList()
                    val adapterList = listSiswa.map { "${it.nama} (${it.nis}) - ${it.kelas}" }

                    val adapter = ArrayAdapter(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        adapterList
                    )

                    bindingDialog.actvSiswa.setAdapter(adapter)

                    bindingDialog.actvSiswa.setOnItemClickListener { parent, _, position, _ ->
                        val selectedText = parent.getItemAtPosition(position) as String
                        // Cari object Siswa yang sesuai dari listSiswa
                        selectedSiswa = listSiswa.find { "${it.nama} (${it.nis}) - ${it.kelas}" == selectedText }
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(this, "Gagal memuat siswa: ${resource.message}", Toast.LENGTH_SHORT).show()
                }
                is Resource.Loading -> {
                    // Loading state
                }
            }
        }

        // Load data Ruang via RuangViewModel
        ruangViewModel.ruangState.observe(this) { resource ->
            when (resource) {
                is Resource.Success -> {
                    val listRuang = resource.data?.map { it.nama_ruang }?.filter { it.isNotEmpty() } ?: emptyList()
                    val adapterRuang = ArrayAdapter(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        listRuang
                    )

                    // Diset ke actvRuangan, BUKAN tilRuangan
                    bindingDialog.actvRuangan.setAdapter(adapterRuang)

                    // Event agar dropdown langsung muncul saat diklik/difokuskan
                    bindingDialog.actvRuangan.setOnClickListener {
                        bindingDialog.actvRuangan.showDropDown()
                    }
                    bindingDialog.actvRuangan.setOnTouchListener { _, _ ->
                        bindingDialog.actvRuangan.showDropDown()
                        false
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(this, "Gagal memuat ruang: ${resource.message}", Toast.LENGTH_SHORT).show()
                }
                is Resource.Loading -> { }
            }
        }

        // Load data Guru via GuruViewModel
        guruViewModel.guruState.observe(this) { resource ->
            when (resource) {
                is Resource.Success -> {
                    val listGuru = resource.data?.map { it.nama_guru }?.filter { it.isNotEmpty() } ?: emptyList()
                    val adapterGuru = ArrayAdapter(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        listGuru
                    )

                    bindingDialog.actvGuru.setAdapter(adapterGuru)

                    // Event agar dropdown langsung muncul saat diklik/difokuskan
                    bindingDialog.actvGuru.setOnClickListener {
                        bindingDialog.actvGuru.showDropDown()
                    }
                    bindingDialog.actvGuru.setOnTouchListener { _, _ ->
                        bindingDialog.actvGuru.showDropDown()
                        false
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(this, "Gagal memuat guru: ${resource.message}", Toast.LENGTH_SHORT).show()
                }
                is Resource.Loading -> { }
            }
        }

        siswaViewModel.fetchSiswa()
        ruangViewModel.fetchRuang()
        guruViewModel.fetchGuru()

        bindingDialog.btnBatalPinjam.setOnClickListener {
            dialog.dismiss()
        }

        bindingDialog.btnKonfirmasiPinjam.setOnClickListener {
            val ruangan = bindingDialog.actvRuangan.text.toString().trim()
            val guru = bindingDialog.actvGuru.text.toString().trim()

            if (selectedSiswa == null) {
                Toast.makeText(this, "Pilih siswa peminjam dari daftar rekomendasi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (ruangan.isEmpty() || guru.isEmpty()) {
                Toast.makeText(this, "Lengkapi ruangan dan guru pengajar!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            executeBorrowTransaction(laptop, selectedSiswa!!, ruangan, guru) { success ->
                if (success) {
                    Toast.makeText(this, "Peminjaman berhasil dicatat!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    refreshDetailData(laptop.inventory_id)
                } else {
                    Toast.makeText(this, "Gagal memproses peminjaman", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    private fun executeBorrowTransaction(
        laptop: Laptop,
        siswa: Siswa,
        ruangan: String,
        guru: String,
        onComplete: (Boolean) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()

        // 1. Cari dokumen item berdasarkan field inventory_id
        db.collection("items")
            .whereEqualTo("inventory_id", laptop.inventory_id)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Toast.makeText(this, "Item tidak ditemukan di basis data!", Toast.LENGTH_SHORT).show()
                    onComplete(false)
                    return@addOnSuccessListener
                }

                val itemDocRef = querySnapshot.documents[0].reference
                val newBorrowRef = db.collection("peminjaman").document()

                val peminjamanData = hashMapOf(
                    "id" to newBorrowRef.id,
                    "itemId" to laptop.inventory_id, // Tetap gunakan inventory_id sebagai acuan
                    "namaItem" to "${laptop.brand} ${laptop.model}".trim(),
                    "siswaId" to siswa.nis,
                    "namaSiswa" to siswa.nama,
                    "kelasSiswa" to siswa.kelas,
                    "ruangan" to ruangan,
                    "guruPengajar" to guru,
                    "waktuPinjam" to com.google.firebase.Timestamp.now(),
                    "waktuKembali" to null,
                    "status" to "DIPINJAM"
                )

                // Execute Batch Update
                db.runBatch { batch ->
                    // Simpan Riwayat Peminjaman
                    batch.set(newBorrowRef, peminjamanData)
                    // Update Status Item di Katalog
                    batch.update(itemDocRef, "status", "DIPINJAM")
                }.addOnSuccessListener {
                    onComplete(true)
                }.addOnFailureListener {
                    onComplete(false)
                }
            }
            .addOnFailureListener {
                onComplete(false)
            }
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

//    private fun showReturnDialog(laptop: Laptop) {
//        AlertDialog.Builder(this)
//            .setTitle("Konfirmasi Pengembalian")
//            .setMessage("Apakah Anda yakin laptop ${laptop.brand} ${laptop.model} (${laptop.inventory_id}) sudah dikembalikan?")
//            .setPositiveButton("Ya, Dikembalikan") { _, _ ->
//                executeReturnTransaction(laptop.inventory_id)
//            }
//            .setNegativeButton("Batal", null)
//            .show()
//    }

    private fun executeReturnTransaction(inventoryId: String) {
        Toast.makeText(this, "Memproses pengembalian...", Toast.LENGTH_SHORT).show()
        val db = FirebaseFirestore.getInstance()

        // 1. Cari dokumen peminjaman aktif untuk barang ini
        db.collection("peminjaman")
            .whereEqualTo("itemId", inventoryId)
            .whereEqualTo("status", "DIPINJAM")
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    // Jika tidak ada catatan peminjaman aktif, cukup update status item
                    updateItemStatusToTersedia(inventoryId)
                } else {
                    // Update batch: ubah status peminjaman & status item
                    val batch = db.batch()

                    for (doc in querySnapshot.documents) {
                        val peminjamanRef = db.collection("peminjaman").document(doc.id)
                        batch.update(peminjamanRef, mapOf(
                            "status" to "DIKEMBALIKAN",
                            "waktuKembali" to com.google.firebase.Timestamp.now()
                        ))
                    }

                    val itemRef = db.collection("items").document(inventoryId)
                    batch.update(itemRef, "status", "TERSEDIA")

                    batch.commit()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Barang berhasil dikembalikan!", Toast.LENGTH_SHORT).show()
                            refreshDetailData(inventoryId)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Gagal mengembalikan: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal mencari data peminjaman: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateItemStatusToTersedia(inventoryId: String) {
        FirebaseFirestore.getInstance().collection("items")
            .document(inventoryId)
            .update("status", "TERSEDIA")
            .addOnSuccessListener {
                Toast.makeText(this, "Barang berhasil dikembalikan!", Toast.LENGTH_SHORT).show()
                refreshDetailData(inventoryId)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal mengubah status: ${e.message}", Toast.LENGTH_LONG).show()
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
            .whereEqualTo("inventory_id", inventoryId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val updatedLaptop = querySnapshot.documents[0].toObject(Laptop::class.java)
                    updatedLaptop?.let { setupUI(it) }
                }
            }
    }


}