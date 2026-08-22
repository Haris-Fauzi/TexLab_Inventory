package com.example.texlabinventory.ui.detail

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.texlabinventory.data.model.Laptop
import com.example.texlabinventory.data.utils.Resource
import com.example.texlabinventory.databinding.ActivityDetailBinding
import com.example.texlabinventory.ui.AddLaptopActivity
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

        val laptop = intent.getSerializableExtra(EXTRA_LAPTOP) as? Laptop
        if (laptop != null) {
            setupUI(laptop)
            setupActionButtons(laptop)
        }
    }

    private fun setupUI(laptop: Laptop) = with(binding) {
        tvDetailId.text = laptop.inventory_id
        tvDetailBrandModel.text = "${laptop.brand} ${laptop.model}"
        tvDetailSN.text = "Serial Number: ${laptop.serial_number}"
        tvDetailLocation.text = "📍 Lokasi: ${laptop.location}"
        tvDetailCondition.text = laptop.condition

        tvDetailProcessor.text = "Processor: ${laptop.specs.processor}"
        tvDetailRam.text = "RAM: ${laptop.specs.ram}"
        tvDetailStorage.text = "Penyimpanan: ${laptop.specs.storage}"

        if (laptop.image_url.isNotEmpty()) {
            Glide.with(this@DetailActivity)
                .load(laptop.image_url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.stat_notify_error)
                .into(ivDetailLaptop)
        }
    }

    private fun setupActionButtons(laptop: Laptop) = with(binding) {
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
        // Jika sedang edit, ambil data terbaru atau refresh dari Firestore
        val laptop = intent.getSerializableExtra(EXTRA_LAPTOP) as? Laptop
        laptop?.let {
            refreshDetailData(it.inventory_id)
        }
    }

    private fun refreshDetailData(inventoryId: String) {
        // Memanggil Firestore langsung/via ViewModel untuk mendapatkan data real-time terbaru
        FirebaseFirestore.getInstance().collection("items")
            .document(inventoryId)
            .get()
            .addOnSuccessListener { document ->
                val updatedLaptop = document.toObject(Laptop::class.java)
                updatedLaptop?.let { setupUI(it) }
            }
    }
}