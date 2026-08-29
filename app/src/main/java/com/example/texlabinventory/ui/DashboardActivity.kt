package com.example.texlabinventory

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
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

    private var selectedDateString: String = ""
    private var activeFilterType: Int = 1

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

        binding.btnNavAkun.setOnClickListener {
            selectMenu(it)
        }
    }

    private fun selectMenu(selectedView: View) {
        binding.btnNavHome.isSelected = (selectedView == binding.btnNavHome)
        binding.btnNavPeminjaman.isSelected = (selectedView == binding.btnNavPeminjaman)
        binding.btnNavInventory.isSelected = (selectedView == binding.btnNavInventory)
        binding.btnNavAkun.isSelected = (selectedView == binding.btnNavAkun)
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
    // isStrictDate = false -> Akumulatif (<= tanggal terpilih). Untuk Inventaris & Kondisi
    // isStrictDate = true  -> Tepat Hari Ini (== tanggal terpilih). Untuk Peminjaman
    private fun isDateValidForFilter(doc: DocumentSnapshot, isStrictDate: Boolean): Boolean {
        val timestamp = doc.getTimestamp("created_at")
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
            val extractedDate = rawDate.take(10) // Ambil format YYYY-MM-DD
            extractedDate <= selectedDateString
        }
    }

    // FILTER 1: JUMLAH LAPTOP PER LAB (Akumulatif s/d Tanggal Terpilih)
    private fun fetchJumlahLaptopReal() {
        dynamicChartListener = db.collection("items").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener

            var cadCount = 0f
            var progCount = 0f

            for (doc in snapshot.documents) {
                if (isDateValidForFilter(doc, isStrictDate = false)) {
                    val lokasi = doc.getString("lokasi")
                        ?: doc.getString("lab")
                        ?: doc.getString("ruangan")
                        ?: doc.getString("ruang") ?: ""

                    if (lokasi.contains("CAD", ignoreCase = true)) {
                        cadCount++
                    } else if (lokasi.contains("Pemrograman", ignoreCase = true) ||
                        lokasi.contains("PROG", ignoreCase = true) ||
                        lokasi.contains("RPL", ignoreCase = true)) {
                        progCount++
                    }
                }
            }

            val entries = arrayListOf(
                BarEntry(0f, cadCount),
                BarEntry(1f, progCount)
            )

            val set = BarDataSet(entries, "Jumlah Laptop").apply {
                colors = listOf(Color.parseColor("#4F46E5"), Color.parseColor("#06B6D4"))
                valueFormatter = integerValueFormatter
                valueTextSize = 12f
                valueTextColor = Color.BLACK
            }

            updateChartUI(set, arrayOf("Lab CAD", "Lab Pemrograman"))
        }
    }

    // FILTER 2: KONDISI LAPTOP (Baik vs Rusak Stacked s/d Tanggal Terpilih)
    private fun fetchKondisiLaptopReal() {
        dynamicChartListener = db.collection("items").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener

            var cadBaik = 0f; var cadRusak = 0f
            var progBaik = 0f; var progRusak = 0f

            for (doc in snapshot.documents) {
                if (isDateValidForFilter(doc, isStrictDate = false)) {
                    val lokasi = doc.getString("lokasi")
                        ?: doc.getString("lab")
                        ?: doc.getString("ruangan")
                        ?: doc.getString("ruang") ?: ""

                    val kondisi = doc.getString("kondisi")
                        ?: doc.getString("status_kondisi")
                        ?: doc.getString("status") ?: "BAIK"

                    val isBaik = kondisi.equals("BAIK", ignoreCase = true) || kondisi.equals("BAGUS", ignoreCase = true)

                    if (lokasi.contains("CAD", ignoreCase = true)) {
                        if (isBaik) cadBaik++ else cadRusak++
                    } else if (lokasi.contains("Pemrograman", ignoreCase = true) ||
                        lokasi.contains("PROG", ignoreCase = true) ||
                        lokasi.contains("RPL", ignoreCase = true)) {
                        if (isBaik) progBaik++ else progRusak++
                    }
                }
            }

            val entries = arrayListOf(
                BarEntry(0f, floatArrayOf(cadBaik, cadRusak)),
                BarEntry(1f, floatArrayOf(progBaik, progRusak))
            )

            val set = BarDataSet(entries, "").apply {
                colors = listOf(Color.parseColor("#10B981"), Color.parseColor("#EF4444"))
                stackLabels = arrayOf("Baik", "Rusak")
                valueFormatter = integerValueFormatter
                valueTextSize = 12f
                valueTextColor = Color.WHITE
            }

            updateChartUI(set, arrayOf("Lab CAD", "Lab Pemrograman"))
        }
    }

    // FILTER 3: PEMINJAMAN (Khusus di Tanggal Terpilih)
    private fun fetchPeminjamanReal() {
        dynamicChartListener = db.collection("peminjaman").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener

            var cadPinjam = 0f; var cadKembali = 0f
            var progPinjam = 0f; var progKembali = 0f

            for (doc in snapshot.documents) {
                if (isDateValidForFilter(doc, isStrictDate = true)) {
                    val lokasi = doc.getString("lab")
                        ?: doc.getString("lokasi")
                        ?: doc.getString("ruangan") ?: ""

                    val status = doc.getString("status") ?: ""
                    val isDipinjam = status.equals("DIPINJAM", ignoreCase = true)

                    if (lokasi.contains("CAD", ignoreCase = true)) {
                        if (isDipinjam) cadPinjam++ else cadKembali++
                    } else if (lokasi.contains("Pemrograman", ignoreCase = true) ||
                        lokasi.contains("PROG", ignoreCase = true) ||
                        lokasi.contains("RPL", ignoreCase = true)) {
                        if (isDipinjam) progPinjam++ else progKembali++
                    }
                }
            }

            val entries = arrayListOf(
                BarEntry(0f, floatArrayOf(cadPinjam, cadKembali)),
                BarEntry(1f, floatArrayOf(progPinjam, progKembali))
            )

            val set = BarDataSet(entries, "").apply {
                colors = listOf(Color.parseColor("#3B82F6"), Color.parseColor("#F59E0B"))
                stackLabels = arrayOf("Dipinjam", "Dikembalikan")
                valueFormatter = integerValueFormatter
                valueTextSize = 12f
                valueTextColor = Color.WHITE
            }

            updateChartUI(set, arrayOf("Lab CAD", "Lab Pemrograman"))
        }
    }

    private fun updateChartUI(dataSet: BarDataSet, labels: Array<String>) {
        binding.barChartLaptop.apply {
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.labelCount = labels.size

            val barData = BarData(dataSet)
            barData.barWidth = 0.45f
            data = barData

            fitScreen()
            notifyDataSetChanged()
            invalidate()
            animateY(400)
        }
    }

    private fun observeDashboardData() {
        laptopListener = db.collection("items").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            binding.tvTotalInventaris.text = snapshot.size().toString()

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

    private fun styleChart(chart: BarChart) {
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            setDrawGridBackground(false)
            setDrawBorders(false)
            setDrawValueAboveBar(true)

            xAxis.apply {
                setDrawGridLines(false)
                setDrawAxisLine(true)
                setDrawLabels(true)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                axisMinimum = -0.5f
                axisMaximum = 1.5f
                textColor = Color.BLACK
                textSize = 12f
            }

            axisLeft.apply {
                setDrawGridLines(true)
                setDrawAxisLine(true)
                setDrawLabels(true)
                axisMinimum = 0f
                granularity = 1f
            }

            axisRight.isEnabled = false
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