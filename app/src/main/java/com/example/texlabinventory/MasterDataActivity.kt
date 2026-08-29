package com.example.texlab_inventory

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.texlab_inventory.databinding.ActivityMasterDataBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MasterDataActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMasterDataBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMasterDataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvMasterData.layoutManager = LinearLayoutManager(this)

        // Single Listener untuk Chip Navigation
        setupChipNavigation()

        // Inisialisasi Tampilan Default (Siswa)
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
        val listKelas = arrayOf(
            "Semua Kelas",
            "X RPL 1", "X RPL 2", "X RPL 3",
            "XI RPL 1", "XI RPL 2", "XI RPL 3",
            "XII RPL 1", "XII RPL 2", "XII RPL 3"
        )
        setDropdownAdapter(listKelas)

        // Load Default Data
        loadDataSiswa("Semua Kelas")

        // Dropdown Click Listener
        binding.spinnerSubFilter.setOnItemClickListener { parent, _, position, _ ->
            val selectedKelas = parent.getItemAtPosition(position).toString()
            loadDataSiswa(selectedKelas)
        }

        // FAB Action
        binding.fabAdd.setOnClickListener {
            Toast.makeText(this, "Tambah Data Siswa", Toast.LENGTH_SHORT).show()
            // TODO: Launch Intent ke AddSiswaActivity
        }
    }

    private fun loadDataSiswa(filterKelas: String) {
        var query: Query = db.collection("siswa")
        if (filterKelas != "Semua Kelas") {
            query = query.whereEqualTo("kelas", filterKelas)
        }

        query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Toast.makeText(this, "Gagal memuat data siswa", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            val listSiswa = snapshot?.toObjects(Siswa::class.java) ?: emptyList()
            binding.rvMasterData.adapter = SiswaAdapter(listSiswa)
        }
    }

    // ================= RUANGAN SECTION =================
    private fun setupRuanganView() {
        val listRuangan = arrayOf("Semua", "Lab", "Teori")
        setDropdownAdapter(listRuangan)

        // Load Default Data
        loadDataRuangan("Semua")

        // Dropdown Click Listener
        binding.spinnerSubFilter.setOnItemClickListener { parent, _, position, _ ->
            val selectedTipe = parent.getItemAtPosition(position).toString()
            loadDataRuangan(selectedTipe)
        }

        // FAB Action
        binding.fabAdd.setOnClickListener {
            Toast.makeText(this, "Tambah Data Ruangan", Toast.LENGTH_SHORT).show()
            // TODO: Launch Intent ke AddRuanganActivity
        }
    }

    private fun loadDataRuangan(filterTipe: String) {
        var query: Query = db.collection("ruangan")
        if (filterTipe != "Semua") {
            query = query.whereEqualTo("tipe", filterTipe)
        }

        query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Toast.makeText(this, "Gagal memuat data ruangan", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            val listRuangan = snapshot?.toObjects(Ruangan::class.java) ?: emptyList()
            binding.rvMasterData.adapter = RuanganAdapter(listRuangan)
        }
    }

    // ================= GURU SECTION =================
    private fun setupGuruView() {
        val listPeran = arrayOf("Semua", "Kepala Lab", "Guru Mapel")
        setDropdownAdapter(listPeran)

        // Load Default Data
        loadDataGuru("Semua")

        // Dropdown Click Listener
        binding.spinnerSubFilter.setOnItemClickListener { parent, _, position, _ ->
            val selectedPeran = parent.getItemAtPosition(position).toString()
            loadDataGuru(selectedPeran)
        }

        // FAB Action
        binding.fabAdd.setOnClickListener {
            Toast.makeText(this, "Tambah Data Guru", Toast.LENGTH_SHORT).show()
            // TODO: Launch Intent ke AddGuruActivity
        }
    }

    private fun loadDataGuru(filterPeran: String) {
        var query: Query = db.collection("guru")
        if (filterPeran != "Semua") {
            query = query.whereEqualTo("peran", filterPeran)
        }

        query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Toast.makeText(this, "Gagal memuat data guru", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            val listGuru = snapshot?.toObjects(Guru::class.java) ?: emptyList()
            binding.rvMasterData.adapter = GuruAdapter(listGuru)
        }
    }

    // Helper reusable function untuk mengisi pilihan dropdown
    private fun setDropdownAdapter(items: Array<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items)
        binding.spinnerSubFilter.setAdapter(adapter)
        binding.spinnerSubFilter.setText(items[0], false)
    }
}