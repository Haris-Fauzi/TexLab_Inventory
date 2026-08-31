package com.example.texlabinventory.ui

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Pair
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.texlabinventory.DashboardActivity
import com.example.texlabinventory.LoginActivity
import com.example.texlabinventory.MainActivity
import com.example.texlabinventory.R
import com.example.texlabinventory.data.model.Peminjaman
import com.example.texlabinventory.data.utils.Resource
import com.example.texlabinventory.databinding.ActivityHistoryPeminjamanBinding
import com.example.texlabinventory.databinding.DialogKembalikanItemBinding
import com.example.texlabinventory.ui.adapter.HistoryPeminjamanAdapter
import com.example.texlabinventory.ui.viewModel.HistoryPeminjamanViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryPeminjamanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryPeminjamanBinding
    private val viewModel: HistoryPeminjamanViewModel by viewModels()
    private lateinit var historyAdapter: HistoryPeminjamanAdapter
    private lateinit var toggle: ActionBarDrawerToggle
    private var fullList: List<Peminjaman> = emptyList()

    // Variable penampung status filter
    private var selectedStatusFilter: String = "SEMUA"
    private var selectedStartDate: Long? = null
    private var selectedEndDate: Long? = null

    // Flag untuk menangani otomatisasi pengembalian dari hasil scan
    private var isAutoReturnProcessed = false

    companion object {
        const val EXTRA_AUTO_RETURN_ITEM_ID = "EXTRA_AUTO_RETURN_ITEM_ID"
        const val EXTRA_AUTO_OPEN_RETURN = "EXTRA_AUTO_OPEN_RETURN"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityHistoryPeminjamanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        setupNavigationDrawer()
        setupRecyclerView()
        setupSearch()
        setupFilterButton()
        setupBackPressed()
        observeViewModel()

        viewModel.fetchHistoryPeminjaman()
    }

    private fun setupNavigationDrawer() {
        setSupportActionBar(binding.toolbarHistory)

        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbarHistory,
            android.R.string.ok,
            android.R.string.cancel
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navigationView.itemIconTintList = null

        val headerView = binding.navigationView.getHeaderView(0)
        headerView.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        binding.navigationView.setCheckedItem(R.id.nav_history)

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_inventaris -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                R.id.nav_master_data -> {
                    val intent = Intent(this, MasterDataActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                R.id.nav_history -> {
                    // Sudah berada di halaman ini
                }
                R.id.nav_logout -> {
                    FirebaseAuth.getInstance().signOut()
                    Toast.makeText(this, "Berhasil Logout", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryPeminjamanAdapter { peminjaman ->
            if (peminjaman.status.equals("DIPINJAM", ignoreCase = true)) {
                showKonfirmasiKembaliDialog(peminjaman)
            }
        }
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(this@HistoryPeminjamanActivity)
            adapter = historyAdapter
        }
    }

    private fun showKonfirmasiKembaliDialog(item: Peminjaman) {
        val dialog = BottomSheetDialog(this)
        val dialogBinding = DialogKembalikanItemBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        // 1. Transparankan background window dialog
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // 2. Wajib: Hilangkan background container bawaan BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.background = null
        }

        // Format Tanggal
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
        val tglFormatted = item.waktuPinjam?.toDate()?.let { sdf.format(it) } ?: "-"

        // Set Data ke UI via ViewBinding
        dialogBinding.tvDialogNamaItem.text = "${item.namaItem ?: "-"} (${item.itemId ?: "-"})"
        dialogBinding.tvDialogPeminjam.text = item.namaSiswa ?: "-"
        dialogBinding.tvDialogKelasNis.text = "${item.kelasSiswa ?: "-"} (${item.siswaId ?: "-"})"
        dialogBinding.tvDialogRuangan.text = item.ruangan ?: "-"
        dialogBinding.tvDialogWaktuPinjam.text = tglFormatted

        // Event Listener Tombol
        dialogBinding.btnDialogBatal.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnDialogKembalikan.setOnClickListener {
            viewModel.kembalikanBarang(item)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupSearch() {
        binding.etSearchHistory.doOnTextChanged { text, _, _, _ ->
            applyCurrentFilter(text.toString())
        }
    }

    private fun setupFilterButton() {
        binding.btnFilterHistory.setOnClickListener {
            showFilterBottomSheet()
        }
    }

    private fun showFilterBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_filter, null)
        dialog.setContentView(dialogView)

        val cgStatus = dialogView.findViewById<ChipGroup>(R.id.cgStatus)
        val etDateRange = dialogView.findViewById<TextInputEditText>(R.id.etDateRange)
        val btnReset = dialogView.findViewById<MaterialButton>(R.id.btnReset)
        val btnApply = dialogView.findViewById<MaterialButton>(R.id.btnApply)

        var tempStartDate = selectedStartDate
        var tempEndDate = selectedEndDate

        if (tempStartDate != null && tempEndDate != null) {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
            etDateRange.setText("${sdf.format(Date(tempStartDate))} - ${sdf.format(Date(tempEndDate))}")
        }

        when (selectedStatusFilter) {
            "DIPINJAM" -> dialogView.findViewById<Chip>(R.id.chipBorrowed)?.isChecked = true
            "DIKEMBALIKAN" -> dialogView.findViewById<Chip>(R.id.chipReturned)?.isChecked = true
            else -> dialogView.findViewById<Chip>(R.id.chipAll)?.isChecked = true
        }

        etDateRange.setOnClickListener {
            val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Pilih Rentang Tanggal")
                .apply {
                    if (tempStartDate != null && tempEndDate != null) {
                        setSelection(Pair(tempStartDate, tempEndDate))
                    }
                }
                .build()

            dateRangePicker.show(supportFragmentManager, "DATE_RANGE_PICKER")

            dateRangePicker.addOnPositiveButtonClickListener { selection ->
                tempStartDate = selection.first
                tempEndDate = selection.second

                val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
                val startFormatted = sdf.format(Date(tempStartDate!!))
                val endFormatted = sdf.format(Date(tempEndDate!!))

                etDateRange.setText("$startFormatted - $endFormatted")
            }
        }

        btnReset.setOnClickListener {
            selectedStatusFilter = "SEMUA"
            selectedStartDate = null
            selectedEndDate = null

            applyCurrentFilter(binding.etSearchHistory.text.toString())
            dialog.dismiss()
        }

        btnApply.setOnClickListener {
            val checkedChipId = cgStatus.checkedChipId
            selectedStatusFilter = when (checkedChipId) {
                R.id.chipBorrowed -> "DIPINJAM"
                R.id.chipReturned -> "DIKEMBALIKAN"
                else -> "SEMUA"
            }

            selectedStartDate = tempStartDate
            selectedEndDate = tempEndDate

            applyCurrentFilter(binding.etSearchHistory.text.toString())
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun applyCurrentFilter(query: String) {
        val filteredList = fullList.filter { item ->
            val matchesSearch = query.trim().isEmpty() ||
                    (item.namaItem?.contains(query, ignoreCase = true) == true) ||
                    (item.namaSiswa?.contains(query, ignoreCase = true) == true) ||
                    (item.siswaId?.contains(query, ignoreCase = true) == true) ||
                    (item.itemId?.contains(query, ignoreCase = true) == true) ||
                    (item.ruangan?.contains(query, ignoreCase = true) == true) ||
                    (item.kelasSiswa?.contains(query, ignoreCase = true) == true)

            val matchesStatus = if (selectedStatusFilter == "SEMUA") {
                true
            } else {
                item.status.equals(selectedStatusFilter, ignoreCase = true)
            }

            val itemTimestamp = item.waktuPinjam?.toDate()?.time ?: 0L
            val matchesDate = if (selectedStartDate != null && selectedEndDate != null) {
                itemTimestamp in selectedStartDate!!..(selectedEndDate!! + 86399000L)
            } else {
                true
            }

            matchesSearch && matchesStatus && matchesDate
        }

        historyAdapter.submitList(filteredList)

        if (filteredList.isEmpty()) {
            binding.layoutEmptyStateHistory.visibility = View.VISIBLE
            binding.rvHistory.visibility = View.GONE
        } else {
            binding.layoutEmptyStateHistory.visibility = View.GONE
            binding.rvHistory.visibility = View.VISIBLE
        }
    }

    private fun setupBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else if (binding.etSearchHistory.hasFocus()) {
                    hideKeyboardAndUnfocus()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun hideKeyboardAndUnfocus() {
        binding.etSearchHistory.clearFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearchHistory.windowToken, 0)
    }

    private fun observeViewModel() {
        viewModel.historyState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBarHistory.visibility = View.VISIBLE
                    binding.rvHistory.visibility = View.GONE
                    binding.layoutEmptyStateHistory.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressBarHistory.visibility = View.GONE
                    fullList = resource.data ?: emptyList()
                    applyCurrentFilter(binding.etSearchHistory.text.toString())

                    // Cek ketersediaan Intent Auto Return dari Scan QR Code
                    checkAutoOpenReturnFromIntent()
                }
                is Resource.Error -> {
                    binding.progressBarHistory.visibility = View.GONE
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.actionState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBarHistory.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.progressBarHistory.visibility = View.GONE
                    Toast.makeText(this, resource.data, Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> {
                    binding.progressBarHistory.visibility = View.GONE
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun checkAutoOpenReturnFromIntent() {
        val autoOpenReturn = intent.getBooleanExtra(EXTRA_AUTO_OPEN_RETURN, false)
        val itemId = intent.getStringExtra(EXTRA_AUTO_RETURN_ITEM_ID)

        if (autoOpenReturn && !itemId.isNullOrEmpty() && !isAutoReturnProcessed) {
            isAutoReturnProcessed = true

            // Cari data transaksi peminjaman aktif yang berstatus DIPINJAM berdasarkan ID Laptop
            val activeLoan = fullList.find {
                it.itemId.equals(itemId, ignoreCase = true) && it.status.equals("DIPINJAM", ignoreCase = true)
            }

            if (activeLoan != null) {
                // Tuliskan ID ke kolom pencarian agar daftar terfilter otomatis
                binding.etSearchHistory.setText(itemId)

                // Munculkan dialog konfirmasi pengembalian
                binding.root.post {
                    showKonfirmasiKembaliDialog(activeLoan)
                }
            } else {
                Toast.makeText(this, "Data peminjaman aktif untuk laptop ini tidak ditemukan!", Toast.LENGTH_LONG).show()
            }

            // Bersihkan extra agar tidak re-trigger saat layar diputar/di-rotate
            intent.removeExtra(EXTRA_AUTO_OPEN_RETURN)
            intent.removeExtra(EXTRA_AUTO_RETURN_ITEM_ID)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    hideKeyboardAndUnfocus()
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onResume() {
        super.onResume()
        binding.navigationView.setCheckedItem(R.id.nav_history)
    }
}