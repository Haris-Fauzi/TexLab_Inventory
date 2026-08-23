package com.example.texlabinventory

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.texlabinventory.data.utils.CloudinaryHelper
import com.example.texlabinventory.data.utils.Resource
import com.example.texlabinventory.databinding.ActivityMainBinding
import com.example.texlabinventory.ui.AddLaptopActivity
import com.example.texlabinventory.ui.adapter.LaptopAdapter
import com.example.texlabinventory.ui.detail.DetailActivity
import com.example.texlabinventory.ui.viewModel.LaptopViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: LaptopViewModel by viewModels()
    private lateinit var laptopAdapter: LaptopAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        CloudinaryHelper.init(this)

        setupRecyclerView()
        setupSearch()
        setupBackPressed()
        observeViewModel()

        binding.fabAddLaptop.setOnClickListener {
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
        binding.rvLaptop.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = laptopAdapter
        }
    }

    private fun setupSearch() {
        binding.etSearch.doOnTextChanged { text, _, _, _ ->
            applyCurrentFilter(text.toString())
        }
    }

    private fun applyCurrentFilter(query: String) {
        laptopAdapter.filter(query) { isEmpty ->
            if (isEmpty) {
                binding.layoutEmptyState.visibility = View.VISIBLE
                binding.rvLaptop.visibility = View.GONE
            } else {
                binding.layoutEmptyState.visibility = View.GONE
                binding.rvLaptop.visibility = View.VISIBLE
            }
        }
    }

    // Unfocus search bar & sembunyikan keyboard saat tombol Back ditekan
    // Menangani alur tombol Back secara bertahap
    private fun setupBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val hasFocus = binding.etSearch.hasFocus()
                val textNotEmpty = binding.etSearch.text?.isNotEmpty() == true

                if (hasFocus || textNotEmpty) {
                    // Sembunyikan keyboard & lepas fokus
                    hideKeyboardAndUnfocus()

                    // Bersihkan teks jika ada isinya
                    if (textNotEmpty) {
                        binding.etSearch.setText("")
                    }
                } else {
                    // Jika search bar sudah tidak fokus dan tidak ada isinya, keluar aplikasi
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun hideKeyboardAndUnfocus() {
        binding.etSearch.clearFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }

    private fun observeViewModel() {
        viewModel.laptopsState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.rvLaptop.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    if (resource.data.isEmpty()) {
                        binding.layoutEmptyState.visibility = View.VISIBLE
                        binding.rvLaptop.visibility = View.GONE
                    } else {
                        laptopAdapter.updateData(resource.data)

                        // Menerapkan ulang filter pencarian yang tersisa di etSearch
                        val currentQuery = binding.etSearch.text.toString()
                        applyCurrentFilter(currentQuery)
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Unfocus otomatis saat menyentuh layar di luar search bar
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
        viewModel.fetchLaptops()
    }
}