package com.example.texlabinventory

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.texlabinventory.databinding.ActivityDashboardBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val db = FirebaseFirestore.getInstance()

    // Realtime listeners
    private var laptopListener: ListenerRegistration? = null
    private var siswaListener: ListenerRegistration? = null
    private var historyListener: ListenerRegistration? = null
    private var dynamicChartListener: ListenerRegistration? = null

    private val itemLocationMap = mutableMapOf<String, String>()

    private var selectedDateString: String = ""
    private var activeFilterType: Int = 1

    // Scanner Launcher yang sudah disanitasi
    private val scanLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            var scannedBarcode = result.data?.getStringExtra("EXTRA_BARCODE_RESULT") ?: ""

            // 1. Bersihkan enter (\n, \r) dan spasi tidak terlihat
            scannedBarcode = scannedBarcode.replace("\n", "").replace("\r", "").trim()

            // 2. Jika isi QR Code berupa URL (contoh: https://domain.com/LTP-001), ambil ID di bagian akhir
            if (scannedBarcode.contains("/")) {
                scannedBarcode = scannedBarcode.substringAfterLast("/")
            }

            if (scannedBarcode.isNotEmpty()) {
                Toast.makeText(this, "Mencari laptop: $scannedBarcode...", Toast.LENGTH_SHORT).show()
                fetchLaptopAndNavigateToDetail(scannedBarcode)
            } else {
                Toast.makeText(this, "Hasil scan kosong!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchLaptopAndNavigateToDetail(inventoryId: String) {
        val collectionRef = db.collection("items")

        // Query 1: Pencarian Exact Match
        collectionRef.whereEqualTo("inventory_id", inventoryId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    navigateToDetail(querySnapshot.documents[0])
                } else {
                    // Query 2: Fallback jika ID di Firestore berformat UPPERCASE (contoh: "LTP-001")
                    collectionRef.whereEqualTo("inventory_id", inventoryId.uppercase())
                        .get()
                        .addOnSuccessListener { upperSnapshot ->
                            if (!upperSnapshot.isEmpty) {
                                navigateToDetail(upperSnapshot.documents[0])
                            } else {
                                Toast.makeText(this, "Laptop dengan ID '$inventoryId' tidak ditemukan!", Toast.LENGTH_LONG).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Gagal mengambil data: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal mengambil data: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun navigateToDetail(document: DocumentSnapshot) {
        val laptop = document.toObject(com.example.texlabinventory.data.model.Laptop::class.java)
        if (laptop != null) {
            val isDipinjam = laptop.status.equals("DIPINJAM", ignoreCase = true)

            if (isDipinjam) {
                // Jika DIPINJAM, arahkan langsung ke HistoryPeminjamanActivity
                val intent = Intent(this, com.example.texlabinventory.ui.HistoryPeminjamanActivity::class.java).apply {
                    putExtra("EXTRA_AUTO_RETURN_ITEM_ID", laptop.inventory_id)
                    putExtra("EXTRA_AUTO_OPEN_RETURN", true)
                }
                startActivity(intent)
            } else {
                // Jika TERSEDIA, buka DetailActivity dan langsung tampilkan dialog pinjam
                val intent = Intent(this, com.example.texlabinventory.ui.detail.DetailActivity::class.java).apply {
                    putExtra(com.example.texlabinventory.ui.detail.DetailActivity.EXTRA_LAPTOP, laptop)
                    putExtra("EXTRA_AUTO_OPEN_LOAN", true)
                }
                startActivity(intent)
            }
        } else {
            Toast.makeText(this, "Gagal mengonversi data laptop!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set default tanggal hari ini (Format YYYY-MM-DD untuk query database)
        val sdfDisplay = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sdfQuery = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = Date()

        binding.btnFilterTanggal.text = "${sdfDisplay.format(today)} 📅"
        selectedDateString = sdfQuery.format(today)

        setupClickListeners()
        setupBottomNav()
        setupWindowInsets()
        observeDashboardData()
        setupChartFilter()
        setupDatePicker()

        styleChart(binding.barChartLaptop)
        loadRealChartData()
    }

    override fun onResume() {
        super.onResume()
        selectMenu(binding.btnNavHome)
    }

    private fun setupClickListeners() {
        binding.cardInventaris.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        binding.cardSiswa.setOnClickListener {
            startActivity(Intent(this, com.example.texlabinventory.ui.SiswaActivity::class.java))
        }

        binding.cardHistory.setOnClickListener {
            startActivity(Intent(this, com.example.texlabinventory.ui.HistoryPeminjamanActivity::class.java))
        }

        binding.cardLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(this, "Berhasil Logout", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun setupBottomNav() {
        selectMenu(binding.btnNavHome)

        binding.btnNavHome.setOnClickListener {
            selectMenu(it)
        }

        binding.btnNavPeminjaman.setOnClickListener {
            selectMenu(it)
            startActivity(Intent(this, com.example.texlabinventory.ui.HistoryPeminjamanActivity::class.java))
        }

        binding.btnNavInventory.setOnClickListener {
            selectMenu(it)
            startActivity(Intent(this, MainActivity::class.java))
        }

        binding.btnNavMasterData.setOnClickListener {
            selectMenu(it)
            startActivity(Intent(this, com.example.texlabinventory.ui.MasterDataActivity::class.java))
        }

        binding.btnNavScan.setOnClickListener {
            val intent = Intent(this, com.example.texlabinventory.ui.ScanActivity::class.java)
            scanLauncher.launch(intent)
        }
    }

    private fun selectMenu(selectedView: View) {
        binding.btnNavHome.isSelected = (selectedView == binding.btnNavHome)
        binding.btnNavPeminjaman.isSelected = (selectedView == binding.btnNavPeminjaman)
        binding.btnNavInventory.isSelected = (selectedView == binding.btnNavInventory)
        binding.btnNavMasterData.isSelected = (selectedView == binding.btnNavMasterData)
    }

    private fun setupChartFilter() {
        binding.btnFilterLab.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add("1. Jumlah Laptop per Lab")
            popup.menu.add("2. Kondisi Laptop (Baik vs Rusak)")
            popup.menu.add("3. Peminjaman Tanggal Ini")

            popup.setOnMenuItemClickListener { item ->
                binding.btnFilterLab.text = "${item.title} ▾"
                when (item.title.toString()) {
                    "1. Jumlah Laptop per Lab" -> activeFilterType = 1
                    "2. Kondisi Laptop (Baik vs Rusak)" -> activeFilterType = 2
                    "3. Peminjaman Tanggal Ini" -> activeFilterType = 3
                }
                loadRealChartData()
                true
            }
            popup.show()
        }
    }

    private fun setupDatePicker() {
        binding.btnFilterTanggal.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = android.app.DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val formattedDisplay = String.format(Locale.getDefault(), "%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                    selectedDateString = String.format(Locale.getDefault(), "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)

                    binding.btnFilterTanggal.text = "$formattedDisplay 📅"
                    loadRealChartData()
                },
                year, month, day
            )
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
            datePickerDialog.show()
        }
    }

    private fun loadRealChartData() {
        dynamicChartListener?.remove()

        when (activeFilterType) {
            1 -> fetchJumlahLaptopReal()
            2 -> fetchKondisiLaptopReal()
            3 -> fetchPeminjamanReal()
        }
    }

    // --- HELPER LOGIKA TANGGAL ---
    private fun isDateValidForFilter(doc: DocumentSnapshot, isStrictDate: Boolean): Boolean {
        val timestamp = doc.getTimestamp("created_at")
            ?: doc.getTimestamp("waktuPinjam")
            ?: doc.getTimestamp("tanggal")
            ?: doc.getTimestamp("tgl_masuk")
            ?: doc.getTimestamp("tgl_pinjam")

        val sdfQuery = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        if (timestamp != null) {
            val docDateStr = sdfQuery.format(timestamp.toDate())
            return if (isStrictDate) docDateStr == selectedDateString else docDateStr <= selectedDateString
        }

        val rawDate = doc.getString("created_at")
            ?: doc.getString("tanggal")
            ?: doc.getString("tgl_masuk")
            ?: doc.getString("tgl_pinjam")
            ?: ""

        if (rawDate.isEmpty()) return !isStrictDate

        return if (isStrictDate) {
            rawDate.contains(selectedDateString)
        } else {
            val extractedDate = rawDate.take(10)
            extractedDate <= selectedDateString
        }
    }

    // 1. FILTER 1: JUMLAH LAPTOP PER LAB
    private fun fetchJumlahLaptopReal() {
        dynamicChartListener = db.collection("items").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener

            var cadCount = 0f
            var progCount = 0f

            for (doc in snapshot.documents) {
                if (isDateValidForFilter(doc, isStrictDate = false)) {
                    val location = doc.getString("location")
                        ?: doc.getString("lokasi")
                        ?: doc.getString("lab") ?: ""

                    if (location.contains("CAD", ignoreCase = true)) {
                        cadCount++
                    } else if (location.contains("Pemrograman", ignoreCase = true) ||
                        location.contains("PROG", ignoreCase = true) ||
                        location.contains("RPL", ignoreCase = true)) {
                        progCount++
                    }
                }
            }

            val entries = arrayListOf(
                BarEntry(0f, cadCount),
                BarEntry(1f, progCount)
            )

            // Warna dinamis mengikuti tema Light/Dark
            val dynamicTextColor = ContextCompat.getColor(this, R.color.text_primary_light)

            val set = BarDataSet(entries, "Jumlah Laptop").apply {
                colors = listOf(Color.parseColor("#6366F1"), Color.parseColor("#0EA5E9"))
                valueFormatter = integerValueFormatter
                valueTextSize = 12f
                valueTextColor = dynamicTextColor
            }

            binding.barChartLaptop.setDrawValueAboveBar(true)
            updateChartUI(set, arrayOf("Lab CAD", "Lab Pemrograman"))
        }
    }

    // 2. FILTER 2: KONDISI LAPTOP (Baik vs Rusak)
    private fun fetchKondisiLaptopReal() {
        dynamicChartListener = db.collection("items").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener

            var cadBaik = 0f; var cadRusak = 0f
            var progBaik = 0f; var progRusak = 0f

            for (doc in snapshot.documents) {
                if (isDateValidForFilter(doc, isStrictDate = false)) {
                    val location = doc.getString("location")
                        ?: doc.getString("lokasi")
                        ?: doc.getString("lab") ?: ""

                    // Mengambil nilai field condition / kondisi
                    val rawCondition = doc.getString("condition")
                        ?: doc.getString("kondisi") ?: ""

                    // Logika Wajib: Hanya "BAIK" yang dianggap baik, selain itu (termasuk null/kosong/rusak) dianggap rusak
                    val isBaik = rawCondition.trim().equals("BAIK", ignoreCase = true)

                    if (location.contains("CAD", ignoreCase = true)) {
                        if (isBaik) cadBaik++ else cadRusak++
                    } else if (location.contains("Pemrograman", ignoreCase = true) ||
                        location.contains("PROG", ignoreCase = true) ||
                        location.contains("RPL", ignoreCase = true)) {
                        if (isBaik) progBaik++ else progRusak++
                    }
                }
            }

            // Stacked Bar Data Entry (Nilai Baik di bawah, Nilai Rusak di atas)
            val entries = arrayListOf(
                BarEntry(0f, floatArrayOf(cadBaik, cadRusak)),
                BarEntry(1f, floatArrayOf(progBaik, progRusak))
            )

            val dynamicTextColor = ContextCompat.getColor(this, R.color.text_primary_light)

            val set = BarDataSet(entries, "").apply {
                // Hijau untuk Baik (#10B981), Merah/Pink untuk Rusak (#F43F5E)
                colors = listOf(Color.parseColor("#10B981"), Color.parseColor("#F43F5E"))
                stackLabels = arrayOf("Baik", "Rusak")
                valueFormatter = integerValueFormatter
                valueTextSize = 11f
                valueTextColor = dynamicTextColor
            }

            binding.barChartLaptop.setDrawValueAboveBar(false)
            updateChartUI(set, arrayOf("Lab CAD", "Lab Pemrograman"))
        }
    }

    // 3. FILTER 3: PEMINJAMAN (Khusus di Tanggal Terpilih berdasarkan Lokasi Asli)
    private fun fetchPeminjamanReal() {
        dynamicChartListener = db.collection("peminjaman").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener

            var cadPinjam = 0f; var cadKembali = 0f
            var progPinjam = 0f; var progKembali = 0f

            for (doc in snapshot.documents) {
                if (isDateValidForFilter(doc, isStrictDate = true)) {
                    val itemIdFromLoan = doc.getString("itemId") ?: ""
                    val lokasiAsliLaptop = itemLocationMap[itemIdFromLoan] ?: ""

                    val status = doc.getString("status") ?: ""
                    val isDipinjam = status.equals("DIPINJAM", ignoreCase = true)

                    if (lokasiAsliLaptop.contains("CAD", ignoreCase = true)) {
                        if (isDipinjam) cadPinjam++ else cadKembali++
                    } else if (lokasiAsliLaptop.contains("Pemrograman", ignoreCase = true) ||
                        lokasiAsliLaptop.contains("PROG", ignoreCase = true) ||
                        lokasiAsliLaptop.contains("RPL", ignoreCase = true)) {
                        if (isDipinjam) progPinjam++ else progKembali++
                    }
                }
            }

            val entries = arrayListOf(
                BarEntry(0f, floatArrayOf(cadPinjam, cadKembali)),
                BarEntry(1f, floatArrayOf(progPinjam, progKembali))
            )

            val dynamicTextColor = ContextCompat.getColor(this, R.color.text_primary_light)

            val set = BarDataSet(entries, "").apply {
                colors = listOf(Color.parseColor("#3B82F6"), Color.parseColor("#F59E0B"))
                stackLabels = arrayOf("Dipinjam", "Dikembalikan")
                valueFormatter = integerValueFormatter
                valueTextSize = 11f
                valueTextColor = dynamicTextColor
            }

            binding.barChartLaptop.setDrawValueAboveBar(false)
            updateChartUI(set, arrayOf("Lab CAD", "Lab Pemrograman"))
        }
    }

    // 4. PENYESUAIAN GAYA CHART (MENDUKUNG DARK/LIGHT MODE DINAMIS)
    private fun styleChart(chart: BarChart) {
        chart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBorders(false)

            val dynamicTextColor = ContextCompat.getColor(this@DashboardActivity, R.color.text_primary_light)

            // Mengatur warna legenda (Kotak keterangan di bawah sumbu X)
            legend.apply {
                isEnabled = true
                textColor = dynamicTextColor
            }

            xAxis.apply {
                setDrawGridLines(false)
                setDrawAxisLine(true)
                setDrawLabels(true)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                axisMinimum = -0.5f
                textColor = dynamicTextColor
                textSize = 12f
            }

            axisLeft.apply {
                setDrawGridLines(true)
                setDrawAxisLine(true)
                setDrawLabels(true)
                axisMinimum = 0f
                granularity = 1f
                textColor = dynamicTextColor
            }

            axisRight.isEnabled = false
        }
    }

    private fun updateChartUI(dataSet: BarDataSet, labels: Array<String>) {
        binding.barChartLaptop.apply {
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.labelCount = labels.size

            val barData = BarData(dataSet)
            barData.barWidth = 0.4f
            data = barData

            axisLeft.resetAxisMaximum()
            axisLeft.axisMinimum = 0f

            notifyDataSetChanged()
            invalidate()
            animateY(500)
        }
    }

    private fun observeDashboardData() {
        laptopListener = db.collection("items").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            binding.tvTotalInventaris.text = snapshot.size().toString()

            itemLocationMap.clear()
            for (doc in snapshot.documents) {
                val itemId = doc.getString("inventory_id") ?: ""
                val originLocation = doc.getString("location") ?: ""

                if (itemId.isNotEmpty()) {
                    itemLocationMap[itemId] = originLocation
                }
            }

            val totalDipinjam = snapshot.documents.count { doc ->
                val status = doc.getString("status")
                status.equals("DIPINJAM", ignoreCase = true)
            }
            binding.tvTotalDipinjam.text = totalDipinjam.toString()
        }

        siswaListener = db.collection("siswa").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            binding.tvTotalSiswa.text = snapshot.size().toString()
        }

        historyListener = db.collection("peminjaman").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            binding.tvTotalHistory.text = snapshot.size().toString()
        }
    }

    private val integerValueFormatter = object : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return if (value > 0) value.toInt().toString() else ""
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            binding.root.setPadding(0, statusBarHeight, 0, 0)
            insets
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        laptopListener?.remove()
        siswaListener?.remove()
        historyListener?.remove()
        dynamicChartListener?.remove()
    }
}