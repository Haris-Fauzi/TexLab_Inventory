package com.example.texlabinventory.ui

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
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

class MasterDataActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMasterDataBinding

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
        binding = ActivityMasterDataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvMasterData.layoutManager = LinearLayoutManager(this)

        setupChipNavigation()
        setupSearchView()
        observeViewModels()

        // Default awal: Tampilkan Siswa
        setupSiswaView()
    }

    private fun setupChipNavigation() {
        binding.chipGroupMaster.setOnCheckedStateChangeListener { _, checkedIds ->
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
        binding.menuSubFilter.hint = "Kategori Ruang"
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
        binding.menuSubFilter.hint = "Jabatan / Role"
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
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                val query = newText.orEmpty()
                if (binding.chipSiswa.isChecked) {
                    siswaAdapter.filter(query) {}
                } else if (binding.chipGuru.isChecked) {
                    guruAdapter.filter(query) {}
                } else if (binding.chipRuangan.isChecked) {
                    ruangAdapter.filter(query) {}
                }
                return true
            }
        })
    }
}