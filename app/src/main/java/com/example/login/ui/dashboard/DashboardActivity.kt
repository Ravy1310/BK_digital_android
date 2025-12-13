// File: app/src/main/java/com/example/login/ui/dashboard/DashboardActivity.kt
package com.example.login.ui.dashboard

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.login.R
import com.example.login.adapter.TesAdapter
import com.example.login.ui.auth.LoginActivity
import com.example.login.viewmodel.DashboardViewModel
import com.example.login.viewmodel.KelolaTesViewModel
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class DashboardActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    // TAG untuk debugging
    private val TAG = "DashboardActivity"

    // Navigation Views
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var profileIcon: ImageView
    private lateinit var menuIcon: ImageView
    private lateinit var titleText: TextView
    private lateinit var fragmentContainer: LinearLayout

    // ViewModels
    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var kelolaTesViewModel: KelolaTesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(TAG, "DashboardActivity onCreate")

        // Initialize Navigation Views
        initializeViews()

        // Setup ViewModels
        dashboardViewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)
        kelolaTesViewModel = ViewModelProvider(this).get(KelolaTesViewModel::class.java)
        Log.d(TAG, "ViewModels initialized")

        // Setup navigation
        setupNavigation()

        // Setup back button
        setupBackPressedHandler()

        // Tampilkan dashboard sebagai default
        showDashboardContent()
    }

    private fun initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        profileIcon = findViewById(R.id.profile_icon)
        menuIcon = findViewById(R.id.menu_icon)
        titleText = findViewById(R.id.title_text)
        fragmentContainer = findViewById(R.id.fragment_container)

        titleText.text = "Dashboard"
    }

    private fun setupNavigation() {
        navigationView.setNavigationItemSelectedListener(this)
        navigationView.setCheckedItem(R.id.nav_dashboard)

        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        profileIcon.setOnClickListener { view ->
            showProfileMenu(view)
        }
    }

    private fun showProfileMenu(view: View) {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userName = sharedPref.getString("user_name", "Pengguna") ?: "Pengguna"

        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.profile_menu, popupMenu.menu)

        val userNameItem = popupMenu.menu.findItem(R.id.menu_user_name)
        userNameItem.title = "Login sebagai: $userName"

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_logout -> {
                    logoutUser()
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_dashboard -> {
                showDashboardContent()
                navigationView.setCheckedItem(R.id.nav_dashboard)
                titleText.text = "Dashboard"
            }
            R.id.nav_siswa -> {
                showSiswa()
                navigationView.setCheckedItem(R.id.nav_siswa)
                titleText.text = "Kelola Siswa"
            }
            R.id.nav_guru -> {
                showGuru()
                navigationView.setCheckedItem(R.id.nav_guru)
                titleText.text = "Kelola Guru"
            }
            R.id.nav_tes -> {
                showTes()
                navigationView.setCheckedItem(R.id.nav_tes)
                titleText.text = "Kelola Tes"
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun showDashboardContent() {
        Log.d(TAG, "showDashboardContent called")

        // Kosongkan container
        fragmentContainer.removeAllViews()

        // Tambahkan layout dashboard
        val dashboardView = layoutInflater.inflate(R.layout.activity_dashboard, null)
        fragmentContainer.addView(dashboardView)

        // Setup dashboard content dengan ViewModel
        setupDashboardContent(dashboardView)
    }

    private fun setupDashboardContent(dashboardView: View) {
        Log.d(TAG, "setupDashboardContent called")

        try {
            // Find views
            val tvJumlahSiswa = dashboardView.findViewById<TextView>(R.id.tv_jumlah_siswa)
            val tvJumlahGuru = dashboardView.findViewById<TextView>(R.id.tv_jumlah_guru)
            val tvJumlahTes = dashboardView.findViewById<TextView>(R.id.tv_jumlah_tes)
            val tvLihatSemua = dashboardView.findViewById<TextView>(R.id.tv_lihat_semua)
            val containerTesTerpopuler = dashboardView.findViewById<LinearLayout>(R.id.container_tes_terpopuler)
            val swipeRefresh = dashboardView.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefresh)
            val progressBar = dashboardView.findViewById<ProgressBar>(R.id.progressBar)
            val errorLayout = dashboardView.findViewById<LinearLayout>(R.id.errorLayout)
            val tvErrorMessage = dashboardView.findViewById<TextView>(R.id.tv_error_message)
            val btnRetry = dashboardView.findViewById<Button>(R.id.btn_retry)

            Log.d(TAG, "All views found successfully")

            // Setup listeners
            tvLihatSemua.setOnClickListener {
                Toast.makeText(this, "Membuka semua tes", Toast.LENGTH_SHORT).show()
            }

            swipeRefresh.setOnRefreshListener {
                Log.d(TAG, "Swipe refresh triggered")
                dashboardViewModel.fetchDashboardData()
            }

            btnRetry.setOnClickListener {
                Log.d(TAG, "Retry button clicked")
                dashboardViewModel.fetchDashboardData()
            }

            // Setup ViewModel observers
            setupDashboardViewModelObservers(tvJumlahSiswa, tvJumlahGuru, tvJumlahTes,
                containerTesTerpopuler, swipeRefresh,
                progressBar, errorLayout, tvErrorMessage)

            // Load initial data
            Log.d(TAG, "Calling fetchDashboardData()")
            dashboardViewModel.fetchDashboardData()

        } catch (e: Exception) {
            Log.e(TAG, "Error in setupDashboardContent: ${e.message}", e)
            Toast.makeText(this, "Error setup: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDashboardViewModelObservers(
        tvJumlahSiswa: TextView,
        tvJumlahGuru: TextView,
        tvJumlahTes: TextView,
        containerTesTerpopuler: LinearLayout,
        swipeRefresh: androidx.swiperefreshlayout.widget.SwipeRefreshLayout,
        progressBar: ProgressBar,
        errorLayout: LinearLayout,
        tvErrorMessage: TextView
    ) {
        Log.d(TAG, "Setting up ViewModel observers")

        dashboardViewModel.dashboardData.observe(this) { data ->
            Log.d(TAG, "dashboardData observer triggered, data: ${data != null}")

            swipeRefresh.isRefreshing = false
            progressBar.visibility = View.GONE
            errorLayout.visibility = View.GONE

            if (data != null) {
                Log.d(TAG, "✓ Data received:")
                Log.d(TAG, "  - Siswa: ${data.jumlahSiswa}")
                Log.d(TAG, "  - Guru: ${data.jumlahGuru}")
                Log.d(TAG, "  - Tes: ${data.jumlahTes}")
                Log.d(TAG, "  - Tes Terpopuler items: ${data.tesTerpopuler.size}")

                // Debug: tampilkan detail tes
                data.tesTerpopuler.forEachIndexed { index, tes ->
                    Log.d(TAG, "    ${index + 1}. ${tes.namaTes} - ${tes.jumlahSiswa} siswa")
                }

                // Update statistics
                tvJumlahSiswa.text = data.jumlahSiswa.toString()
                tvJumlahGuru.text = data.jumlahGuru.toString()
                tvJumlahTes.text = data.jumlahTes.toString()

                // Update tes terpopuler
                updateTesTerpopuler(containerTesTerpopuler, data.tesTerpopuler)

                // Tampilkan toast untuk konfirmasi
                Toast.makeText(this,
                    "Data loaded: ${data.tesTerpopuler.size} tes terpopuler",
                    Toast.LENGTH_SHORT
                ).show()

            } else {
                Log.w(TAG, "⚠ Data is null!")
                Toast.makeText(this, "Data kosong", Toast.LENGTH_SHORT).show()
            }
        }

        dashboardViewModel.isLoading.observe(this) { isLoading ->
            Log.d(TAG, "isLoading observer: $isLoading")

            if (isLoading && !swipeRefresh.isRefreshing) {
                progressBar.visibility = View.VISIBLE
                errorLayout.visibility = View.GONE
            }
        }

        dashboardViewModel.errorMessage.observe(this) { errorMessage ->
            Log.d(TAG, "errorMessage observer: $errorMessage")

            swipeRefresh.isRefreshing = false
            if (errorMessage != null) {
                progressBar.visibility = View.GONE
                errorLayout.visibility = View.VISIBLE
                tvErrorMessage.text = errorMessage

                Log.e(TAG, "Error loading data: $errorMessage")
            }
        }
    }

    /**
     * Fungsi untuk menampilkan tes terpopuler di container
     */
    private fun updateTesTerpopuler(
        container: LinearLayout,
        tesList: List<com.example.login.models.TesTerpopuler>
    ) {
        Log.d(TAG, "updateTesTerpopuler called with ${tesList.size} items")

        // Hapus semua view yang ada
        container.removeAllViews()

        if (tesList.isEmpty()) {
            Log.d(TAG, "Tes list is empty, showing placeholder")
            val textView = TextView(this).apply {
                text = "Belum ada tes yang dikerjakan"
                setTextColor(resources.getColor(android.R.color.darker_gray, theme))
                textSize = 14f
                setPadding(0, dpToPx(32), 0, 0)
                gravity = Gravity.CENTER
            }
            container.addView(textView)
            return
        }

        Log.d(TAG, "Creating ${tesList.size} tes cards")

        // Tampilkan setiap tes
        tesList.forEachIndexed { index, tes ->
            Log.d(TAG, "Creating card for: ${tes.namaTes} (${tes.jumlahSiswa} siswa)")

            try {
                // Inflate layout item tes
                val cardView = layoutInflater.inflate(
                    R.layout.item_tes_terpopuler,
                    container,
                    false
                ) as androidx.cardview.widget.CardView

                // Find views
                val tvNamaTes = cardView.findViewById<TextView>(R.id.tv_nama_tes)
                val tvJumlahSiswa = cardView.findViewById<TextView>(R.id.tv_jumlah_siswa)

                // Set data
                tvNamaTes.text = tes.namaTes
                tvJumlahSiswa.text = "Dikerjakan oleh ${tes.jumlahSiswa} siswa"

                // Tambahkan ke container
                container.addView(cardView)

                // Tambahkan margin bottom (sesuai layout: 10dp)
                val layoutParams = cardView.layoutParams as ViewGroup.MarginLayoutParams
                layoutParams.bottomMargin = dpToPx(10)
                cardView.layoutParams = layoutParams

                // Tambahkan click listener
                cardView.setOnClickListener {
                    Toast.makeText(this, "Membuka: ${tes.namaTes}", Toast.LENGTH_SHORT).show()
                    // Bisa ditambahkan intent ke detail tes nanti
                }

                Log.d(TAG, "✓ Card created successfully for: ${tes.namaTes}")

            } catch (e: Exception) {
                Log.e(TAG, "Error creating card for ${tes.namaTes}: ${e.message}")
            }
        }

        Log.d(TAG, "✓ Successfully added ${tesList.size} tes items to UI")
    }

    /**
     * Fungsi untuk menampilkan halaman Kelola Tes dengan data dari API
     */
    private fun showTes() {
        Log.d(TAG, "showTes called")

        fragmentContainer.removeAllViews()
        val tesView = layoutInflater.inflate(R.layout.kelolasoaltes, null)
        fragmentContainer.addView(tesView)

        // Setup ViewModel dan load data dari API
        setupKelolaTesContent(tesView)
    }

    private fun setupKelolaTesContent(tesView: View) {
        Log.d(TAG, "setupKelolaTesContent called")

        try {
            // Temukan semua views dari XML
            val tvTotalSoal = tesView.findViewById<TextView>(R.id.tvTotalSoal)
            val tvJenisTes = tesView.findViewById<TextView>(R.id.tvJenisTes)
            val kelolaTesBKLayout = tesView.findViewById<LinearLayout>(R.id.kelolaTesBKLayout)
            val tambahTesBaruLayout = tesView.findViewById<LinearLayout>(R.id.tambahTesBaruLayout)
            val rvDaftarTes = tesView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvDaftarTes)
            val progressBarTes = tesView.findViewById<ProgressBar>(R.id.progressBarTes)
            val tvErrorTes = tesView.findViewById<TextView>(R.id.tvErrorTes)

            // Setup RecyclerView
            rvDaftarTes.layoutManager = LinearLayoutManager(this)
            rvDaftarTes.setHasFixedSize(true)

            // Setup click listeners untuk tombol aksi
            kelolaTesBKLayout.setOnClickListener {
                Toast.makeText(this, "Membuka Kelola Soal Tes", Toast.LENGTH_SHORT).show()
            }

            tambahTesBaruLayout.setOnClickListener {
                Toast.makeText(this, "Menambah Tes Baru", Toast.LENGTH_SHORT).show()
            }

            // Setup observers untuk ViewModel
            kelolaTesViewModel.kelolaTesData.observe(this) { response ->
                progressBarTes.visibility = View.GONE

                if (response != null && response.success && response.data != null) {
                    val data = response.data

                    Log.d(TAG, "✓ Data tes berhasil dimuat:")
                    Log.d(TAG, "  - Total Soal: ${data.totalSoal}")
                    Log.d(TAG, "  - Jenis Tes: ${data.jenisTes}")
                    Log.d(TAG, "  - Jumlah Tes: ${data.daftarTes.size}")

                    // Update statistik
                    tvTotalSoal.text = data.totalSoal.toString()
                    tvJenisTes.text = data.jenisTes.toString()

                    // Update daftar tes di RecyclerView
                    if (data.daftarTes.isNotEmpty()) {
                        val adapter = TesAdapter(data.daftarTes) { tes ->
                            val statusText = if (tes.status == "aktif") "Aktif" else "Nonaktif"
                            Toast.makeText(
                                this,
                                "${tes.namaTes}\n${tes.jumlahSoal} soal - Status: $statusText",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        rvDaftarTes.adapter = adapter
                        tvErrorTes.visibility = View.GONE
                    } else {
                        tvErrorTes.text = "Belum ada tes tersedia"
                        tvErrorTes.visibility = View.VISIBLE
                    }

                    Toast.makeText(this, "Data tes berhasil dimuat", Toast.LENGTH_SHORT).show()
                } else if (response != null && !response.success) {
                    Log.e(TAG, "API returned error: ${response.message}")
                    tvErrorTes.text = "Error: ${response.message}"
                    tvErrorTes.visibility = View.VISIBLE

                    // Tampilkan data default jika error
                    tvTotalSoal.text = "0"
                    tvJenisTes.text = "0"
                }
            }

            kelolaTesViewModel.isLoading.observe(this) { isLoading ->
                Log.d(TAG, "KelolaTes isLoading: $isLoading")
                progressBarTes.visibility = if (isLoading) View.VISIBLE else View.GONE
            }

            kelolaTesViewModel.errorMessage.observe(this) { errorMessage ->
                progressBarTes.visibility = View.GONE

                if (errorMessage != null) {
                    Log.e(TAG, "KelolaTes error: $errorMessage")
                    tvErrorTes.text = "Error: $errorMessage"
                    tvErrorTes.visibility = View.VISIBLE

                    // Tampilkan data default jika error
                    tvTotalSoal.text = "0"
                    tvJenisTes.text = "0"
                }
            }

            // Tampilkan loading saat awal
            progressBarTes.visibility = View.VISIBLE

            // Load data dari API
            Log.d(TAG, "Fetching kelola tes data...")
            kelolaTesViewModel.fetchKelolaTesData()

        } catch (e: Exception) {
            Log.e(TAG, "Error in setupKelolaTesContent: ${e.message}", e)
            Toast.makeText(this, "Error setup: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSiswa() {
        fragmentContainer.removeAllViews()
        val textView = TextView(this)
        textView.text = "Halaman Kelola Siswa\n\nFitur akan segera tersedia"
        textView.textSize = 18f
        textView.gravity = android.view.Gravity.CENTER
        fragmentContainer.addView(textView)
        Toast.makeText(this, "Kelola Data Siswa", Toast.LENGTH_SHORT).show()
    }

    private fun showGuru() {
        fragmentContainer.removeAllViews()
        val textView = TextView(this)
        textView.text = "Halaman Kelola Guru\n\nFitur akan segera tersedia"
        textView.textSize = 18f
        textView.gravity = android.view.Gravity.CENTER
        fragmentContainer.addView(textView)
        Toast.makeText(this, "Kelola Data Guru", Toast.LENGTH_SHORT).show()
    }

    private fun setupBackPressedHandler() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    if (System.currentTimeMillis() - backPressedTime < 2000) {
                        finish()
                    } else {
                        Toast.makeText(this@DashboardActivity, "Tekan kembali sekali lagi untuk keluar", Toast.LENGTH_SHORT).show()
                        backPressedTime = System.currentTimeMillis()
                    }
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    private var backPressedTime: Long = 0

    /**
     * Konversi dp ke px
     */
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun logoutUser() {
        try {
            Firebase.auth.signOut()

            val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
            sharedPref.edit().clear().apply()

            Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Error saat logout: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}