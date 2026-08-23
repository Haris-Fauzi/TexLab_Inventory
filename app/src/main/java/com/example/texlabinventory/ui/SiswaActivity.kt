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
import com.example.texlabinventory.MainActivity
import com.example.texlabinventory.LoginActivity
import com.example.texlabinventory.R
import com.example.texlabinventory.data.utils.Resource
import com.example.texlabinventory.databinding.ActivitySiswaBinding
import com.example.texlabinventory.ui.adapter.SiswaAdapter
import com.example.texlabinventory.ui.viewModel.SiswaViewModel
import com.google.firebase.auth.FirebaseAuth

class SiswaActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySiswaBinding
    private val viewModel: SiswaViewModel by viewModels()
    private lateinit var siswaAdapter: SiswaAdapter
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySiswaBinding.inflate(layoutInflater)
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

        viewModel.fetchSiswa()
    }

    private fun setupNavigationDrawer() {
        setSupportActionBar(binding.toolbarSiswa)

        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbarSiswa,
            android.R.string.ok,
            android.R.string.cancel
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navigationView.setCheckedItem(R.id.nav_siswa)

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_inventaris -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                R.id.nav_siswa -> {
                    // Sudah berada di halaman ini
                }
                R.id.nav_history -> {
                    Toast.makeText(this, "Fitur History Peminjaman", Toast.LENGTH_SHORT).show()
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
        siswaAdapter = SiswaAdapter()
        binding.rvSiswa.apply {
            layoutManager = LinearLayoutManager(this@SiswaActivity)
            adapter = siswaAdapter
        }
    }

    private fun setupSearch() {
        binding.etSearchSiswa.doOnTextChanged { text, _, _, _ ->
            applyCurrentFilter(text.toString())
        }
    }

    private fun applyCurrentFilter(query: String) {
        siswaAdapter.filter(query) { isEmpty ->
            if (isEmpty) {
                binding.layoutEmptyStateSiswa.visibility = View.VISIBLE
                binding.rvSiswa.visibility = View.GONE
            } else {
                binding.layoutEmptyStateSiswa.visibility = View.GONE
                binding.rvSiswa.visibility = View.VISIBLE
            }
        }
    }

    private fun setupBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else if (binding.etSearchSiswa.hasFocus()) {
                    hideKeyboardAndUnfocus()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun hideKeyboardAndUnfocus() {
        binding.etSearchSiswa.clearFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearchSiswa.windowToken, 0)
    }

    private fun observeViewModel() {
        viewModel.siswaState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBarSiswa.visibility = View.VISIBLE
                    binding.rvSiswa.visibility = View.GONE
                    binding.layoutEmptyStateSiswa.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressBarSiswa.visibility = View.GONE
                    if (resource.data.isEmpty()) {
                        binding.layoutEmptyStateSiswa.visibility = View.VISIBLE
                        binding.rvSiswa.visibility = View.GONE
                    } else {
                        siswaAdapter.setData(resource.data)
                        val currentQuery = binding.etSearchSiswa.text.toString()
                        applyCurrentFilter(currentQuery)
                    }
                }
                is Resource.Error -> {
                    binding.progressBarSiswa.visibility = View.GONE
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
        binding.navigationView.setCheckedItem(R.id.nav_siswa)
    }
}