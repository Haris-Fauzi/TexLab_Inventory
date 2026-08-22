package com.example.texlabinventory

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.texlabinventory.data.utils.CloudinaryHelper
import com.example.texlabinventory.data.utils.Resource
import com.example.texlabinventory.ui.AddLaptopActivity
import com.example.texlabinventory.ui.adapter.LaptopAdapter
import com.example.texlabinventory.ui.detail.DetailActivity
import com.example.texlabinventory.ui.viewModel.LaptopViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private val viewModel: LaptopViewModel by viewModels()
    private lateinit var laptopAdapter: LaptopAdapter

    private lateinit var rvLaptop: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var etSearch: TextInputEditText
    private lateinit var layoutEmptyState: View

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Penanganan Safe Area Insets agar UI tidak tertutup status bar / nav bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainRoot)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        CloudinaryHelper.init(this)

        rvLaptop = findViewById(R.id.rvLaptop)
        progressBar = findViewById(R.id.progressBar)
        etSearch = findViewById(R.id.etSearch)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)

        setupRecyclerView()
        setupSearch()
        observeViewModel()

        findViewById<FloatingActionButton>(R.id.fabAddLaptop).setOnClickListener {
            val intent = Intent(this, AddLaptopActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        laptopAdapter = LaptopAdapter(
            onItemClick = { laptop ->
                val intent = Intent(this, DetailActivity::class.java).apply {
                    putExtra(DetailActivity.EXTRA_LAPTOP, laptop)
                }
                startActivity(intent)
            }
        )
        rvLaptop.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = laptopAdapter
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                laptopAdapter.filter(query) { isEmpty ->
                    if (isEmpty) {
                        layoutEmptyState.visibility = View.VISIBLE
                        rvLaptop.visibility = View.GONE
                    } else {
                        layoutEmptyState.visibility = View.GONE
                        rvLaptop.visibility = View.VISIBLE
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeViewModel() {
        viewModel.laptopsState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    rvLaptop.visibility = View.GONE
                    layoutEmptyState.visibility = View.GONE
                }
                is Resource.Success -> {
                    progressBar.visibility = View.GONE
                    if (resource.data.isEmpty()) {
                        layoutEmptyState.visibility = View.VISIBLE
                        rvLaptop.visibility = View.GONE
                    } else {
                        layoutEmptyState.visibility = View.GONE
                        rvLaptop.visibility = View.VISIBLE
                        laptopAdapter.updateData(resource.data)
                    }
                }
                is Resource.Error -> {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchLaptops()
    }
}