package com.example.texlabinventory.ui

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.texlabinventory.DashboardActivity
import com.example.texlabinventory.LoginActivity
import com.example.texlabinventory.MainActivity
import com.example.texlabinventory.R
import com.example.texlabinventory.data.model.Guru
import com.example.texlabinventory.data.model.Ruang
import com.example.texlabinventory.data.model.Siswa
import com.example.texlabinventory.data.utils.Resource
import com.example.texlabinventory.databinding.ActivityMasterDataBinding
import com.example.texlabinventory.ui.adapter.GuruAdapter
import com.example.texlabinventory.ui.adapter.RuangAdapter
import com.example.texlabinventory.ui.adapter.SiswaAdapter
import com.example.texlabinventory.ui.viewModel.GuruViewModel
import com.example.texlabinventory.ui.viewModel.RuangViewModel
import com.example.texlabinventory.ui.viewModel.SiswaViewModel
import com.google.firebase.auth.FirebaseAuth

class MasterDataActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMasterDataBinding
    private lateinit var toggle: ActionBarDrawerToggle

    private val siswaViewModel: SiswaViewModel by viewModels()
    private val guruViewModel: GuruViewModel by viewModels()
    private val ruangViewModel: RuangViewModel by viewModels()

    private val siswaAdapter = SiswaAdapter()
    private val guruAdapter = GuruAdapter()
    private val ruangAdapter = RuangAdapter()

    private var originalSiswaList = listOf<Siswa>()
    private var originalGuruList = listOf<Guru>()
    private var originalRuangList = listOf<Ruang>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMasterDataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Mencegah konten menabrak Status Bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        binding.rvMasterData.layoutManager = LinearLayoutManager(this)

        setupNavigationDrawer()
        setupChipNavigation()
        setupSearchView()
        setupBackPressed()
        observeViewModels()

        setupSiswaView()
    }

    private fun setupNavigationDrawer() {
        setSupportActionBar(binding.toolbarMaster)

        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbarMaster,
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

        binding.navigationView.setCheckedItem(R.id.nav_master_data)

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_inventaris -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                R.id.nav_master_data -> {
                    // Berada di halaman ini
                }
                R.id.nav_history -> {
                    val intent = Intent(this, HistoryPeminjamanActivity::class.java)
                    startActivity(intent)
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

    private fun setupChipNavigation() {
        binding.chipGroupMaster.setOnCheckedStateChangeListener { _, checkedIds ->
            binding.etSearchMaster.text?.clear()
            when (checkedIds.firstOrNull()) {
                R.id.chipSiswa -> setupSiswaView()
                R.id.chipRuangan -> setupRuanganView()
                R.id.chipGuru -> setupGuruView()
            }
        }
    }

    // ================= SISWA SECTION =================
    private fun setupSiswaView() {
        binding.menuSubFilter.hint = "Kelas"
        val listKelas = arrayOf(
            "Semua Kelas",
            "X RPL 1", "X RPL 2", "X RPL 3",
            "XI RPL 1", "XI RPL 2", "XI RPL 3",
            "XII RPL 1", "XII RPL 2", "XII RPL 3"
        )
        val dropdownAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, listKelas)
        binding.spinnerSubFilter.setAdapter(dropdownAdapter)
        binding.spinnerSubFilter.setText(listKelas[0], false)

        binding.rvMasterData.adapter = siswaAdapter
        siswaViewModel.fetchSiswa()

        binding.spinnerSubFilter.setOnItemClickListener { parent, _, position, _ ->
            val selectedKelas = parent.getItemAtPosition(position).toString()
            filterSiswaByKelas(selectedKelas)
        }
    }

    private fun filterSiswaByKelas(kelas: String) {
        if (kelas == "Semua Kelas") {
            siswaAdapter.setData(originalSiswaList)
        } else {
            val filtered = originalSiswaList.filter { it.kelas.equals(kelas, ignoreCase = true) }
            siswaAdapter.setData(filtered)
        }
    }

    // ================= RUANGAN SECTION =================
    private fun setupRuanganView() {
        binding.menuSubFilter.hint = "Kategori"
        val listKategori = arrayOf("Semua Ruang", "Lab", "Teori")
        val dropdownAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, listKategori)
        binding.spinnerSubFilter.setAdapter(dropdownAdapter)
        binding.spinnerSubFilter.setText(listKategori[0], false)

        binding.rvMasterData.adapter = ruangAdapter
        ruangViewModel.fetchRuang()

        binding.spinnerSubFilter.setOnItemClickListener { parent, _, position, _ ->
            val selectedKategori = parent.getItemAtPosition(position).toString()
            filterRuangByKategori(selectedKategori)
        }
    }

    private fun filterRuangByKategori(kategori: String) {
        if (kategori == "Semua Ruang") {
            ruangAdapter.setData(originalRuangList)
        } else {
            val filtered = originalRuangList.filter {
                it.nama_ruang.contains(kategori, ignoreCase = true)
            }
            ruangAdapter.setData(filtered)
        }
    }

    // ================= GURU SECTION =================
    private fun setupGuruView() {
        binding.menuSubFilter.hint = "Jabatan"
        val listFilterGuru = arrayOf("Semua Guru", "Kepala Lab", "Guru Mapel")
        val dropdownAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, listFilterGuru)
        binding.spinnerSubFilter.setAdapter(dropdownAdapter)
        binding.spinnerSubFilter.setText(listFilterGuru[0], false)

        binding.rvMasterData.adapter = guruAdapter
        guruViewModel.fetchGuru()

        binding.spinnerSubFilter.setOnItemClickListener { parent, _, position, _ ->
            val selectedFilter = parent.getItemAtPosition(position).toString()
            filterGuruByRole(selectedFilter)
        }
    }

    private fun filterGuruByRole(role: String) {
        if (role == "Semua Guru") {
            guruAdapter.setData(originalGuruList)
        } else {
            val filtered = originalGuruList.filter {
                it.nama_guru.contains(role, ignoreCase = true)
            }
            guruAdapter.setData(filtered)
        }
    }

    // ================= OBSERVER DATA =================
    private fun observeViewModels() {
        siswaViewModel.siswaState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {}
                is Resource.Success -> {
                    originalSiswaList = resource.data
                    siswaAdapter.setData(resource.data)
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        ruangViewModel.ruangState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {}
                is Resource.Success -> {
                    originalRuangList = resource.data
                    ruangAdapter.setData(resource.data)
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        guruViewModel.guruState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {}
                is Resource.Success -> {
                    originalGuruList = resource.data
                    guruAdapter.setData(resource.data)
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupSearchView() {
        binding.etSearchMaster.doOnTextChanged { text, _, _, _ ->
            val query = text.toString()
            if (binding.chipSiswa.isChecked) {
                siswaAdapter.filter(query) {}
            } else if (binding.chipGuru.isChecked) {
                guruAdapter.filter(query) {}
            } else if (binding.chipRuangan.isChecked) {
                ruangAdapter.filter(query) {}
            }
        }
    }

    private fun setupBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else if (binding.etSearchMaster.hasFocus()) {
                    hideKeyboardAndUnfocus()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun hideKeyboardAndUnfocus() {
        binding.etSearchMaster.clearFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearchMaster.windowToken, 0)
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
        binding.navigationView.setCheckedItem(R.id.nav_master_data)
    }
}