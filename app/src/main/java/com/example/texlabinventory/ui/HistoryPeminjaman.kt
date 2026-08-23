package com.example.texlabinventory.ui

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
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.texlabinventory.LoginActivity
import com.example.texlabinventory.MainActivity
import com.example.texlabinventory.R
import com.example.texlabinventory.data.model.Peminjaman // Import model Peminjaman
import com.example.texlabinventory.data.utils.Resource
import com.example.texlabinventory.databinding.ActivityHistoryPeminjamanBinding
import com.example.texlabinventory.ui.adapter.HistoryPeminjamanAdapter
import com.example.texlabinventory.ui.viewModel.HistoryPeminjamanViewModel
import com.google.firebase.auth.FirebaseAuth

class HistoryPeminjamanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryPeminjamanBinding
    private val viewModel: HistoryPeminjamanViewModel by viewModels()
    private lateinit var historyAdapter: HistoryPeminjamanAdapter
    private lateinit var toggle: ActionBarDrawerToggle
    private var fullList: List<Peminjaman> = emptyList()

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

        binding.navigationView.setCheckedItem(R.id.nav_history)

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_inventaris -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                R.id.nav_siswa -> {
                    val intent = Intent(this, SiswaActivity::class.java)
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
        historyAdapter = HistoryPeminjamanAdapter()
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(this@HistoryPeminjamanActivity)
            adapter = historyAdapter
        }
    }

    private fun setupSearch() {
        binding.etSearchHistory.doOnTextChanged { text, _, _, _ ->
            applyCurrentFilter(text.toString())
        }
    }

    private fun applyCurrentFilter(query: String) {
        val filteredList = if (query.trim().isEmpty()) {
            fullList
        } else {
            fullList.filter { item ->
                (item.namaItem?.contains(query, ignoreCase = true) == true) ||
                        (item.namaSiswa?.contains(query, ignoreCase = true) == true) ||
                        (item.siswaId?.contains(query, ignoreCase = true) == true) ||
                        (item.itemId?.contains(query, ignoreCase = true) == true) ||
                        (item.ruangan?.contains(query, ignoreCase = true) == true) ||
                        (item.kelasSiswa?.contains(query, ignoreCase = true) == true)
            }
        }

        // ListAdapter menggunakan submitList
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
                }
                is Resource.Error -> {
                    binding.progressBarHistory.visibility = View.GONE
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
        binding.navigationView.setCheckedItem(R.id.nav_history)
    }
}