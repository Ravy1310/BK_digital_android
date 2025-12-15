// File: app/src/main/java/com/example/login/ui/dashboard/DashboardActivity.kt
package com.example.login.ui.dashboard

import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
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
import com.example.login.adapter.TesAdapter
import com.example.login.api.ApiClient
import com.example.login.models.TambahTesRequest
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
import java.io.InputStreamReader
import android.os.Handler
import android.os.Looper
import java.io.ByteArrayInputStream
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
                // Menggunakan ContextCompat.getColor dengan this@DashboardActivity
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

    /**
     * Fungsi untuk membuka file picker
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
     */
    /**
     * Fungsi untuk membaca file CSV menjadi string (VERSI DIPERBAIKI)
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

//                    // Preview CSV content (opsional)
//                    try {
//                        val csvPreview = readCSVFile(uri)?.lines()?.take(3)?.joinToString("\n")
//                        Log.d(TAG, "CSV Preview:\n$csvPreview")
//                    } catch (e: Exception) {
//                        Log.w(TAG, "Could not preview CSV: ${e.message}")
//                    }

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
                Toast.makeText(this, "Membuka Kelola Soal Tes", Toast.LENGTH_SHORT).show()
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

    private fun showSiswa() {
        Log.d(TAG, "showSiswa called")

        // Debug: cek jumlah child sebelum clear
        Log.d(TAG, "Siswa - Child count before: ${fragmentContainer.childCount}")

        fragmentContainer.removeAllViews()

        val textView = TextView(this)
        textView.text = "Halaman Kelola Siswa\n\nFitur akan segera tersedia"
        textView.textSize = 18f
        textView.gravity = android.view.Gravity.CENTER
        fragmentContainer.addView(textView)
        Toast.makeText(this, "Kelola Data Siswa", Toast.LENGTH_SHORT).show()
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
                } else if (titleText.text == "Tambah Tes Baru") {
                    // Jika sedang di form tambah tes, kembali ke kelola tes
                    showTes()
                } else if (titleText.text == "Kelola Tes") {
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
}