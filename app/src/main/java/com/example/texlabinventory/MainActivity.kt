package com.example.texlabinventory

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.texlabinventory.data.utils.CloudinaryHelper
import com.example.texlabinventory.data.utils.Resource
import com.example.texlabinventory.ui.adapter.LaptopAdapter
import com.example.texlabinventory.ui.viewModel.LaptopViewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: LaptopViewModel by viewModels()
    private lateinit var laptopAdapter: LaptopAdapter

    private lateinit var rvLaptop: RecyclerView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Inisialisasi Cloudinary saat aplikasi dibuka
        CloudinaryHelper.init(this)

        // Init UI Components dari XML
        rvLaptop = findViewById(R.id.rvLaptop)
        progressBar = findViewById(R.id.progressBar)

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        laptopAdapter = LaptopAdapter()
        rvLaptop.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = laptopAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.laptopsState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    rvLaptop.visibility = View.GONE
                }
                is Resource.Success -> {
                    progressBar.visibility = View.GONE
                    rvLaptop.visibility = View.VISIBLE
                    laptopAdapter.updateData(resource.data)
                }
                is Resource.Error -> {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}