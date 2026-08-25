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
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.texlabinventory.data.utils.CloudinaryHelper
import com.example.texlabinventory.data.utils.Resource
import com.example.texlabinventory.databinding.ActivityMainBinding
import com.example.texlabinventory.ui.AddLaptopActivity
import com.example.texlabinventory.ui.HistoryPeminjamanActivity // Import HistoryPeminjamanActivity
import com.example.texlabinventory.ui.SiswaActivity
import com.example.texlabinventory.ui.adapter.LaptopAdapter
import com.example.texlabinventory.ui.detail.DetailActivity
import com.example.texlabinventory.ui.viewModel.LaptopViewModel
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: LaptopViewModel by viewModels()
    private lateinit var laptopAdapter: LaptopAdapter
    private lateinit var toggle: ActionBarDrawerToggle

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

        setupNavigationDrawer()
        setupRecyclerView()
        setupSearch()
        setupBackPressed()
        observeViewModel()

        binding.fabAddLaptop.setOnClickListener {
            val intent = Intent(this, AddLaptopActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupNavigationDrawer() {
        setSupportActionBar(binding.toolbar)

        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            android.R.string.ok,
            android.R.string.cancel
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // 1. Matikan tint warna agar icon PNG tampil warna aslinya
        binding.navigationView.itemIconTintList = null

        // 2. Set listener klik pada Header Navigation (Logo / Teks) untuk kembali ke Dashboard
        val headerView = binding.navigationView.getHeaderView(0)

        // Menggunakan ID ImageView/TextView yang ada di nav_header.xml
        val ivLogoHeader = headerView.findViewById<View>(R.id.ivNavLogo) // Ganti id sesuai xml kamu
        ivLogoHeader?.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        binding.navigationView.setCheckedItem(R.id.nav_inventaris)

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_inventaris -> {
                    // Sudah berada di halaman ini
                }
                R.id.nav_siswa -> {
                    val intent = Intent(this, SiswaActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_history -> {
                    // Pindah ke HistoryPeminjamanActivity
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

    private fun setupBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else if (binding.etSearch.hasFocus()) {
                    hideKeyboardAndUnfocus()
                } else {
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
        binding.navigationView.setCheckedItem(R.id.nav_inventaris)
        viewModel.fetchLaptops()
    }
}