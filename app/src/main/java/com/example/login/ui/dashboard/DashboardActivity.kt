// File: app/src/main/java/com/example/login/ui/dashboard/DashboardActivity.kt
package com.example.login.ui.dashboard

import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.text.format.DateFormat
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.login.R
import com.example.login.adapter.KelolaSoalAdapter
import com.example.login.adapter.SoalTesAdapter
import com.example.login.adapter.TesAdapter
import com.example.login.api.ApiClient
import com.example.login.models.*
import com.example.login.ui.auth.LoginActivity
import com.example.login.viewmodel.DashboardViewModel
import com.example.login.viewmodel.KelolaTesViewModel
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    // TAG untuk debugging
    private val TAG = "DashboardActivity"

    // Navigation Views
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var profileIcon: ImageView
    private lateinit var menuIcon: ImageView
    private lateinit var titleText: TextView
    private lateinit var fragmentContainer: FrameLayout

    // ViewModels
    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var kelolaTesViewModel: KelolaTesViewModel

    // Variables untuk file CSV
    private var selectedFileUri: Uri? = null
    private var selectedFileName: String? = null

    // Variables untuk Kelola Siswa
    private lateinit var tvTotalSiswa: TextView
    private lateinit var tvMaleCount: TextView
    private lateinit var tvFemaleCount: TextView
    private lateinit var tableRowsContainer: LinearLayout
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var progressBarSiswa: ProgressBar
    private lateinit var btnAddStudentFloating: Button

    // Data class untuk Siswa
    data class Siswa(
        val id: String,
        val name: String,
        val kelas: String,
        val gender: String,
        val status: String = "aktif"
    )

    // Sample data untuk siswa
    private val siswaList = mutableListOf<Siswa>()
    private var siswaIdCounter = 1

    // Request codes
    companion object {
        private const val REQUEST_CODE_PICK_CSV = 1001
    }

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

    // ==================== FUNGSI KELOLA SISWA ====================
    private fun showSiswa() {
        Log.d(TAG, "showSiswa called")

        // Debug: cek jumlah child sebelum clear
        Log.d(TAG, "Siswa - Child count before: ${fragmentContainer.childCount}")

        // Kosongkan container
        fragmentContainer.removeAllViews()

        // Debug: cek jumlah child setelah clear
        Log.d(TAG, "Siswa - Child count after: ${fragmentContainer.childCount}")

        // Tambahkan layout kelola siswa
        val siswaView = layoutInflater.inflate(R.layout.activity_kelola_siswa, null)
        fragmentContainer.addView(siswaView)

        // Debug: cek ukuran view
        siswaView.post {
            Log.d(TAG, "Siswa view width: ${siswaView.width}, height: ${siswaView.height}")
            Log.d(TAG, "Fragment container width: ${fragmentContainer.width}, height: ${fragmentContainer.height}")
        }

        // Setup konten kelola siswa
        setupKelolaSiswaContent(siswaView)
    }

    private fun setupKelolaSiswaContent(siswaView: View) {
        Log.d(TAG, "setupKelolaSiswaContent called")

        try {
            // Temukan semua views
            tvTotalSiswa = siswaView.findViewById(R.id.tvTotalSiswa)
            tvMaleCount = siswaView.findViewById(R.id.tvMaleCount)
            tvFemaleCount = siswaView.findViewById(R.id.tvFemaleCount)
            tableRowsContainer = siswaView.findViewById(R.id.tableRowsContainer)
            emptyStateLayout = siswaView.findViewById(R.id.emptyStateLayout)
            progressBarSiswa = siswaView.findViewById(R.id.progressBar)
            btnAddStudentFloating = siswaView.findViewById(R.id.btnAddStudentFloating)

            // Setup click listeners
            btnAddStudentFloating.setOnClickListener {
                addSampleStudent()
            }

            // Load initial data
            loadInitialDataSiswa()

        } catch (e: Exception) {
            Log.e(TAG, "Error in setupKelolaSiswaContent: ${e.message}", e)
            Toast.makeText(this, "Error setup kelola siswa: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadInitialDataSiswa() {
        showLoadingSiswa()

        // Simulate API call delay
        Handler(Looper.getMainLooper()).postDelayed({
            // Add sample data
            siswaList.clear()
            siswaList.addAll(getSampleDataSiswa())
            siswaIdCounter = siswaList.size + 1

            updateUISiswa()
            hideLoadingSiswa()
        }, 1000)
    }

    private fun getSampleDataSiswa(): List<Siswa> {
        return listOf(
            Siswa("001", "Ahmad Budiman", "XII IPA 1", "Laki-laki", "aktif"),
            Siswa("002", "Siti Aisyah", "XI IPS 2", "Perempuan", "aktif"),
            Siswa("003", "Rizki Pratama", "X IPA 3", "Laki-laki", "aktif"),
            Siswa("004", "Dewi Anggraini", "XII IPA 2", "Perempuan", "aktif"),
            Siswa("005", "Bambang Sugiarto", "XI IPA 1", "Laki-laki", "aktif"),
            Siswa("006", "Maya Sari", "X IPS 1", "Perempuan", "aktif"),
            Siswa("007", "Fajar Hidayat", "XII IPS 1", "Laki-laki", "nonaktif"),
            Siswa("008", "Rina Melati", "XI IPA 3", "Perempuan", "aktif")
        )
    }

    private fun addSampleStudent() {
        val newId = siswaIdCounter.toString().padStart(3, '0')
        val gender = if (siswaIdCounter % 2 == 0) "Perempuan" else "Laki-laki"
        val kelas = when ((siswaIdCounter - 1) % 6) {
            0 -> "X IPA 1"
            1 -> "X IPA 2"
            2 -> "XI IPA 1"
            3 -> "XI IPA 2"
            4 -> "XII IPA 1"
            else -> "XII IPA 2"
        }

        val newSiswa = Siswa(
            id = newId,
            name = "Siswa Baru $newId",
            kelas = kelas,
            gender = gender,
            status = "aktif"
        )

        siswaList.add(0, newSiswa) // Add to top
        siswaIdCounter++

        // Update UI
        updateUISiswa()

        // Show success message
        Toast.makeText(
            this,
            "Siswa $newId berhasil ditambahkan",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun updateUISiswa() {
        // Update statistics
        updateStatisticsSiswa()

        // Update table
        updateTableSiswa()
    }

    private fun updateStatisticsSiswa() {
        val total = siswaList.size
        val male = siswaList.count { it.gender == "Laki-laki" }
        val female = siswaList.count { it.gender == "Perempuan" }

        tvTotalSiswa.text = total.toString()
        tvMaleCount.text = male.toString()
        tvFemaleCount.text = female.toString()
    }

    private fun updateTableSiswa() {
        // Clear existing rows
        tableRowsContainer.removeAllViews()

        if (siswaList.isEmpty()) {
            emptyStateLayout.visibility = View.VISIBLE
            return
        }

        emptyStateLayout.visibility = View.GONE

        // Add student rows
        siswaList.forEachIndexed { index, siswa ->
            addStudentRow(siswa, index + 1)
        }
    }

    private fun addStudentRow(siswa: Siswa, position: Int) {
        // Inflate row layout
        val rowView = layoutInflater.inflate(R.layout.table_row_empty, tableRowsContainer, false)

        // Set data
        rowView.findViewById<TextView>(R.id.tvId).text = siswa.id
        rowView.findViewById<TextView>(R.id.tvNama).text = siswa.name
        rowView.findViewById<TextView>(R.id.tvKelas).text = siswa.kelas
        rowView.findViewById<TextView>(R.id.tvGender).text = siswa.gender

        // Set alternate background color
        if (position % 2 == 0) {
            rowView.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
        } else {
            rowView.setBackgroundColor(ContextCompat.getColor(this, R.color.table_row_alternate))
        }

        // Add click listener for row
        rowView.setOnClickListener {
            showSiswaDetail(siswa)
        }

        // Add to container
        tableRowsContainer.addView(rowView)
    }

    private fun showSiswaDetail(siswa: Siswa) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Detail Siswa")
            .setMessage(
                "ID: ${siswa.id}\n" +
                        "Nama: ${siswa.name}\n" +
                        "Kelas: ${siswa.kelas}\n" +
                        "Jenis Kelamin: ${siswa.gender}\n" +
                        "Status: ${if (siswa.status == "aktif") "Aktif" else "Nonaktif"}"
            )
            .setPositiveButton("OK", null)
            .setNeutralButton("Edit") { _, _ ->
                editSiswa(siswa)
            }
            .setNegativeButton("Hapus") { _, _ ->
                deleteSiswa(siswa)
            }
            .show()
    }

    private fun editSiswa(siswa: Siswa) {
        Toast.makeText(
            this,
            "Edit siswa: ${siswa.name}",
            Toast.LENGTH_SHORT
        ).show()
        // Implement edit functionality here
    }

    private fun deleteSiswa(siswa: Siswa) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Konfirmasi Hapus")
            .setMessage("Apakah Anda yakin ingin menghapus siswa ${siswa.name}?")
            .setPositiveButton("Hapus") { _, _ ->
                siswaList.remove(siswa)
                updateUISiswa()
                Toast.makeText(
                    this,
                    "Siswa ${siswa.name} berhasil dihapus",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showLoadingSiswa() {
        progressBarSiswa.visibility = View.VISIBLE
        tableRowsContainer.visibility = View.GONE
    }

    private fun hideLoadingSiswa() {
        progressBarSiswa.visibility = View.GONE
        tableRowsContainer.visibility = View.VISIBLE
    }

    // ==================== FUNGSI DASHBOARD ====================
    private fun showDashboardContent() {
        Log.d(TAG, "showDashboardContent called")

        // Debug: cek jumlah child sebelum clear
        Log.d(TAG, "Dashboard - Child count before: ${fragmentContainer.childCount}")

        // Kosongkan container
        fragmentContainer.removeAllViews()

        // Debug: cek jumlah child setelah clear
        Log.d(TAG, "Dashboard - Child count after: ${fragmentContainer.childCount}")

        // Tambahkan layout dashboard
        val dashboardView = layoutInflater.inflate(R.layout.activity_dashboard, null)
        fragmentContainer.addView(dashboardView)

        // Debug: cek ukuran view
        dashboardView.post {
            Log.d(TAG, "Dashboard view width: ${dashboardView.width}, height: ${dashboardView.height}")
            Log.d(TAG, "Fragment container width: ${fragmentContainer.width}, height: ${fragmentContainer.height}")
        }

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
                setTextColor(ContextCompat.getColor(this@DashboardActivity, android.R.color.darker_gray))
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
                }

                Log.d(TAG, "✓ Card created successfully for: ${tes.namaTes}")

            } catch (e: Exception) {
                Log.e(TAG, "Error creating card for ${tes.namaTes}: ${e.message}")
            }
        }

        Log.d(TAG, "✓ Successfully added ${tesList.size} tes items to UI")
    }

    // ==================== FUNGSI KELOLA TES ====================
    /**
     * Fungsi untuk menampilkan halaman Kelola Tes dengan data dari API
     */
    private fun showTes() {
        Log.d(TAG, "showTes called")

        // Debug: cek jumlah child sebelum clear
        Log.d(TAG, "Tes - Child count before: ${fragmentContainer.childCount}")

        fragmentContainer.removeAllViews()

        // Debug: cek jumlah child setelah clear
        Log.d(TAG, "Tes - Child count after: ${fragmentContainer.childCount}")

        val tesView = layoutInflater.inflate(R.layout.kelolasoaltes, null)
        fragmentContainer.addView(tesView)

        // Debug: cek ukuran view
        tesView.post {
            Log.d(TAG, "Tes view width: ${tesView.width}, height: ${tesView.height}")
            Log.d(TAG, "Fragment container width: ${fragmentContainer.width}, height: ${fragmentContainer.height}")
        }

        // Reset selected file
        selectedFileUri = null
        selectedFileName = null

        // Setup ViewModel dan load data dari API
        setupKelolaTesContent(tesView)
    }

    /**
     * Fungsi untuk menampilkan form tambah tes baru
     */
    private fun showTambahTesForm() {
        Log.d(TAG, "showTambahTesForm called")

        // Debug: cek jumlah child sebelum clear
        Log.d(TAG, "Form - Child count before: ${fragmentContainer.childCount}")

        fragmentContainer.removeAllViews()

        // Debug: cek jumlah child setelah clear
        Log.d(TAG, "Form - Child count after: ${fragmentContainer.childCount}")

        val tambahTesView = layoutInflater.inflate(R.layout.formtambahtes, null)
        fragmentContainer.addView(tambahTesView)

        // Debug: cek ukuran view
        tambahTesView.post {
            Log.d(TAG, "Form view width: ${tambahTesView.width}, height: ${tambahTesView.height}")
            Log.d(TAG, "Fragment container width: ${fragmentContainer.width}, height: ${fragmentContainer.height}")
        }

        // Tetapkan menu navigasi tes sebagai aktif
        navigationView.setCheckedItem(R.id.nav_tes)

        // Update judul
        titleText.text = "Tambah Tes Baru"

        // Setup form tambah tes
        setupTambahTesForm(tambahTesView)
    }

    /**
     * Fungsi untuk menampilkan form kelola soal tes
     */
    private fun showKelolaSoalTes() {
        Log.d(TAG, "showKelolaSoalTes called")

        fragmentContainer.removeAllViews()

        // Debug: cek jumlah child setelah clear
        Log.d(TAG, "KelolaSoal - Child count after: ${fragmentContainer.childCount}")

        val kelolaSoalView = layoutInflater.inflate(R.layout.formkelolasoal, null)
        fragmentContainer.addView(kelolaSoalView)

        // Debug: cek ukuran view
        kelolaSoalView.post {
            Log.d(TAG, "KelolaSoal view width: ${kelolaSoalView.width}, height: ${kelolaSoalView.height}")
            Log.d(TAG, "Fragment container width: ${fragmentContainer.width}, height: ${fragmentContainer.height}")
        }

        // Tetapkan menu navigasi tes sebagai aktif
        navigationView.setCheckedItem(R.id.nav_tes)

        // Update judul
        titleText.text = "Kelola Tes BK"

        // Setup form kelola soal
        setupKelolaSoalTes(kelolaSoalView)
    }

    /**
     * Setup form kelola soal tes dengan data dari API
     */
    private fun setupKelolaSoalTes(kelolaSoalView: View) {
        Log.d(TAG, "setupKelolaSoalTes called")

        try {
            // Temukan views
            val tesRecyclerView = kelolaSoalView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.tesRecyclerView)
            val emptyStateLayout = kelolaSoalView.findViewById<LinearLayout>(R.id.emptyStateLayout)
            val loadingProgress = kelolaSoalView.findViewById<ProgressBar>(R.id.loadingProgress)
            val backButton = kelolaSoalView.findViewById<com.google.android.material.button.MaterialButton>(R.id.backButton)
            val titleTextView = kelolaSoalView.findViewById<TextView>(R.id.titleText)
            val subtitleTextView = kelolaSoalView.findViewById<TextView>(R.id.subtitleText)

            // Setup RecyclerView
            tesRecyclerView.layoutManager = LinearLayoutManager(this)
            tesRecyclerView.setHasFixedSize(true)

            // Setup tombol kembali
            backButton.setOnClickListener {
                Log.d(TAG, "Back button clicked from kelola soal")
                showTes() // Kembali ke halaman kelola tes
            }

            // Update judul
            titleTextView.text = "Kelola Tes BK"
            subtitleTextView.text = "Ubah, hapus, atau aktifkan/nonaktifkan jenis tes yang tersedia."

            // Tampilkan loading
            loadingProgress.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE

            // Fetch data dari API
            fetchKelolaSoalData(tesRecyclerView, emptyStateLayout, loadingProgress)

        } catch (e: Exception) {
            Log.e(TAG, "Error in setupKelolaSoalTes: ${e.message}", e)
            Toast.makeText(this, "Error setup: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Fungsi untuk mengambil data kelola soal dari API
     */
    private fun fetchKelolaSoalData(
        recyclerView: androidx.recyclerview.widget.RecyclerView,
        emptyStateLayout: LinearLayout,
        loadingProgress: ProgressBar
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getKelolaSoalData()

                withContext(Dispatchers.Main) {
                    loadingProgress.visibility = View.GONE

                    if (response.isSuccessful) {
                        val result = response.body()
                        if (result != null && result.success && result.data != null) {
                            val data = result.data

                            // DEBUG: Tampilkan data yang diterima
                            Log.d(TAG, "Data kelola soal diterima:")
                            Log.d(TAG, "Success: ${result.success}")
                            Log.d(TAG, "Message: ${result.message}")
                            Log.d(TAG, "Total tes: ${data.totalTes}")
                            Log.d(TAG, "Total soal: ${data.totalSoal}")
                            Log.d(TAG, "Jumlah daftar tes: ${data.daftarTes.size}")

                            // Tampilkan detail setiap tes
                            data.daftarTes.forEachIndexed { index, tes ->
                                Log.d(TAG, "Tes ${index + 1}:")
                                Log.d(TAG, "  - ID: ${tes.idTes}")
                                Log.d(TAG, "  - Kategori: ${tes.kategoriTes}")
                                Log.d(TAG, "  - Status: ${tes.status}")
                                Log.d(TAG, "  - Jumlah Soal: ${tes.jumlahSoal}")
                            }

                            if (data.daftarTes.isNotEmpty()) {
                                // Setup adapter - gunakan data.daftarTes (List<TesDetail>)
                                val adapter = KelolaSoalAdapter(
                                    data.daftarTes,
                                    { tes -> // Callback untuk detail
                                        showTesDetailDialog(tes)
                                    },
                                    { tes -> // Callback untuk edit
                                        showEditTesForm(tes)
                                    }
                                )
                                recyclerView.adapter = adapter
                                emptyStateLayout.visibility = View.GONE

                                Toast.makeText(
                                    this@DashboardActivity,
                                    "✅ ${data.daftarTes.size} tes ditemukan",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                emptyStateLayout.visibility = View.VISIBLE
                                Toast.makeText(
                                    this@DashboardActivity,
                                    "Belum ada tes yang tersedia",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            val errorMsg = result?.message ?: "Data tidak valid"
                            Log.e(TAG, "API Response error: $errorMsg")
                            Toast.makeText(this@DashboardActivity,
                                "❌ $errorMsg",
                                Toast.LENGTH_SHORT).show()
                            emptyStateLayout.visibility = View.VISIBLE
                        }
                    } else {
                        val errorCode = response.code()
                        Log.e(TAG, "HTTP Error: $errorCode - ${response.message()}")
                        Toast.makeText(this@DashboardActivity,
                            "❌ Error $errorCode: ${response.message()}",
                            Toast.LENGTH_SHORT).show()
                        emptyStateLayout.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingProgress.visibility = View.GONE
                    Log.e(TAG, "Network error: ${e.message}", e)
                    Toast.makeText(this@DashboardActivity,
                        "❌ Network error: ${e.message}",
                        Toast.LENGTH_SHORT).show()
                    emptyStateLayout.visibility = View.VISIBLE
                }
            }
        }
    }

    /**
     * FUNGSI BARU: Menampilkan form edit tes (formkelolates.xml)
     */
    private fun showEditTesForm(tes: TesDetail) {
        Log.d(TAG, "showEditTesForm called for tes: ${tes.kategoriTes} (ID: ${tes.idTes})")

        fragmentContainer.removeAllViews()

        val editTesView = layoutInflater.inflate(R.layout.formkelolates, null)
        fragmentContainer.addView(editTesView)

        // Tetapkan menu navigasi tes sebagai aktif
        navigationView.setCheckedItem(R.id.nav_tes)

        // Update judul dengan nama tes yang sedang diedit
        titleText.text = "Edit Tes: ${tes.kategoriTes}"

        // Setup form edit tes dengan data dari API
        setupEditTesForm(editTesView, tes)
    }

    /**
     * FUNGSI BARU: Setup form edit tes dengan RecyclerView
     */
    private fun setupEditTesForm(editTesView: View, tes: TesDetail) {
        Log.d(TAG, "setupEditTesForm called for tes: ${tes.kategoriTes} (ID: ${tes.idTes})")

        try {
            // Temukan semua views dari formkelolates.xml
            val headerText = editTesView.findViewById<TextView>(R.id.tvHeaderTes)
            val subtitleText = editTesView.findViewById<TextView>(R.id.tvSubtitleTes)
            val totalSoalText = editTesView.findViewById<TextView>(R.id.tvTotalSoalTes)
            val btnTambahSoal = editTesView.findViewById<Button>(R.id.btnTambahSoal)
            val btnBatal = editTesView.findViewById<Button>(R.id.btnBatal)
            val rvSoalTes = editTesView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvSoalTes)

            // Update header dengan nama tes
            headerText.text = "Kelola Tes : ${tes.kategoriTes}"

            // Setup RecyclerView
            rvSoalTes.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
            rvSoalTes.setHasFixedSize(true)

            // Setup tombol tambah soal
            btnTambahSoal.setOnClickListener {
                Toast.makeText(this, "Menambah soal baru untuk ${tes.kategoriTes}", Toast.LENGTH_SHORT).show()
                // Implementasi tambah soal nanti
            }

            // Setup tombol batal
            btnBatal.setOnClickListener {
                Log.d(TAG, "Batal button clicked from edit tes form")
                showKelolaSoalTes() // Kembali ke halaman kelola soal
            }

            // Tampilkan loading
            val progressDialog = ProgressDialog(this).apply {
                setMessage("Memuat data soal...")
                setCancelable(false)
            }
            progressDialog.show()

            // Fetch data soal dari API
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d(TAG, "🔍 Mengambil data soal untuk tes ID: ${tes.idTes}")

                    val response = ApiClient.apiService.getSoalByTes(tes.idTes)
                    Log.d(TAG, "✅ Response code: ${response.code()}")

                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()

                        if (response.isSuccessful) {
                            val result = response.body()
                            Log.d(TAG, "📊 Response body ada: ${result != null}")

                            if (result != null) {
                                Log.d(TAG, "✅ Success status: ${result.success}")
                                Log.d(TAG, "✅ Error message: ${result.error}")

                                if (result.success && result.data != null) {
                                    val data = result.data
                                    val soalList = data.soal_list ?: emptyList()

                                    Log.d(TAG, "🎯 Jumlah soal diterima: ${soalList.size}")

                                    // Debug detail setiap soal - DENGAN PENANGANAN NULL
                                    if (soalList.isNotEmpty()) {
                                        Log.d(TAG, "=== DETAIL SOAL ===")
                                        for (i in soalList.indices) {
                                            val soal = soalList[i]
                                            Log.d(TAG, "\n📝 Soal ${i + 1}:")
                                            Log.d(TAG, "   - ID: ${soal.id_soal}")
                                            Log.d(TAG, "   - Pertanyaan: ${soal.pertanyaan}")
                                            Log.d(TAG, "   - ID Tes: ${soal.id_tes}")

                                            // Cek apakah opsi_list ada dan tidak null
                                            val opsiList = soal.opsi_list
                                            if (opsiList != null) {
                                                Log.d(TAG, "   - Jumlah opsi: ${opsiList.size}")

                                                if (opsiList.isNotEmpty()) {
                                                    for (j in opsiList.indices) {
                                                        val opsi = opsiList[j]
                                                        Log.d(TAG, "     ○ Opsi ${j + 1}:")
                                                        Log.d(TAG, "       ID: ${opsi.id_opsi}")
                                                        Log.d(TAG, "       Text: ${opsi.opsi_text}")
                                                        Log.d(TAG, "       Bobot: ${opsi.bobot}")
                                                    }
                                                } else {
                                                    Log.w(TAG, "   ⚠ Opsi list KOSONG (size: 0)")
                                                }
                                            } else {
                                                Log.e(TAG, "   ❌ Opsi list is NULL!")
                                            }
                                        }
                                        Log.d(TAG, "=== END DETAIL ===")
                                    } else {
                                        Log.w(TAG, "⚠ Soal list KOSONG")
                                    }

                                    // Update total soal
                                    totalSoalText.text = "Total ${data.jumlah_soal} soal untuk tes ini"

                                    // Cek apakah ada soal
                                    if (soalList.isNotEmpty()) {
                                        // Setup adapter untuk RecyclerView
                                        val adapter = SoalTesAdapter(
                                            soalList = soalList,
                                            onEditClick = { soal ->
                                                // Handle edit soal - langsung pakai data yang sudah ada
                                                showEditSoalFormDialog(soal)
                                            },
                                            onDeleteClick = { soal ->
                                                // Handle delete soal
                                                showDeleteSoalDialog(soal.id_soal, soal.pertanyaan)
                                            }
                                        )
                                        rvSoalTes.adapter = adapter

                                        Toast.makeText(
                                            this@DashboardActivity,
                                            "✅ ${soalList.size} soal dimuat",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        // Tampilkan pesan jika tidak ada soal
                                        Toast.makeText(
                                            this@DashboardActivity,
                                            "ℹ️ Belum ada soal untuk tes ini",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                } else {
                                    // Data null atau success = false
                                    val errorMsg = result.error ?: "Data tidak valid atau kosong"
                                    Log.e(TAG, "❌ API Error: $errorMsg")
                                    Log.e(TAG, "❌ Data field is null: ${result.data == null}")

                                    Toast.makeText(
                                        this@DashboardActivity,
                                        "❌ $errorMsg",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } else {
                                // Result null
                                Log.e(TAG, "❌ Response body is NULL")
                                Toast.makeText(
                                    this@DashboardActivity,
                                    "❌ Tidak ada data yang diterima dari server",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Log.e(TAG, "❌ HTTP Error: ${response.code()} - ${response.message()}")

                            // Coba baca error body
                            try {
                                val errorBody = response.errorBody()?.string()
                                Log.e(TAG, "❌ Error body: $errorBody")
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Cannot read error body: ${e.message}")
                            }

                            Toast.makeText(
                                this@DashboardActivity,
                                "❌ Error ${response.code()}: ${response.message()}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        Log.e(TAG, "❌ Exception fetching soal data: ${e.message}", e)

                        // Tampilkan error lebih detail
                        val errorMsg = when (e) {
                            is java.net.UnknownHostException -> "Tidak dapat terhubung ke server. Cek koneksi internet."
                            is java.net.SocketTimeoutException -> "Timeout. Server terlalu lama merespon."
                            is javax.net.ssl.SSLHandshakeException -> "Error SSL. Pastikan URL HTTPS valid."
                            else -> "Error: ${e.message}"
                        }

                        Toast.makeText(
                            this@DashboardActivity,
                            "❌ $errorMsg",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in setupEditTesForm: ${e.message}", e)
            Toast.makeText(this, "❌ Error setup form edit: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * FUNGSI BARU: Menampilkan dialog form edit soal dengan model SoalData
     */
    private fun showEditSoalFormDialog(soal: SoalData) {
        try {
            // Buat dialog custom
            val dialog = android.app.AlertDialog.Builder(this)
                .setTitle("Edit Soal")
                .setCancelable(false)
                .create()

            // Inflate layout dialog
            val dialogView = layoutInflater.inflate(R.layout.dialog_edit_soal, null)
            dialog.setView(dialogView)

            // Temukan views dalam dialog
            val etPertanyaan = dialogView.findViewById<EditText>(R.id.etPertanyaan)
            val etOpsiA = dialogView.findViewById<EditText>(R.id.etOpsiA)
            val etBobotA = dialogView.findViewById<EditText>(R.id.etBobotA)
            val etOpsiB = dialogView.findViewById<EditText>(R.id.etOpsiB)
            val etBobotB = dialogView.findViewById<EditText>(R.id.etBobotB)
            val etOpsiC = dialogView.findViewById<EditText>(R.id.etOpsiC)
            val etBobotC = dialogView.findViewById<EditText>(R.id.etBobotC)
            val btnSimpan = dialogView.findViewById<Button>(R.id.btnSimpan)
            val btnBatal = dialogView.findViewById<Button>(R.id.btnBatal)

            // Set data ke form
            etPertanyaan.setText(soal.pertanyaan)

            // Set opsi dari soal.opsi_list
            val opsiList = soal.opsi_list ?: emptyList()
            if (opsiList.isNotEmpty()) {
                etOpsiA.setText(opsiList[0].opsi_text)
                etBobotA.setText(opsiList[0].bobot.toString())
            } else {
                etOpsiA.setText("")
                etBobotA.setText("0")
            }

            if (opsiList.size > 1) {
                etOpsiB.setText(opsiList[1].opsi_text)
                etBobotB.setText(opsiList[1].bobot.toString())
            } else {
                etOpsiB.setText("")
                etBobotB.setText("0")
            }

            if (opsiList.size > 2) {
                etOpsiC.setText(opsiList[2].opsi_text)
                etBobotC.setText(opsiList[2].bobot.toString())
            } else {
                etOpsiC.setText("")
                etBobotC.setText("0")
            }

            // Setup tombol simpan
            btnSimpan.setOnClickListener {
                val pertanyaanBaru = etPertanyaan.text.toString().trim()
                val opsiABaru = etOpsiA.text.toString().trim()
                val bobotABaru = etBobotA.text.toString().toIntOrNull() ?: 0
                val opsiBBaru = etOpsiB.text.toString().trim()
                val bobotBBaru = etBobotB.text.toString().toIntOrNull() ?: 0
                val opsiCBaru = etOpsiC.text.toString().trim()
                val bobotCBaru = etBobotC.text.toString().toIntOrNull() ?: 0

                if (pertanyaanBaru.isEmpty()) {
                    etPertanyaan.error = "Pertanyaan tidak boleh kosong"
                    return@setOnClickListener
                }

                if (opsiABaru.isEmpty() || opsiBBaru.isEmpty() || opsiCBaru.isEmpty()) {
                    Toast.makeText(this, "Semua opsi harus diisi", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Panggil API update soal
                updateSoal(
                    soal.id_soal,
                    pertanyaanBaru,
                    opsiABaru, bobotABaru,
                    opsiBBaru, bobotBBaru,
                    opsiCBaru, bobotCBaru,
                    dialog
                )
            }

            // Setup tombol batal
            btnBatal.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()

        } catch (e: Exception) {
            Log.e(TAG, "Error showing edit dialog: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * FUNGSI BARU: Update soal ke server
     */
    private fun updateSoal(
        idSoal: Int,
        pertanyaan: String,
        opsiA: String, bobotA: Int,
        opsiB: String, bobotB: Int,
        opsiC: String, bobotC: Int,
        dialog: android.app.AlertDialog
    ) {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Menyimpan perubahan...")
            setCancelable(false)
        }
        progressDialog.show()

        // Untuk sekarang, tampilkan data yang akan dikirim
        progressDialog.dismiss()
        dialog.dismiss()

        Log.d(TAG, "Mengupdate soal ID: $idSoal")
        Log.d(TAG, "Pertanyaan: $pertanyaan")
        Log.d(TAG, "Opsi A: $opsiA (Bobot: $bobotA)")
        Log.d(TAG, "Opsi B: $opsiB (Bobot: $bobotB)")
        Log.d(TAG, "Opsi C: $opsiC (Bobot: $bobotC)")

        Toast.makeText(
            this,
            "Fitur update soal akan segera tersedia",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * FUNGSI BARU: Menampilkan dialog konfirmasi hapus soal
     */
    private fun showDeleteSoalDialog(idSoal: Int, pertanyaan: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Hapus Soal")
            .setMessage("Apakah Anda yakin ingin menghapus soal ini?\n\n\"${pertanyaan.take(50)}...\"")
            .setPositiveButton("Ya, Hapus") { dialog, which ->
                // Panggil API hapus soal
                deleteSoal(idSoal)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    /**
     * FUNGSI BARU: Hapus soal dari server
     */
    private fun deleteSoal(idSoal: Int) {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Menghapus soal...")
            setCancelable(false)
        }
        progressDialog.show()

        // Untuk sekarang, tampilkan konfirmasi
        progressDialog.dismiss()

        Log.d(TAG, "Menghapus soal ID: $idSoal")

        Toast.makeText(
            this,
            "Fitur hapus soal akan segera tersedia",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Dialog untuk menampilkan detail tes
     */
    private fun showTesDetailDialog(tes: TesDetail) {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(tes.kategoriTes)
            .setMessage(
                """
                📋 Deskripsi: 
                ${tes.deskripsiTes}
                
                📊 Jumlah Soal: ${tes.jumlahSoal}
                🟢 Status: ${tes.statusText}
                📅 Dibuat: ${formatDate(tes.createdAt)}
                🔄 Diupdate: ${formatDate(tes.updatedAt)}
                """.trimIndent()
            )
            .setPositiveButton("Tutup") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }

    /**
     * Fungsi untuk format tanggal sederhana
     */
    private fun formatDate(dateString: String): String {
        return try {
            // Coba parsing format dari database
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(dateString)

            val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            outputFormat.format(date)
        } catch (e: Exception) {
            dateString // Return as-is jika error
        }
    }

    /**
     * Setup form tambah tes dengan semua fungsi dan listeners
     */
    private fun setupTambahTesForm(tambahTesView: View) {
        Log.d(TAG, "setupTambahTesForm called")

        try {
            // Temukan views
            val etNamaTesBaru = tambahTesView.findViewById<EditText>(R.id.et_nama_tes_baru)
            val etDeskripsiTes = tambahTesView.findViewById<EditText>(R.id.et_deskripsi_tes)
            val tvFileStatus = tambahTesView.findViewById<TextView>(R.id.tv_file_status)
            val btnBrowse = tambahTesView.findViewById<Button>(R.id.btn_browse)
            val btnBatal = tambahTesView.findViewById<Button>(R.id.btn_batal)
            val btnSimpan = tambahTesView.findViewById<Button>(R.id.btn_simpan)

            // Reset status file
            selectedFileUri = null
            selectedFileName = null
            tvFileStatus.text = "[Pilih file CSV]"
            tvFileStatus.setTextColor(Color.parseColor("#888888"))

            // Setup browse button untuk memilih file CSV
            btnBrowse.setOnClickListener {
                Log.d(TAG, "Browse button clicked")
                openFilePicker()
            }

            // Setup button batal (kembali ke halaman kelola tes)
            btnBatal.setOnClickListener {
                Log.d(TAG, "Batal button clicked")
                showTes() // Kembali ke halaman kelola tes
            }

            // Setup button simpan
            btnSimpan.setOnClickListener {
                Log.d(TAG, "Simpan button clicked")

                val namaTes = etNamaTesBaru.text.toString().trim()
                val deskripsi = etDeskripsiTes.text.toString().trim()

                // Validasi input
                if (namaTes.isEmpty()) {
                    etNamaTesBaru.error = "Nama tes harus diisi"
                    etNamaTesBaru.requestFocus()
                    return@setOnClickListener
                }

                if (deskripsi.isEmpty()) {
                    etDeskripsiTes.error = "Deskripsi tes harus diisi"
                    etDeskripsiTes.requestFocus()
                    return@setOnClickListener
                }

                if (selectedFileUri == null) {
                    Toast.makeText(
                        this,
                        "Silakan pilih file CSV terlebih dahulu",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                // Tampilkan progress dialog
                val progressDialog = ProgressDialog(this).apply {
                    setMessage("Mengupload tes ke server...")
                    setCancelable(false)
                }
                progressDialog.show()

                // Baca file CSV dan kirim ke API
                try {
                    val csvContent = readCSVFile(selectedFileUri!!)
                    if (csvContent == null) {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this,
                            "Gagal membaca file CSV",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }

                    // Validasi minimal CSV memiliki header
                    if (!csvContent.contains("PERTANYAAN") || !csvContent.contains("OPSI_A")) {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this,
                            "Format CSV tidak valid. Pastikan file memiliki header yang benar",
                            Toast.LENGTH_LONG
                        ).show()
                        return@setOnClickListener
                    }

                    Log.d(TAG, "CSV content length: ${csvContent.length}")
                    Log.d(TAG, "First 500 chars of CSV: ${csvContent.take(500)}...")

                    // Kirim ke API SEDERHANA tanpa token
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val request = TambahTesRequest(
                                nama_tes = namaTes,
                                deskripsi_tes = deskripsi,
                                csv_content = csvContent
                            )

                            Log.d(TAG, "Mengirim request ke API...")
                            Log.d(TAG, "Nama Tes: ${request.nama_tes}")
                            Log.d(TAG, "Deskripsi length: ${request.deskripsi_tes.length}")
                            Log.d(TAG, "CSV lines: ${csvContent.lines().size}")

                            // Gunakan API sederhana tanpa token
                            val response = ApiClient.apiService.tambahTes(request)

                            withContext(Dispatchers.Main) {
                                progressDialog.dismiss()

                                if (response.isSuccessful) {
                                    val result = response.body()
                                    Log.d(TAG, "API Response Status: ${response.code()}")
                                    Log.d(TAG, "API Response Body: $result")

                                    if (result != null) {
                                        if (result.status == "success") {
                                            Toast.makeText(
                                                this@DashboardActivity,
                                                "✅ ${result.message}\nID Tes: ${result.tes_id}",
                                                Toast.LENGTH_LONG
                                            ).show()

                                            // Refresh halaman kelola tes setelah 2 detik
                                            Handler(Looper.getMainLooper()).postDelayed({
                                                showTes()
                                            }, 2000)

                                        } else {
                                            Toast.makeText(
                                                this@DashboardActivity,
                                                "❌ ${result.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    } else {
                                        Log.e(TAG, "Response body is null")
                                        Toast.makeText(
                                            this@DashboardActivity,
                                            "❌ Tidak ada response dari server",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                } else {
                                    val errorCode = response.code()
                                    val errorBody = response.errorBody()?.string() ?: "No error body"

                                    Log.e(TAG, "API Error Code: $errorCode")
                                    Log.e(TAG, "API Error Body: $errorBody")

                                    val errorMessage = when (errorCode) {
                                        400 -> "Bad Request: Data tidak valid"
                                        404 -> "Endpoint tidak ditemukan"
                                        500 -> "Server error: Silakan coba lagi nanti"
                                        502 -> "Bad Gateway: Server sedang maintenance"
                                        503 -> "Service Unavailable: Server sedang sibuk"
                                        else -> "Error $errorCode: $errorBody"
                                    }

                                    Toast.makeText(
                                        this@DashboardActivity,
                                        "❌ $errorMessage",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        } catch (e: java.net.SocketTimeoutException) {
                            withContext(Dispatchers.Main) {
                                progressDialog.dismiss()
                                Log.e(TAG, "Socket timeout: ${e.message}", e)
                                Toast.makeText(
                                    this@DashboardActivity,
                                    "❌ Timeout: Koneksi ke server terlalu lama",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } catch (e: java.net.UnknownHostException) {
                            withContext(Dispatchers.Main) {
                                progressDialog.dismiss()
                                Log.e(TAG, "Unknown host: ${e.message}", e)
                                Toast.makeText(
                                    this@DashboardActivity,
                                    "❌ Tidak dapat terhubung ke server. Cek koneksi internet",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } catch (e: java.io.IOException) {
                            withContext(Dispatchers.Main) {
                                progressDialog.dismiss()
                                Log.e(TAG, "IO Error: ${e.message}", e)
                                Toast.makeText(
                                    this@DashboardActivity,
                                    "❌ Network error: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                progressDialog.dismiss()
                                Log.e(TAG, "Unexpected error: ${e.message}", e)
                                Toast.makeText(
                                    this@DashboardActivity,
                                    "❌ Error: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }

                } catch (e: Exception) {
                    progressDialog.dismiss()
                    Log.e(TAG, "Error processing CSV: ${e.message}", e)
                    Toast.makeText(
                        this,
                        "❌ Error membaca file: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in setupTambahTesForm: ${e.message}", e)
            Toast.makeText(this, "Error setup form: ${e.message}", Toast.LENGTH_SHORT).show()
        }
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
            val progressBarInitial = tesView.findViewById<ProgressBar>(R.id.progressBarInitial)
            val mainLayout = tesView.findViewById<LinearLayout>(R.id.kelolaTesMainLayout)
            val tvErrorTes = tesView.findViewById<TextView>(R.id.tvErrorTes)
            val swipeRefreshTes = tesView.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshTes)

            // Temukan TextView "Tambah Tes Baru"
            val tvTambahTesBaru = tesView.findViewById<TextView>(R.id.tvTambahTesBaru)

            // Temukan TextView "Kelola Tes BK"
            val tvKelolaTesBK = tesView.findViewById<TextView>(R.id.tvKelolaTesBK)

            // Setup RecyclerView
            rvDaftarTes.layoutManager = LinearLayoutManager(this)
            rvDaftarTes.setHasFixedSize(true)

            // Setup SwipeRefreshLayout
            swipeRefreshTes.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
            )

            swipeRefreshTes.setOnRefreshListener {
                Log.d(TAG, "Swipe refresh triggered for kelola tes")
                kelolaTesViewModel.fetchKelolaTesData()
            }

            // Setup click listeners untuk tombol aksi
            kelolaTesBKLayout.setOnClickListener {
                Log.d(TAG, "KelolaTesBKLayout clicked")
                showKelolaSoalTes()
            }

            // Listener untuk TextView "Kelola Tes BK"
            tvKelolaTesBK?.setOnClickListener {
                Log.d(TAG, "tvKelolaTesBK clicked")
                showKelolaSoalTes()
            }

            // Listener untuk layout Tambah Tes Baru
            tambahTesBaruLayout.setOnClickListener {
                Log.d(TAG, "TambahTesBaruLayout clicked")
                showTambahTesForm()
            }

            // Listener untuk TextView "Tambah Tes Baru"
            tvTambahTesBaru.setOnClickListener {
                Log.d(TAG, "tvTambahTesBaru clicked")
                showTambahTesForm()
            }

            // Setup observers untuk ViewModel
            kelolaTesViewModel.kelolaTesData.observe(this) { response ->
                swipeRefreshTes.isRefreshing = false
                progressBarInitial.visibility = View.GONE

                if (response != null && response.success && response.data != null) {
                    val data = response.data

                    Log.d(TAG, "✓ Data tes berhasil dimuat:")
                    Log.d(TAG, "  - Total Soal: ${data.totalSoal}")
                    Log.d(TAG, "  - Jenis Tes: ${data.jenisTes}")
                    Log.d(TAG, "  - Jumlah Tes: ${data.daftarTes.size}")

                    // Tampilkan main layout
                    mainLayout.visibility = View.VISIBLE
                    tvErrorTes.visibility = View.GONE

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
                    } else {
                        tvErrorTes.text = "Belum ada tes tersedia"
                        tvErrorTes.visibility = View.VISIBLE
                    }

                    Toast.makeText(this, "Data tes berhasil dimuat", Toast.LENGTH_SHORT).show()
                } else if (response != null && !response.success) {
                    Log.e(TAG, "API returned error: ${response.message}")
                    mainLayout.visibility = View.VISIBLE
                    tvErrorTes.text = "Error: ${response.message}"
                    tvErrorTes.visibility = View.VISIBLE

                    // Tampilkan data default jika error
                    tvTotalSoal.text = "0"
                    tvJenisTes.text = "0"
                }
            }

            kelolaTesViewModel.isLoading.observe(this) { isLoading ->
                Log.d(TAG, "KelolaTes isLoading: $isLoading")

                if (isLoading && !swipeRefreshTes.isRefreshing) {
                    progressBarInitial.visibility = View.VISIBLE
                    mainLayout.visibility = View.GONE
                }

                // Nonaktifkan swipe refresh saat loading initial
                swipeRefreshTes.isEnabled = !isLoading
            }

            kelolaTesViewModel.errorMessage.observe(this) { errorMessage ->
                swipeRefreshTes.isRefreshing = false
                progressBarInitial.visibility = View.GONE

                if (errorMessage != null) {
                    Log.e(TAG, "KelolaTes error: $errorMessage")
                    mainLayout.visibility = View.VISIBLE
                    tvErrorTes.text = "Error: $errorMessage"
                    tvErrorTes.visibility = View.VISIBLE

                    // Tampilkan data default jika error
                    tvTotalSoal.text = "0"
                    tvJenisTes.text = "0"
                }
            }

            // Tampilkan loading saat awal
            progressBarInitial.visibility = View.VISIBLE
            mainLayout.visibility = View.GONE

            // Load data dari API
            Log.d(TAG, "Fetching kelola tes data...")
            kelolaTesViewModel.fetchKelolaTesData()

        } catch (e: Exception) {
            Log.e(TAG, "Error in setupKelolaTesContent: ${e.message}", e)
            Toast.makeText(this, "Error setup: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showGuru() {
        Log.d(TAG, "showGuru called")

        // Debug: cek jumlah child sebelum clear
        Log.d(TAG, "Guru - Child count before: ${fragmentContainer.childCount}")

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
                } else if (titleText.text.toString().startsWith("Tambah Tes Baru")) {
                    // Jika sedang di form tambah tes, kembali ke kelola tes
                    showTes()
                } else if (titleText.text.toString().startsWith("Kelola Tes BK")) {
                    // Jika sedang di form kelola tes BK, kembali ke kelola tes
                    showTes()
                } else if (titleText.text.toString().startsWith("Edit Tes:")) {
                    // Jika sedang di form edit tes, kembali ke kelola soal
                    showKelolaSoalTes()
                } else if (titleText.text.toString() == "Kelola Tes") {
                    // Jika sedang di kelola tes, cek apakah ingin keluar app
                    if (System.currentTimeMillis() - backPressedTime < 2000) {
                        finish()
                    } else {
                        Toast.makeText(this@DashboardActivity,
                            "Tekan kembali sekali lagi untuk keluar",
                            Toast.LENGTH_SHORT).show()
                        backPressedTime = System.currentTimeMillis()
                    }
                } else {
                    // Untuk halaman lain, cek apakah ingin keluar app
                    if (System.currentTimeMillis() - backPressedTime < 2000) {
                        finish()
                    } else {
                        Toast.makeText(this@DashboardActivity,
                            "Tekan kembali sekali lagi untuk keluar",
                            Toast.LENGTH_SHORT).show()
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

    // ==================== FUNGSI FILE HANDLING ====================
    /**
     * Fungsi untuk membuka file picker
     * HANYA SATU FUNGSI INI YANG ADA
     */
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)

            // Filter untuk file CSV
            val mimeTypes = arrayOf(
                "text/csv",
                "text/comma-separated-values",
                "application/csv",
                "application/vnd.ms-excel",
                "text/plain"
            )

            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        }

        try {
            startActivityForResult(
                Intent.createChooser(intent, "Pilih File CSV"),
                REQUEST_CODE_PICK_CSV
            )
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(
                this,
                "Silakan install file manager untuk memilih file",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Fungsi untuk membaca file CSV menjadi string
     * HANYA SATU FUNGSI INI YANG ADA
     */
    private fun readCSVFile(uri: Uri): String? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                // BACA SEMUA BYTE DULU ke ByteArray (ini mendukung reset)
                val bytes = inputStream.readBytes()

                // Coba berbagai encoding
                val encodings = listOf("UTF-8", "ISO-8859-1", "Windows-1252")
                var content: String? = null

                for (encoding in encodings) {
                    try {
                        // Buat ByteArrayInputStream baru untuk setiap encoding
                        val byteArrayStream = ByteArrayInputStream(bytes)
                        val reader = BufferedReader(InputStreamReader(byteArrayStream, encoding))
                        val stringBuilder = StringBuilder()
                        var line: String?

                        while (reader.readLine().also { line = it } != null) {
                            stringBuilder.append(line)
                            stringBuilder.append("\n")
                        }

                        reader.close()

                        val testContent = stringBuilder.toString()
                        // Validasi sederhana: pastikan tidak banyak karakter aneh
                        if (testContent.isNotEmpty() && !testContent.contains("�")) {
                            content = testContent
                            Log.d(TAG, "Successfully read CSV with encoding: $encoding")
                            break
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed with encoding $encoding: ${e.message}")
                        // Lanjut ke encoding berikutnya
                    }
                }

                // Fallback ke UTF-8 jika semua gagal
                if (content == null) {
                    content = String(bytes, Charsets.UTF_8)
                    Log.d(TAG, "Using UTF-8 fallback")
                }

                // Log untuk debugging
                if (content != null) {
                    Log.d(TAG, "CSV Content loaded: ${content.length} characters")
                    Log.d(TAG, "First 3 lines of CSV:")
                    content.lines().take(3).forEachIndexed { index, line ->
                        Log.d(TAG, "Line $index: $line")
                    }

                    // Hapus BOM character jika ada
                    if (content.startsWith("\uFEFF")) {
                        Log.d(TAG, "Detected BOM character, removing...")
                        content = content.substring(1)
                    }
                }

                content
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading CSV file: ${e.message}", e)
            null
        }
    }

    /**
     * Handle activity result untuk file picker
     * HANYA SATU FUNGSI INI YANG ADA
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_PICK_CSV && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    // Simpan URI file
                    selectedFileUri = uri

                    // Dapatkan nama file
                    selectedFileName = getFileNameFromUri(uri)

                    // Update UI
                    val tambahTesView = fragmentContainer.getChildAt(0)
                    val tvFileStatus = tambahTesView.findViewById<TextView?>(R.id.tv_file_status)

                    if (tvFileStatus != null && selectedFileName != null) {
                        tvFileStatus.text = "$selectedFileName (Dipilih)"
                        tvFileStatus.setTextColor(Color.parseColor("#4CAF50"))

                        // Set nama file sebagai nama tes default jika kosong
                        val etNamaTesBaru = tambahTesView.findViewById<EditText?>(R.id.et_nama_tes_baru)
                        if (etNamaTesBaru != null && etNamaTesBaru.text.toString().trim().isEmpty()) {
                            val baseName = selectedFileName!!.substringBeforeLast(".")
                            if (baseName.isNotEmpty()) {
                                etNamaTesBaru.setText(baseName)
                            }
                        }
                    }

                    Toast.makeText(
                        this,
                        "File dipilih: $selectedFileName",
                        Toast.LENGTH_SHORT
                    ).show()

                    Log.d(TAG, "File selected: $selectedFileName")

                } catch (e: Exception) {
                    Log.e(TAG, "Error processing selected file: ${e.message}", e)
                    Toast.makeText(
                        this,
                        "Error memilih file: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    /**
     * Fungsi untuk mendapatkan nama file dari URI
     * HANYA SATU FUNGSI INI YANG ADA
     */
    private fun getFileNameFromUri(uri: Uri): String? {
        var fileName: String? = null

        // Coba dapatkan nama file dari cursor
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    fileName = cursor.getString(displayNameIndex)
                }
            }
        }

        // Jika tidak dapat dari cursor, coba dari path
        if (fileName.isNullOrEmpty()) {
            val path = uri.path
            if (path != null) {
                fileName = path.substringAfterLast("/")
            }
        }

        return fileName ?: "unknown.csv"
    }
}