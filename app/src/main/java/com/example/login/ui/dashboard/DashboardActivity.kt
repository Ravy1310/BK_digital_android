package com.example.login.ui.dashboard

import android.app.Dialog
import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
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
import com.example.login.adapter.*
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
import kotlinx.coroutines.*



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

    // Data siswa dari API
    private val siswaList = mutableListOf<SiswaData>()

    // Variables untuk Tes
    private var currentTes: TesDetail? = null

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

        fragmentContainer.removeAllViews()
        val siswaView = layoutInflater.inflate(R.layout.activity_kelola_siswa, null)
        fragmentContainer.addView(siswaView)

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
                showTambahSiswaForm()
            }

            // Load data dari API
            fetchDataSiswa()

        } catch (e: Exception) {
            Log.e(TAG, "Error in setupKelolaSiswaContent: ${e.message}", e)
            Toast.makeText(this, "Error setup kelola siswa: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchDataSiswa() {
        showLoadingSiswa()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Mengambil data siswa dari API...")

                // Gunakan ApiClient.apiService yang sudah ada
                val response = ApiClient.apiService.getDataSiswa()

                withContext(Dispatchers.Main) {
                    hideLoadingSiswa()

                    if (response.isSuccessful) {
                        val result = response.body()
                        if (result != null && result.success) {
                            val data = result.data ?: emptyList()

                            // Update UI dengan data dari API
                            updateUISiswaWithApiData(data)

                            Log.d(TAG, "✓ ${data.size} data siswa dimuat dari API")
                            Toast.makeText(
                                this@DashboardActivity,
                                "✅ ${data.size} data siswa dimuat",
                                Toast.LENGTH_SHORT
                            ).show()

                        } else {
                            val errorMsg = result?.message ?: "Data tidak valid"
                            showErrorState("❌ $errorMsg")
                        }
                    } else {
                        val errorCode = response.code()
                        showErrorState("❌ Error $errorCode: ${response.message()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideLoadingSiswa()

                    val errorMsg = when (e) {
                        is java.net.UnknownHostException -> "Tidak dapat terhubung ke server"
                        is java.net.SocketTimeoutException -> "Timeout. Server terlalu lama merespon."
                        else -> "Error: ${e.message}"
                    }

                    showErrorState("❌ $errorMsg")
                    Log.e(TAG, "Network error fetching siswa: ${e.message}", e)
                }
            }
        }
    }

    private fun updateUISiswaWithApiData(data: List<SiswaData>) {
        siswaList.clear()
        siswaList.addAll(data)

        // Update statistics
        updateStatisticsSiswa()

        // Update table
        updateTableSiswa()
    }

    private fun updateStatisticsSiswa() {
        val total = siswaList.size
        val male = siswaList.count { it.jenis_kelamin == "Laki-Laki" }
        val female = siswaList.count { it.jenis_kelamin == "Perempuan" }

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

    private fun addStudentRow(siswa: SiswaData, position: Int) {
        // Inflate row layout
        val rowView = layoutInflater.inflate(R.layout.table_row_empty, tableRowsContainer, false)

        // Set data
        rowView.findViewById<TextView>(R.id.tvId).text = siswa.id_siswa
        rowView.findViewById<TextView>(R.id.tvNama).text = siswa.nama
        rowView.findViewById<TextView>(R.id.tvKelas).text = siswa.kelas
        rowView.findViewById<TextView>(R.id.tvGender).text = siswa.jenis_kelamin

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

    private fun showSiswaDetail(siswa: SiswaData) {
        try {
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.dialog_detail_siswa)
            dialog.setCancelable(true)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.setDimAmount(0.6f)

            // Temukan views
            val btnClose = dialog.findViewById<ImageView>(R.id.btnClose)
            val tvIdSiswa = dialog.findViewById<TextView>(R.id.tvIdSiswa)
            val tvNama = dialog.findViewById<TextView>(R.id.tvNama)
            val tvKelas = dialog.findViewById<TextView>(R.id.tvKelas)
            val tvGender = dialog.findViewById<TextView>(R.id.tvGender)
            val tvTahunMasuk = dialog.findViewById<TextView>(R.id.tvTahunMasuk)
            val btnEdit = dialog.findViewById<Button>(R.id.btnEdit)
            val btnHapus = dialog.findViewById<Button>(R.id.btnHapus)

            // Set data siswa
            tvIdSiswa.text = siswa.id_siswa
            tvNama.text = siswa.nama
            tvKelas.text = siswa.kelas
            tvGender.text = siswa.jenis_kelamin
            tvTahunMasuk.text = siswa.tahun_masuk

            // Setup tombol
            btnClose.setOnClickListener { dialog.dismiss() }

            btnEdit.setOnClickListener {
                dialog.dismiss()
                editSiswa(siswa)
            }

            btnHapus.setOnClickListener {
                dialog.dismiss()
                deleteSiswa(siswa)
            }

            // Tampilkan dialog
            dialog.show()

            // Atur ukuran dialog
            val displayMetrics = resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.9).toInt()
            val height = ViewGroup.LayoutParams.WRAP_CONTENT

            dialog.window?.setLayout(width, height)
            dialog.window?.setGravity(Gravity.CENTER)

        } catch (e: Exception) {
            Log.e(TAG, "Error showing siswa detail dialog: ${e.message}", e)

            // Fallback ke AlertDialog jika ada error
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Detail Siswa")
                .setMessage(
                    "ID: ${siswa.id_siswa}\n" +
                            "Nama: ${siswa.nama}\n" +
                            "Kelas: ${siswa.kelas}\n" +
                            "Jenis Kelamin: ${siswa.jenis_kelamin}\n" +
                            "Tahun Masuk: ${siswa.tahun_masuk}"
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
    }

    private fun editSiswa(siswa: SiswaData) {
        Log.d(TAG, "Memulai edit siswa: ${siswa.nama} (ID: ${siswa.id_siswa})")

        // Tampilkan loading
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Mengambil data siswa...")
            setCancelable(false)
        }
        progressDialog.show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Mengambil detail siswa dari API untuk ID: ${siswa.id_siswa}")

                // Gunakan fungsi yang sama dengan getDataSiswa
                val response = ApiClient.apiService.getSiswaById(siswa.id_siswa)

                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()

                    if (response.isSuccessful) {
                        val result = response.body()

                        if (result != null && result.success) {
                            val data = result.data ?: emptyList()

                            // Cari siswa dengan ID yang sesuai
                            val detailSiswa = data.find { it.id_siswa == siswa.id_siswa }

                            if (detailSiswa != null) {
                                Log.d(TAG, "Data siswa berhasil diambil: ${detailSiswa.nama}")

                                // Redirect ke form tambah siswa dengan data terisi
                                showTambahSiswaForm(detailSiswa)
                            } else {
                                Log.e(TAG, "Siswa tidak ditemukan dalam response")

                                // Fallback: gunakan data yang sudah ada
                                Toast.makeText(
                                    this@DashboardActivity,
                                    "Menggunakan data lokal",
                                    Toast.LENGTH_SHORT
                                ).show()
                                showTambahSiswaForm(siswa)
                            }
                        } else {
                            val errorMsg = result?.message ?: "Data tidak ditemukan"
                            Log.e(TAG, "Error detail siswa: $errorMsg")

                            // Fallback: gunakan data yang sudah ada
                            Toast.makeText(
                                this@DashboardActivity,
                                "Menggunakan data lokal",
                                Toast.LENGTH_SHORT
                            ).show()
                            showTambahSiswaForm(siswa)
                        }
                    } else {
                        val errorCode = response.code()
                        Log.e(TAG, "Error $errorCode: ${response.message()}")

                        // Fallback: gunakan data yang sudah ada
                        Toast.makeText(
                            this@DashboardActivity,
                            "Menggunakan data lokal",
                            Toast.LENGTH_SHORT
                        ).show()
                        showTambahSiswaForm(siswa)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Log.e(TAG, "Error fetching detail siswa: ${e.message}", e)

                    // Fallback: gunakan data yang sudah ada
                    Toast.makeText(
                        this@DashboardActivity,
                        "Menggunakan data lokal",
                        Toast.LENGTH_SHORT
                    ).show()
                    showTambahSiswaForm(siswa)
                }
            }
        }
    }

    private fun deleteSiswa(siswa: SiswaData) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Konfirmasi Hapus")
            .setMessage(
                """
            Apakah Anda yakin ingin menghapus siswa ini?
            
            👤 Nama: ${siswa.nama}
            🆔 ID: ${siswa.id_siswa}
            📚 Kelas: ${siswa.kelas}
            
            Data yang dihapus tidak dapat dikembalikan.
            """.trimIndent()
            )
            .setPositiveButton("Ya, Hapus") { _, _ ->
                // Panggil fungsi untuk menghapus dari API
                deleteSiswaFromDatabase(siswa)
            }
            .setNegativeButton("Batal", null)
            .setIcon(ContextCompat.getDrawable(this, android.R.drawable.ic_delete))
            .show()
    }
    /**
     * Fungsi untuk menghapus data siswa dari database via API
     */
    private fun deleteSiswaFromDatabase(siswa: SiswaData) {
        // Tampilkan ProgressDialog
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Menghapus data siswa...")
            setCancelable(false)
        }
        progressDialog.show()

        // Buat request
        val request = HapusSiswaRequest(
            id_siswa = siswa.id_siswa
        )

        // Kirim ke API
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Mengirim request hapus siswa: $request")
                val response = ApiClient.apiService.hapusSiswa(request)

                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()

                    if (response.isSuccessful) {
                        val result = response.body()

                        if (result != null) {
                            if (result.success) {
                                Toast.makeText(
                                    this@DashboardActivity,
                                    "✅ ${result.message}",
                                    Toast.LENGTH_LONG
                                ).show()

                                Log.d(TAG, "Data siswa berhasil dihapus: ${siswa.nama}")

                                // Hapus dari list lokal
                                siswaList.remove(siswa)

                                // Update UI
                                updateStatisticsSiswa()
                                updateTableSiswa()

                                // Jika tidak ada data lagi, tampilkan empty state
                                if (siswaList.isEmpty()) {
                                    emptyStateLayout.visibility = View.VISIBLE
                                    tableRowsContainer.visibility = View.GONE
                                }

                            } else {
                                val errorMsg = result.error ?: result.message
                                Toast.makeText(
                                    this@DashboardActivity,
                                    "❌ $errorMsg",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                this@DashboardActivity,
                                "❌ Tidak ada response dari server",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        val errorCode = response.code()
                        val errorBody = response.errorBody()?.string() ?: "No error body"

                        Log.e(TAG, "API Error: $errorCode - $errorBody")

                        Toast.makeText(
                            this@DashboardActivity,
                            "❌ Error $errorCode: ${response.message()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Log.e(TAG, "Error deleting siswa: ${e.message}", e)

                    val errorMsg = when (e) {
                        is java.net.UnknownHostException -> "Tidak dapat terhubung ke server"
                        is java.net.SocketTimeoutException -> "Timeout, coba lagi"
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
    }

    private fun showTambahSiswaForm(siswaData: SiswaData? = null) {
        fragmentContainer.removeAllViews()
        val tambahSiswaView = layoutInflater.inflate(R.layout.tambahsiswa, null)
        fragmentContainer.addView(tambahSiswaView)

        navigationView.setCheckedItem(R.id.nav_siswa)

        if (siswaData != null) {
            titleText.text = "Edit Data Siswa"
            setupTambahSiswaForm(tambahSiswaView, true, siswaData)
        } else {
            titleText.text = "Tambah Siswa Baru"
            setupTambahSiswaForm(tambahSiswaView, false, null)
        }
    }

    private fun setupTambahSiswaForm(tambahSiswaView: View, isEditMode: Boolean = false, siswaData: SiswaData? = null) {
        Log.d(TAG, "setupTambahSiswaForm called - isEditMode: $isEditMode")

        try {
            val tvTitle = tambahSiswaView.findViewById<TextView>(R.id.tvTitle)
            val btnBatal = tambahSiswaView.findViewById<Button>(R.id.btnBack)
            val btnSimpan = tambahSiswaView.findViewById<Button>(R.id.btnSimpan)
            val etID = tambahSiswaView.findViewById<EditText>(R.id.etID)
            val etName = tambahSiswaView.findViewById<EditText>(R.id.etName)
            val etKelas = tambahSiswaView.findViewById<EditText>(R.id.etKelas)
            val etTahunMasuk = tambahSiswaView.findViewById<EditText>(R.id.etTahunMasuk)
            val rgJenisKelamin = tambahSiswaView.findViewById<RadioGroup>(R.id.rgJenisKelamin)

            // Set judul berdasarkan mode
            if (isEditMode) {
                tvTitle.text = "Edit Data Siswa"
                btnSimpan.text = "Update"

                // Isi data jika ada
                siswaData?.let { siswa ->
                    etID.setText(siswa.id_siswa)
                    etName.setText(siswa.nama)
                    etKelas.setText(siswa.kelas)
                    etTahunMasuk.setText(siswa.tahun_masuk)

                    // Set jenis kelamin
                    when (siswa.jenis_kelamin.toLowerCase()) {
                        "laki-laki", "laki", "laki laki" -> rgJenisKelamin.check(R.id.rbLaki)
                        "perempuan" -> rgJenisKelamin.check(R.id.rbPerempuan)
                    }

                    // Nonaktifkan field ID di mode edit
                    etID.isEnabled = false
                    etID.setTextColor(Color.GRAY)
                }
            } else {
                tvTitle.text = "Tambah Data Siswa"
                btnSimpan.text = "Simpan"
            }

            // Setup tombol Batal
            btnBatal.setOnClickListener {
                Log.d(TAG, "Tombol Batal diklik")
                showSiswa()
            }

            // Setup tombol Simpan/Update
            btnSimpan.setOnClickListener {
                // Validasi semua field
                val id = etID.text.toString().trim()
                val nama = etName.text.toString().trim()
                val kelas = etKelas.text.toString().trim()
                val tahunMasuk = etTahunMasuk.text.toString().trim()

                // Reset error
                etID.error = null
                etName.error = null
                etKelas.error = null
                etTahunMasuk.error = null

                var hasError = false

                // Validasi Nama
                if (nama.isEmpty()) {
                    etName.error = "Nama tidak boleh kosong"
                    etName.requestFocus()
                    hasError = true
                }

                // Validasi Kelas
                if (kelas.isEmpty()) {
                    etKelas.error = "Kelas tidak boleh kosong"
                    if (!hasError) {
                        etKelas.requestFocus()
                        hasError = true
                    }
                }

                // Validasi Tahun Masuk
                if (tahunMasuk.isEmpty()) {
                    etTahunMasuk.error = "Tahun masuk tidak boleh kosong"
                    if (!hasError) {
                        etTahunMasuk.requestFocus()
                        hasError = true
                    }
                } else if (tahunMasuk.length != 4 || !tahunMasuk.all { it.isDigit() }) {
                    etTahunMasuk.error = "Tahun harus 4 digit angka"
                    if (!hasError) {
                        etTahunMasuk.requestFocus()
                        hasError = true
                    }
                }

                // Validasi Jenis Kelamin
                val selectedId = rgJenisKelamin.checkedRadioButtonId
                if (selectedId == -1) {
                    Toast.makeText(this@DashboardActivity, "Pilih jenis kelamin", Toast.LENGTH_SHORT).show()
                    hasError = true
                }

                if (hasError) {
                    return@setOnClickListener
                }

                // Dapatkan jenis kelamin
                val jenisKelamin = when (selectedId) {
                    R.id.rbLaki -> "Laki-laki"
                    R.id.rbPerempuan -> "Perempuan"
                    else -> ""
                }

                if (isEditMode) {
                    // Mode edit: update data
                    updateSiswaInDatabase(id, nama, kelas, tahunMasuk, jenisKelamin)
                } else {
                    // Mode tambah: simpan data baru
                    saveSiswaToDatabase(id, nama, kelas, tahunMasuk, jenisKelamin)
                }
            }

            // Fokus ke field yang sesuai
            if (!isEditMode) {
                etID.requestFocus()
            } else {
                etName.requestFocus()
            }

            Log.d(TAG, "Form siswa berhasil di-setup (Mode: ${if (isEditMode) "Edit" else "Tambah"})")

        } catch (e: Exception) {
            Log.e(TAG, "Error in setupTambahSiswaForm: ${e.message}", e)
            Toast.makeText(this, "Gagal memuat form: ${e.message}", Toast.LENGTH_SHORT).show()
            showSiswa()
        }
    }

    /**
     * Fungsi baru: Update data siswa ke database via API
     */
    private fun updateSiswaInDatabase(
        idSiswa: String,
        nama: String,
        kelas: String,
        tahunMasuk: String,
        jenisKelamin: String
    ) {
        // Validasi input
        if (idSiswa.isEmpty() || nama.isEmpty() || kelas.isEmpty() || tahunMasuk.isEmpty() || jenisKelamin.isEmpty()) {
            Toast.makeText(this, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        // Validasi tahun masuk
        if (tahunMasuk.length != 4 || !tahunMasuk.all { it.isDigit() }) {
            Toast.makeText(this, "Tahun harus 4 digit angka", Toast.LENGTH_SHORT).show()
            return
        }

        // Tampilkan ProgressDialog
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Memperbarui data siswa...")
            setCancelable(false)
        }
        progressDialog.show()

        // Buat request
        val request = UpdateSiswaRequest(
            id_siswa = idSiswa,
            nama = nama,
            kelas = kelas,
            tahun_masuk = tahunMasuk,
            jenis_kelamin = jenisKelamin
        )

        // Kirim ke API
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Mengirim request update siswa: $request")
                val response = ApiClient.apiService.updateSiswa(request)

                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()

                    if (response.isSuccessful) {
                        val result = response.body()

                        if (result != null) {
                            if (result.success) {
                                Toast.makeText(
                                    this@DashboardActivity,
                                    "✅ ${result.message}",
                                    Toast.LENGTH_LONG
                                ).show()

                                Log.d(TAG, "Data siswa berhasil diupdate: ${result.data}")

                                // Kembali ke halaman kelola siswa dan refresh data
                                Handler(Looper.getMainLooper()).postDelayed({
                                    showSiswa()
                                }, 1500)

                            } else {
                                val errorMsg = result.error ?: result.message
                                Toast.makeText(
                                    this@DashboardActivity,
                                    "❌ $errorMsg",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                this@DashboardActivity,
                                "❌ Tidak ada response dari server",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        val errorCode = response.code()
                        val errorBody = response.errorBody()?.string() ?: "No error body"

                        Log.e(TAG, "API Error: $errorCode - $errorBody")

                        Toast.makeText(
                            this@DashboardActivity,
                            "❌ Error $errorCode: ${response.message()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Log.e(TAG, "Error updating siswa: ${e.message}", e)

                    val errorMsg = when (e) {
                        is java.net.UnknownHostException -> "Tidak dapat terhubung ke server"
                        is java.net.SocketTimeoutException -> "Timeout, coba lagi"
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
    }

    private fun saveSiswaToDatabase(
        id: String,
        nama: String,
        kelas: String,
        tahunMasuk: String,
        jenisKelamin: String
    ) {
        if (id.isEmpty() || nama.isEmpty() || kelas.isEmpty() || tahunMasuk.isEmpty()) {
            Toast.makeText(this, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        // Validasi tahun masuk
        if (tahunMasuk.length != 4 || !tahunMasuk.all { it.isDigit() }) {
            Toast.makeText(this, "Tahun harus 4 digit angka", Toast.LENGTH_SHORT).show()
            return
        }

        // Tampilkan ProgressDialog
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Menyimpan data siswa...")
            setCancelable(false)
        }
        progressDialog.show()

        // Buat request
        val request = TambahSiswaRequest(
            id_siswa = id,
            nama = nama,
            kelas = kelas,
            tahun_masuk = tahunMasuk,
            jenis_kelamin = jenisKelamin
        )

        // Kirim ke API
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Mengirim request tambah siswa: $request")
                val response = ApiClient.apiService.tambahSiswa(request)

                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()

                    if (response.isSuccessful) {
                        val result = response.body()

                        if (result != null) {
                            if (result.success) {
                                Toast.makeText(
                                    this@DashboardActivity,
                                    "✅ ${result.message}",
                                    Toast.LENGTH_LONG
                                ).show()

                                // Kembali ke halaman kelola siswa dan refresh data
                                Handler(Looper.getMainLooper()).postDelayed({
                                    showSiswa()
                                }, 1500)

                            } else {
                                val errorMsg = result.error ?: result.message
                                Toast.makeText(
                                    this@DashboardActivity,
                                    "❌ $errorMsg",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                this@DashboardActivity,
                                "❌ Tidak ada response dari server",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        val errorCode = response.code()
                        val errorBody = response.errorBody()?.string() ?: "No error body"

                        Log.e(TAG, "API Error: $errorCode - $errorBody")

                        Toast.makeText(
                            this@DashboardActivity,
                            "❌ Error $errorCode: ${response.message()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Log.e(TAG, "Error saving siswa: ${e.message}", e)

                    val errorMsg = when (e) {
                        is java.net.UnknownHostException -> "Tidak dapat terhubung ke server"
                        is java.net.SocketTimeoutException -> "Timeout, coba lagi"
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
    }

    private fun showLoadingSiswa() {
        progressBarSiswa.visibility = View.VISIBLE
        tableRowsContainer.visibility = View.GONE
        emptyStateLayout.visibility = View.GONE
    }

    private fun hideLoadingSiswa() {
        progressBarSiswa.visibility = View.GONE
        tableRowsContainer.visibility = View.VISIBLE
    }

    private fun showErrorState(errorMessage: String) {
        progressBarSiswa.visibility = View.GONE
        tableRowsContainer.visibility = View.GONE
        emptyStateLayout.visibility = View.VISIBLE

        // Update error message in empty state
        emptyStateLayout.findViewById<TextView>(R.id.tv_error_message).text = errorMessage
    }

    // ==================== FUNGSI DASHBOARD ====================
    private fun showDashboardContent() {
        fragmentContainer.removeAllViews()
        val dashboardView = layoutInflater.inflate(R.layout.activity_dashboard, null)
        fragmentContainer.addView(dashboardView)
        setupDashboardContent(dashboardView)
    }

    private fun setupDashboardContent(dashboardView: View) {
        Log.d(TAG, "setupDashboardContent called")

        try {
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

            tvLihatSemua.setOnClickListener {
                Toast.makeText(this, "Membuka semua tes", Toast.LENGTH_SHORT).show()
            }

            swipeRefresh.setOnRefreshListener {
                dashboardViewModel.fetchDashboardData()
            }

            btnRetry.setOnClickListener {
                dashboardViewModel.fetchDashboardData()
            }

            setupDashboardViewModelObservers(tvJumlahSiswa, tvJumlahGuru, tvJumlahTes,
                containerTesTerpopuler, swipeRefresh,
                progressBar, errorLayout, tvErrorMessage)

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
        dashboardViewModel.dashboardData.observe(this) { data ->
            swipeRefresh.isRefreshing = false
            progressBar.visibility = View.GONE
            errorLayout.visibility = View.GONE

            if (data != null) {
                tvJumlahSiswa.text = data.jumlahSiswa.toString()
                tvJumlahGuru.text = data.jumlahGuru.toString()
                tvJumlahTes.text = data.jumlahTes.toString()
                updateTesTerpopuler(containerTesTerpopuler, data.tesTerpopuler)
            }
        }

        dashboardViewModel.isLoading.observe(this) { isLoading ->
            if (isLoading && !swipeRefresh.isRefreshing) {
                progressBar.visibility = View.VISIBLE
                errorLayout.visibility = View.GONE
            }
        }

        dashboardViewModel.errorMessage.observe(this) { errorMessage ->
            swipeRefresh.isRefreshing = false
            if (errorMessage != null) {
                progressBar.visibility = View.GONE
                errorLayout.visibility = View.VISIBLE
                tvErrorMessage.text = errorMessage
            }
        }
    }

    private fun updateTesTerpopuler(
        container: LinearLayout,
        tesList: List<TesTerpopuler>
    ) {
        container.removeAllViews()

        if (tesList.isEmpty()) {
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

        tesList.forEachIndexed { index, tes ->
            try {
                val cardView = layoutInflater.inflate(
                    R.layout.item_tes_terpopuler,
                    container,
                    false
                ) as androidx.cardview.widget.CardView

                val tvNamaTes = cardView.findViewById<TextView>(R.id.tv_nama_tes)
                val tvJumlahSiswa = cardView.findViewById<TextView>(R.id.tv_jumlah_siswa)

                tvNamaTes.text = tes.namaTes
                tvJumlahSiswa.text = "Dikerjakan oleh ${tes.jumlahSiswa} siswa"

                container.addView(cardView)

                val layoutParams = cardView.layoutParams as ViewGroup.MarginLayoutParams
                layoutParams.bottomMargin = dpToPx(10)
                cardView.layoutParams = layoutParams

                cardView.setOnClickListener {
                    Toast.makeText(this, "Membuka: ${tes.namaTes}", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error creating card for ${tes.namaTes}: ${e.message}")
            }
        }
    }

    // ==================== FUNGSI KELOLA TES ====================
    private fun showTes() {
        fragmentContainer.removeAllViews()
        val tesView = layoutInflater.inflate(R.layout.kelolasoaltes, null)
        fragmentContainer.addView(tesView)

        selectedFileUri = null
        selectedFileName = null
        setupKelolaTesContent(tesView)
    }

    private fun showTambahTesForm() {
        fragmentContainer.removeAllViews()
        val tambahTesView = layoutInflater.inflate(R.layout.formtambahtes, null)
        fragmentContainer.addView(tambahTesView)

        navigationView.setCheckedItem(R.id.nav_tes)
        titleText.text = "Tambah Tes Baru"
        setupTambahTesForm(tambahTesView)
    }

    private fun showKelolaSoalTes() {
        fragmentContainer.removeAllViews()
        val kelolaSoalView = layoutInflater.inflate(R.layout.formkelolasoal, null)
        fragmentContainer.addView(kelolaSoalView)

        navigationView.setCheckedItem(R.id.nav_tes)
        titleText.text = "Kelola Tes BK"
        setupKelolaSoalTes(kelolaSoalView)
    }

    private fun setupKelolaSoalTes(kelolaSoalView: View) {
        Log.d(TAG, "setupKelolaSoalTes called")

        try {
            val tesRecyclerView = kelolaSoalView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.tesRecyclerView)
            val emptyStateLayout = kelolaSoalView.findViewById<LinearLayout>(R.id.emptyStateLayout)
            val loadingProgress = kelolaSoalView.findViewById<ProgressBar>(R.id.loadingProgress)
            val backButton = kelolaSoalView.findViewById<com.google.android.material.button.MaterialButton>(R.id.backButton)
            val titleTextView = kelolaSoalView.findViewById<TextView>(R.id.titleText)
            val subtitleTextView = kelolaSoalView.findViewById<TextView>(R.id.subtitleText)

            tesRecyclerView.layoutManager = LinearLayoutManager(this)
            tesRecyclerView.setHasFixedSize(true)

            backButton.setOnClickListener {
                showTes()
            }

            titleTextView.text = "Kelola Tes BK"
            subtitleTextView.text = "Ubah, hapus, atau aktifkan/nonaktifkan jenis tes yang tersedia."

            loadingProgress.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE

            fetchKelolaSoalData(tesRecyclerView, emptyStateLayout, loadingProgress)

        } catch (e: Exception) {
            Log.e(TAG, "Error in setupKelolaSoalTes: ${e.message}", e)
            Toast.makeText(this, "Error setup: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

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

                            Log.d(TAG, "Data kelola soal diterima:")
                            Log.d(TAG, "Total tes: ${data.totalTes}")
                            Log.d(TAG, "Total soal: ${data.totalSoal}")
                            Log.d(TAG, "Jumlah daftar tes: ${data.daftarTes.size}")

                            if (data.daftarTes.isNotEmpty()) {
                                val adapter = KelolaSoalAdapter(
                                    data.daftarTes,
                                    { tes ->
                                        showTesDetailDialog(tes)
                                    },
                                    { tes ->
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
                            emptyStateLayout.visibility = View.VISIBLE
                        }
                    } else {
                        val errorCode = response.code()
                        Toast.makeText(this@DashboardActivity,
                            "❌ Error $errorCode: ${response.message()}",
                            Toast.LENGTH_SHORT).show()
                        emptyStateLayout.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingProgress.visibility = View.GONE
                    Toast.makeText(this@DashboardActivity,
                        "❌ Network error: ${e.message}",
                        Toast.LENGTH_SHORT).show()
                    emptyStateLayout.visibility = View.VISIBLE
                }
            }
        }
    }

    /**
     * Menampilkan form edit tes
     */
    private fun showEditTesForm(tes: TesDetail) {
        Log.d(TAG, "showEditTesForm called for tes: ${tes.kategoriTes} (ID: ${tes.idTes})")
        currentTes = tes

        fragmentContainer.removeAllViews()
        val editTesView = layoutInflater.inflate(R.layout.formkelolates, null)
        fragmentContainer.addView(editTesView)

        navigationView.setCheckedItem(R.id.nav_tes)
        titleText.text = "Edit Tes: ${tes.kategoriTes}"

        setupEditTesForm(editTesView, tes)
    }

    /**
     * Setup form edit tes dengan RecyclerView
     */
    private fun setupEditTesForm(editTesView: View, tes: TesDetail) {
        Log.d(TAG, "setupEditTesForm called for tes: ${tes.kategoriTes} (ID: ${tes.idTes})")

        try {
            val headerText = editTesView.findViewById<TextView>(R.id.tvHeaderTes)
            val subtitleText = editTesView.findViewById<TextView>(R.id.tvSubtitleTes)
            val totalSoalText = editTesView.findViewById<TextView>(R.id.tvTotalSoalTes)
            val btnTambahSoal = editTesView.findViewById<Button>(R.id.btnTambahSoal)
            val btnBatal = editTesView.findViewById<Button>(R.id.btnBatal)
            val rvSoalTes = editTesView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvSoalTes)

            headerText.text = "Kelola Tes : ${tes.kategoriTes}"
            subtitleText.text = "Kelola soal-soal untuk tes ${tes.kategoriTes}"

            rvSoalTes.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
            rvSoalTes.setHasFixedSize(true)

            btnTambahSoal.setOnClickListener {
                showTambahSoalForm(tes.idTes, tes.kategoriTes)
            }

            btnBatal.setOnClickListener {
                showKelolaSoalTes()
            }

            val progressDialog = ProgressDialog(this).apply {
                setMessage("Memuat data soal...")
                setCancelable(false)
            }
            progressDialog.show()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d(TAG, "🔍 Mengambil data soal untuk tes ID: ${tes.idTes}")
                    val response = ApiClient.apiService.getSoalByTes(tes.idTes)

                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()

                        if (response.isSuccessful) {
                            val result = response.body()
                            if (result != null && result.success && result.data != null) {
                                val data = result.data
                                val soalList = data.soal_list ?: emptyList()

                                Log.d(TAG, "🎯 Jumlah soal diterima: ${soalList.size}")

                                totalSoalText.text = "Total ${data.jumlah_soal} soal untuk tes ini"

                                if (soalList.isNotEmpty()) {
                                    val adapter = SoalTesAdapter(
                                        soalList = soalList,
                                        onEditClick = { soal ->
                                            showEditSoalFormDialog(soal)
                                        },
                                        onDeleteClick = { soal ->
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
                                    Toast.makeText(
                                        this@DashboardActivity,
                                        "ℹ️ Belum ada soal untuk tes ini",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                val errorMsg = result?.error ?: "Data tidak valid"
                                Toast.makeText(
                                    this@DashboardActivity,
                                    "❌ $errorMsg",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Log.e(TAG, "❌ HTTP Error: ${response.code()} - ${response.message()}")
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

                        val errorMsg = when (e) {
                            is java.net.UnknownHostException -> "Tidak dapat terhubung ke server"
                            is java.net.SocketTimeoutException -> "Timeout. Server terlalu lama merespon."
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
     * FUNGSI BARU: Menampilkan form edit soal dengan opsi yang MUNCUL
     */
    private fun showEditSoalFormDialog(soal: SoalData) {
        try {
            // DEBUG: Tampilkan detail soal
            debugSoalData(soal)

            // Buat dialog
            val dialog = android.app.AlertDialog.Builder(this)
                .setTitle("Edit Soal #${soal.id_soal}")
                .setCancelable(false)
                .create()

            val dialogView = layoutInflater.inflate(R.layout.dialog_edit_soal, null)
            dialog.setView(dialogView)

            // Temukan views
            val tvEditSoal = dialogView.findViewById<TextView>(R.id.tvEditSoal)
            val iconClose = dialogView.findViewById<ImageView>(R.id.iconClose)
            val etPertanyaan = dialogView.findViewById<EditText>(R.id.etPertanyaan)
            val containerOpsi = dialogView.findViewById<LinearLayout>(R.id.containerOpsi)
            val btnSimpan = dialogView.findViewById<LinearLayout>(R.id.btnSimpan)
            val btnBatal = dialogView.findViewById<LinearLayout>(R.id.btnBatal)

            // Set judul
            tvEditSoal.text = "Edit Soal #${soal.id_soal}"

            // Set pertanyaan
            etPertanyaan.setText(soal.pertanyaan)

            // Kosongkan container
            containerOpsi.removeAllViews()

            // Tampilkan semua opsi dari API
            val opsiList = soal.opsi_list ?: emptyList()
            if (opsiList.isNotEmpty()) {
                Log.d(TAG, "🎨 Menampilkan ${opsiList.size} opsi")

                // Labels untuk opsi
                val labels = listOf("A", "B", "C", "D", "E", "F", "G", "H")

                for (i in opsiList.indices) {
                    val opsi = opsiList[i]
                    val label = if (i < labels.size) labels[i] else "${i + 1}"

                    try {
                        // EKSTRAK DATA DARI OPSI
                        var text = ""
                        var bobot = "1"

                        // Cara 1: Coba akses langsung jika modelnya OpsiData
                        if (opsi is com.example.login.models.OpsiData) {
                            text = opsi.opsi ?: ""
                            bobot = opsi.bobot?.toString() ?: "1"
                        }
                        // Cara 2: Coba akses dengan refleksi
                        else {
                            try {
                                val opsiField = opsi.javaClass.getDeclaredField("opsi")
                                opsiField.isAccessible = true
                                val opsiValue = opsiField.get(opsi)
                                text = opsiValue?.toString() ?: ""

                                val bobotField = opsi.javaClass.getDeclaredField("bobot")
                                bobotField.isAccessible = true
                                val bobotValue = bobotField.get(opsi)
                                bobot = bobotValue?.toString() ?: "1"
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error akses field: ${e.message}")
                                text = opsi.toString()
                            }
                        }

                        Log.d(TAG, "📝 Opsi $label: '$text' (bobot: $bobot)")

                        // Inflate layout
                        val opsiItemView = layoutInflater.inflate(R.layout.item_opsi_edit, containerOpsi, false)

                        // Temukan views
                        val tvLabel = opsiItemView.findViewById<TextView>(R.id.tvLabelOpsi)
                        val etOpsi = opsiItemView.findViewById<EditText>(R.id.etOpsi)
                        val etBobot = opsiItemView.findViewById<EditText>(R.id.etBobot)

                        // SET DATA KE UI
                        tvLabel.text = "Opsi $label"
                        etOpsi.setText(text)
                        etBobot.setText(bobot)

                        // Debug: verifikasi data sudah diset
                        Log.d(TAG, "   ✓ Set: ${tvLabel.text} = '${etOpsi.text}' (bobot: ${etBobot.text})")

                        // Tambahkan ke container
                        containerOpsi.addView(opsiItemView)

                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error menampilkan opsi $i: ${e.message}")
                    }
                }
            } else {
                // Jika tidak ada opsi
                val textView = TextView(this).apply {
                    text = "Tidak ada opsi jawaban"
                    setTextColor(Color.GRAY)
                    textSize = 14f
                    gravity = Gravity.CENTER
                    setPadding(0, 32, 0, 32)
                }
                containerOpsi.addView(textView)
                Log.w(TAG, "⚠️ Tidak ada opsi untuk ditampilkan")
            }

            // Setup tombol close
            iconClose.setOnClickListener { dialog.dismiss() }

            // Setup tombol batal
            btnBatal.setOnClickListener { dialog.dismiss() }

            // Setup tombol simpan - FUNGSI UTAMA
            btnSimpan.setOnClickListener {
                saveSoalToDatabase(
                    soalId = soal.id_soal,
                    originalSoal = soal,
                    etPertanyaan = etPertanyaan,
                    containerOpsi = containerOpsi,
                    dialog = dialog
                )
            }

            // Tampilkan dialog
            dialog.show()

            // Atur ukuran dialog
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            Log.d(TAG, "✅ Dialog edit soal berhasil ditampilkan")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error showEditSoalFormDialog: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * FUNGSI UTAMA: Simpan soal ke database via API - SESUAI FORMAT SERVER
     */
    private fun saveSoalToDatabase(
        soalId: Int,
        originalSoal: SoalData,
        etPertanyaan: EditText,
        containerOpsi: LinearLayout,
        dialog: android.app.AlertDialog
    ) {
        // 1. Validasi pertanyaan
        val pertanyaanBaru = etPertanyaan.text.toString().trim()
        if (pertanyaanBaru.isEmpty()) {
            etPertanyaan.error = "Pertanyaan tidak boleh kosong"
            etPertanyaan.requestFocus()
            return
        }

        // 2. Validasi semua opsi
        val opsiData = mutableListOf<Pair<String, Int>>()
        for (i in 0 until containerOpsi.childCount) {
            val child = containerOpsi.getChildAt(i)
            if (child is LinearLayout) {
                try {
                    val etOpsi = child.findViewById<EditText?>(R.id.etOpsi)
                    val etBobot = child.findViewById<EditText?>(R.id.etBobot)

                    if (etOpsi != null && etBobot != null) {
                        val opsiText = etOpsi.text.toString().trim()
                        val bobotText = etBobot.text.toString().trim()

                        if (opsiText.isEmpty()) {
                            etOpsi.error = "Opsi tidak boleh kosong"
                            etOpsi.requestFocus()
                            return
                        }

                        val bobot = bobotText.toIntOrNull() ?: 1
                        // Validasi bobot 1-5
                        val validatedBobot = if (bobot < 1 || bobot > 5) 1 else bobot
                        opsiData.add(Pair(opsiText, validatedBobot))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading opsi $i: ${e.message}")
                }
            }
        }

        // 3. Validasi minimal 2 opsi
        if (opsiData.size < 2) {
            Toast.makeText(this, "Minimal diperlukan 2 opsi jawaban", Toast.LENGTH_SHORT).show()
            return
        }

        // 4. Tampilkan progress dialog
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Menyimpan perubahan ke database...")
            setCancelable(false)
        }
        progressDialog.show()

        // 5. Siapkan data opsi_list sesuai format server
        val originalOpsiList = originalSoal.opsi_list ?: emptyList()
        val opsiList = mutableListOf<OpsiUpdateItem>()

        for (i in opsiData.indices) {
            val (opsiText, bobot) = opsiData[i]

            // CARI ID_OPSI DENGAN CARA YANG BENAR
            var idOpsi = 0

            try {
                if (i < originalOpsiList.size) {
                    val opsiObj = originalOpsiList[i]

                    // DEKLARASIKAN SEBAGAI OBJECT KOSONG DULU
                    Log.d(TAG, "🔍 Mencari id_opsi untuk opsi ke-$i")

                    // COBA 1: Jika OpsiData
                    if (opsiObj is com.example.login.models.OpsiData) {
                        idOpsi = opsiObj.id_opsi ?: 0
                        Log.d(TAG, "✅ id_opsi dari OpsiData: $idOpsi")
                    }
                    // COBA 2: Refleksi
                    else {
                        // Cari field yang mengandung 'id_opsi'
                        val fields = opsiObj.javaClass.declaredFields
                        for (field in fields) {
                            val fieldName = field.name.lowercase()
                            if (fieldName.contains("id_opsi") || fieldName.contains("id")) {
                                field.isAccessible = true
                                val value = field.get(opsiObj)
                                when (value) {
                                    is Int -> idOpsi = value
                                    is Number -> idOpsi = value.toInt()
                                    is String -> idOpsi = value.toIntOrNull() ?: 0
                                }
                                if (idOpsi > 0) {
                                    Log.d(TAG, "✅ id_opsi ditemukan di field '${field.name}': $idOpsi")
                                    break
                                }
                            }
                        }
                    }
                }

                // Jika masih 0, coba pendekatan lain
                if (idOpsi <= 0) {
                    // COBA 3: Ambil dari string representation
                    val opsiStr = originalOpsiList[i].toString()
                    if (opsiStr.contains("id_opsi")) {
                        val regex = """id_opsi[=:]\s*(\d+)""".toRegex()
                        val match = regex.find(opsiStr)
                        idOpsi = match?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error mencari id_opsi: ${e.message}")
            }

            // VALIDASI FINAL: id_opsi HARUS > 0
            if (idOpsi <= 0) {
                // Jika tidak ditemukan, kita perlu mendapatkan dari API atau memberi nilai default
                // Untuk sekarang, gunakan incremental sebagai fallback
                idOpsi = i + 1
                Log.w(TAG, "⚠️ id_opsi tidak ditemukan, menggunakan fallback: $idOpsi")

                // Tampilkan error jika tidak bisa mendapatkan id_opsi
                Toast.makeText(
                    this,
                    "⚠️ ID opsi tidak valid. Pastikan data soal sudah dimuat dengan benar.",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Buat OpsiUpdateItem sesuai format server
            opsiList.add(OpsiUpdateItem(
                id_opsi = idOpsi,
                opsi = opsiText,
                bobot = bobot
            ))

            Log.d(TAG, "📝 Opsi $i: id_opsi=$idOpsi, opsi='$opsiText', bobot=$bobot")
        }

        // 6. Buat request sesuai format server PHP
        val request = UpdateSoalCompleteRequest(
            id_soal = soalId,
            pertanyaan = pertanyaanBaru,
            opsi_list = opsiList
        )

        // 7. DEBUG: Tampilkan request JSON
        val gson = com.google.gson.Gson()
        val jsonRequest = gson.toJson(request)
        Log.d(TAG, "📋 Request JSON yang dikirim:")
        Log.d(TAG, jsonRequest)

        // 8. Kirim ke API
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "🚀 Memanggil API updateSoalComplete...")
                val response = ApiClient.apiService.updateSoalComplete(request)

                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()

                    Log.d(TAG, "📥 Response received. Code: ${response.code()}")
                    Log.d(TAG, "📥 Response message: ${response.message()}")

                    if (response.isSuccessful) {
                        val result = response.body()

                        // Debug response body
                        if (result != null) {
                            Log.d(TAG, "✅ Response body: success=${result.success}, message=${result.message}, error=${result.error}")
                        } else {
                            Log.e(TAG, "❌ Response body is null")
                        }

                        if (result != null) {
                            if (result.success) {
                                Log.d(TAG, "✅ API Success: ${result.message}")

                                dialog.dismiss()

                                Toast.makeText(
                                    this@DashboardActivity,
                                    "✅ ${result.message}",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Refresh halaman edit tes
                                currentTes?.let { tes ->
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        showEditTesForm(tes)
                                    }, 800)
                                }

                            } else {
                                val errorMsg = result.error ?: "Update gagal"
                                Log.e(TAG, "❌ API Error: $errorMsg")

                                // Tampilkan error yang lebih spesifik
                                val displayError = if (errorMsg.contains("id_opsi")) {
                                    "Error ID opsi: $errorMsg\nPastikan data sudah dimuat dengan benar."
                                } else {
                                    errorMsg
                                }

                                Toast.makeText(
                                    this@DashboardActivity,
                                    "❌ $displayError",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Log.e(TAG, "❌ Response body is null")
                            Toast.makeText(
                                this@DashboardActivity,
                                "❌ Tidak ada response dari server",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        val errorCode = response.code()
                        val errorBody = try {
                            response.errorBody()?.string() ?: "No error body"
                        } catch (e: Exception) {
                            "Cannot read error body"
                        }

                        Log.e(TAG, "❌ HTTP Error $errorCode: $errorBody")

                        // Parse error body untuk info lebih detail
                        val errorMessage = try {
                            val errorJson = com.google.gson.Gson().fromJson(errorBody, Map::class.java)
                            errorJson["error"]?.toString() ?: "Error $errorCode"
                        } catch (e: Exception) {
                            when (errorCode) {
                                400 -> "Bad Request: Data tidak sesuai format"
                                401 -> "Unauthorized"
                                404 -> "Endpoint tidak ditemukan"
                                500 -> "Server error"
                                else -> "Error $errorCode"
                            }
                        }

                        Toast.makeText(
                            this@DashboardActivity,
                            "❌ $errorMessage",
                            Toast.LENGTH_LONG
                        ).show()

                        // DEBUG: Tampilkan error body
                        Log.e(TAG, "❌ Error Body: $errorBody")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Log.e(TAG, "❌ Network error: ${e.message}", e)

                    val errorMsg = when (e) {
                        is java.net.UnknownHostException -> "Tidak dapat terhubung ke server"
                        is java.net.SocketTimeoutException -> "Timeout: Server terlalu lama merespon"
                        is javax.net.ssl.SSLHandshakeException -> "Error SSL/Koneksi aman"
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
    }

    /**
     * Fungsi debug untuk melihat struktur data soal
     */
    private fun debugSoalData(soal: SoalData) {
        Log.d(TAG, "=== DEBUG DETAIL SOAL ===")
        Log.d(TAG, "Soal ID: ${soal.id_soal}")
        Log.d(TAG, "Pertanyaan: ${soal.pertanyaan}")
        Log.d(TAG, "ID Tes: ${soal.id_tes}")
        Log.d(TAG, "opsi_list is null? ${soal.opsi_list == null}")

        val opsiList = soal.opsi_list
        if (opsiList != null) {
            Log.d(TAG, "Jumlah opsi: ${opsiList.size}")

            for (i in opsiList.indices) {
                val opsi = opsiList[i]
                Log.d(TAG, "--- Opsi $i ---")
                Log.d(TAG, "  Class: ${opsi.javaClass.name}")
                Log.d(TAG, "  SimpleName: ${opsi.javaClass.simpleName}")
                Log.d(TAG, "  toString(): $opsi")

                try {
                    val fields = opsi.javaClass.declaredFields
                    Log.d(TAG, "  Available fields: ${fields.map { it.name }}")

                    for (field in fields) {
                        try {
                            field.isAccessible = true
                            val value = field.get(opsi)
                            Log.d(TAG, "    ${field.name}: $value (Type: ${value?.javaClass?.simpleName})")
                        } catch (e: Exception) {
                            Log.d(TAG, "    ${field.name}: ERROR - ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "  Error in reflection: ${e.message}")
                }
            }
        } else {
            Log.d(TAG, "opsi_list is NULL")
        }
        Log.d(TAG, "=== END DEBUG ===")
    }

    /**
     * FUNGSI BARU: Menampilkan dialog konfirmasi hapus soal
     */
    private fun showDeleteSoalDialog(idSoal: Int, pertanyaan: String) {
        try {
            val pertanyaanSingkat = if (pertanyaan.length > 100) {
                "${pertanyaan.substring(0, 100)}..."
            } else {
                pertanyaan
            }

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Hapus Soal")
                .setMessage("Apakah Anda yakin ingin menghapus soal ini?\n\n\"$pertanyaanSingkat\"")
                .setPositiveButton("Ya, Hapus") { dialog, which ->
                    deleteSoal(idSoal, pertanyaan) // Panggil fungsi baru yang terhubung API
                }
                .setNegativeButton("Batal", null)
                .setIcon(ContextCompat.getDrawable(this, android.R.drawable.ic_delete))
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing delete dialog: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * FUNGSI BARU: Hapus soal dari server
     */
    private fun deleteSoal(idSoal: Int, pertanyaan: String) {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Menghapus soal...")
            setCancelable(false)
        }
        progressDialog.show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "🚀 Mengirim request hapus soal ID: $idSoal")

                val request = HapusSoalRequest(idSoal = idSoal)

                // Kirim request ke API
                val response = ApiClient.apiService.hapusSoal(request)

                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()

                    if (response.isSuccessful) {
                        val result = response.body()

                        if (result != null) {
                            if (result.success) {
                                Log.d(TAG, "✅ Soal berhasil dihapus: ${result.message}")

                                Toast.makeText(
                                    this@DashboardActivity,
                                    "✅ ${result.message}",
                                    Toast.LENGTH_LONG
                                ).show()

                                // Refresh data setelah hapus
                                currentTes?.let {
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        showEditTesForm(it)
                                    }, 800)
                                }

                            } else {
                                val errorMsg = result.error ?: "Gagal menghapus soal"
                                Log.e(TAG, "❌ Error: $errorMsg")

                                Toast.makeText(
                                    this@DashboardActivity,
                                    "❌ $errorMsg",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                this@DashboardActivity,
                                "❌ Tidak ada response dari server",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        val errorCode = response.code()
                        val errorBody = response.errorBody()?.string() ?: "No error body"

                        Log.e(TAG, "❌ HTTP Error $errorCode: $errorBody")

                        Toast.makeText(
                            this@DashboardActivity,
                            "❌ Error $errorCode: ${response.message()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Log.e(TAG, "❌ Network error: ${e.message}", e)

                    val errorMsg = when (e) {
                        is java.net.UnknownHostException -> "Tidak dapat terhubung ke server"
                        is java.net.SocketTimeoutException -> "Timeout: Server terlalu lama merespon"
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
    }

    /**
     * FUNGSI BARU: Menampilkan form tambah soal
     */
    private fun showTambahSoalForm(idTes: Int, namaTes: String) {
        Log.d(TAG, "showTambahSoalForm called for tes ID: $idTes - $namaTes")

        fragmentContainer.removeAllViews()
        val tambahSoalView = layoutInflater.inflate(R.layout.tambahsoal, null)
        fragmentContainer.addView(tambahSoalView)

        titleText.text = "Tambah Soal - $namaTes"
        setupTambahSoalForm(tambahSoalView, idTes, namaTes)
    }

    /**
     * FUNGSI BARU: Setup form tambah soal
     */
    private fun setupTambahSoalForm(tambahSoalView: View, idTes: Int, namaTes: String) {
        Log.d(TAG, "setupTambahSoalForm called for tes ID: $idTes")

        try {
            val tvHeader = tambahSoalView.findViewById<TextView>(R.id.tvHeader)
            val etPertanyaan = tambahSoalView.findViewById<EditText>(R.id.etPertanyaan)
            val containerOpsi = tambahSoalView.findViewById<LinearLayout>(R.id.containerOpsi)
            val btnTambahOpsi = tambahSoalView.findViewById<LinearLayout>(R.id.btnTambahOpsi)
            val btnBatal = tambahSoalView.findViewById<LinearLayout>(R.id.btnBatal)
            val btnSimpan = tambahSoalView.findViewById<LinearLayout>(R.id.btnSimpan)

            tvHeader.text = "Tambah Soal Baru - $namaTes"

            val opsiList = mutableListOf<Pair<EditText, EditText>>()
            setupInitialOpsi(tambahSoalView, opsiList)

            btnTambahOpsi.setOnClickListener {
                addNewOpsiOption(containerOpsi, opsiList)
            }

            btnBatal.setOnClickListener {
                currentTes?.let { tes ->
                    showEditTesForm(tes)
                }
            }

            btnSimpan.setOnClickListener {
                saveSoal(idTes, etPertanyaan, opsiList, namaTes)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in setupTambahSoalForm: ${e.message}", e)
            Toast.makeText(this, "Error setup form: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * FUNGSI BARU: Setup opsi awal
     */
    private fun setupInitialOpsi(view: View, opsiList: MutableList<Pair<EditText, EditText>>) {
        try {
            val etOpsi1 = view.findViewById<EditText>(R.id.etOpsi1)
            val etBobot1 = view.findViewById<EditText>(R.id.etBobot1)
            opsiList.add(Pair(etOpsi1, etBobot1))

            val etOpsi2 = view.findViewById<EditText>(R.id.etOpsi2)
            val etBobot2 = view.findViewById<EditText>(R.id.etBobot2)
            opsiList.add(Pair(etOpsi2, etBobot2))

            val btnHapus2 = view.findViewById<androidx.cardview.widget.CardView>(R.id.btnHapus2)
            btnHapus2.setOnClickListener {
                removeOpsiOption(etOpsi2, etBobot2, btnHapus2, opsiList)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in setupInitialOpsi: ${e.message}", e)
        }
    }

    /**
     * FUNGSI BARU: Tambah opsi baru
     */
    private fun addNewOpsiOption(container: LinearLayout, opsiList: MutableList<Pair<EditText, EditText>>) {
        try {
            val inflater = LayoutInflater.from(this)
            val opsiItem = inflater.inflate(R.layout.item_opsi_dinamis, container, false)

            val etOpsi = opsiItem.findViewById<EditText>(R.id.etOpsiDinamis)
            val etBobot = opsiItem.findViewById<EditText>(R.id.etBobotDinamis)
            val btnHapus = opsiItem.findViewById<androidx.cardview.widget.CardView>(R.id.btnHapusDinamis)

            etOpsi.hint = "Opsi jawaban"
            etBobot.hint = "1"
            etBobot.setText("1")

            btnHapus.setOnClickListener {
                removeOpsiOption(etOpsi, etBobot, btnHapus, opsiList)
                container.removeView(opsiItem)
            }

            container.addView(opsiItem)
            opsiList.add(Pair(etOpsi, etBobot))

            container.post {
                val scrollView = container.parent as? androidx.core.widget.NestedScrollView
                scrollView?.fullScroll(View.FOCUS_DOWN)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error adding new opsi: ${e.message}", e)
            Toast.makeText(this, "Gagal menambah opsi", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * FUNGSI BARU: Hapus opsi
     */
    private fun removeOpsiOption(
        etOpsi: EditText,
        etBobot: EditText,
        btnHapus: androidx.cardview.widget.CardView,
        opsiList: MutableList<Pair<EditText, EditText>>
    ) {
        val pairToRemove = opsiList.find { it.first == etOpsi && it.second == etBobot }
        opsiList.remove(pairToRemove)
    }

    /**
     * FUNGSI BARU: Simpan soal ke server
     */
    private fun saveSoal(
        idTes: Int,
        etPertanyaan: EditText,
        opsiList: List<Pair<EditText, EditText>>,
        namaTes: String
    ) {
        val pertanyaan = etPertanyaan.text.toString().trim()

        if (pertanyaan.isEmpty()) {
            etPertanyaan.error = "Pertanyaan tidak boleh kosong"
            etPertanyaan.requestFocus()
            return
        }

        val opsiData = mutableListOf<Pair<String, Int>>()
        for (pair in opsiList) {
            val opsiText = pair.first.text.toString().trim()
            val bobotText = pair.second.text.toString().trim()

            if (opsiText.isEmpty()) {
                pair.first.error = "Opsi tidak boleh kosong"
                pair.first.requestFocus()
                return
            }

            val bobot = bobotText.toIntOrNull() ?: 1
            opsiData.add(Pair(opsiText, bobot))
        }

        if (opsiData.size < 2) {
            Toast.makeText(this, "Minimal diperlukan 2 opsi jawaban", Toast.LENGTH_SHORT).show()
            return
        }

        val progressDialog = ProgressDialog(this).apply {
            setMessage("Menyimpan soal...")
            setCancelable(false)
        }
        progressDialog.show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = TambahSoalRequest(
                    id_tes = idTes,
                    pertanyaan = pertanyaan,
                    opsi = opsiData.map { it.first },
                    bobot = opsiData.map { it.second }
                )

                Log.d(TAG, "Mengirim request tambah soal:")
                Log.d(TAG, "ID Tes: $idTes")
                Log.d(TAG, "Pertanyaan: $pertanyaan")
                Log.d(TAG, "Jumlah opsi: ${opsiData.size}")

                val response = ApiClient.apiService.tambahSoal(request)

                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()

                    if (response.isSuccessful) {
                        val result = response.body()
                        Log.d(TAG, "API Response: $result")

                        if (result != null) {
                            if (result.success) {
                                val idSoalBaru = result.data?.id_soal ?: 0
                                Toast.makeText(
                                    this@DashboardActivity,
                                    "✅ ${result.message}",
                                    Toast.LENGTH_LONG
                                ).show()

                                Handler(Looper.getMainLooper()).postDelayed({
                                    currentTes?.let {
                                        showEditTesForm(it)
                                    }
                                }, 1500)

                            } else {
                                val errorMsg = if (!result.error.isNullOrEmpty()) {
                                    "${result.message}: ${result.error}"
                                } else {
                                    result.message
                                }
                                Toast.makeText(
                                    this@DashboardActivity,
                                    "❌ $errorMsg",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                this@DashboardActivity,
                                "❌ Tidak ada response dari server",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        val errorCode = response.code()
                        val errorBody = response.errorBody()?.string() ?: "No error body"

                        Log.e(TAG, "API Error: $errorCode - $errorBody")

                        Toast.makeText(
                            this@DashboardActivity,
                            "❌ Error $errorCode: ${response.message()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Log.e(TAG, "Error saving soal: ${e.message}", e)

                    val errorMsg = when (e) {
                        is java.net.UnknownHostException -> "Tidak dapat terhubung ke server"
                        is java.net.SocketTimeoutException -> "Timeout, coba lagi"
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
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(dateString)

            val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            outputFormat.format(date)
        } catch (e: Exception) {
            dateString
        }
    }

    /**
     * Setup form tambah tes dengan semua fungsi dan listeners
     */
    private fun setupTambahTesForm(tambahTesView: View) {
        Log.d(TAG, "setupTambahTesForm called")

        try {
            val etNamaTesBaru = tambahTesView.findViewById<EditText>(R.id.et_nama_tes_baru)
            val etDeskripsiTes = tambahTesView.findViewById<EditText>(R.id.et_deskripsi_tes)
            val tvFileStatus = tambahTesView.findViewById<TextView>(R.id.tv_file_status)
            val btnBrowse = tambahTesView.findViewById<Button>(R.id.btn_browse)
            val btnBatal = tambahTesView.findViewById<Button>(R.id.btn_batal)
            val btnSimpan = tambahTesView.findViewById<Button>(R.id.btn_simpan)

            selectedFileUri = null
            selectedFileName = null
            tvFileStatus.text = "[Pilih file CSV]"
            tvFileStatus.setTextColor(Color.parseColor("#888888"))

            btnBrowse.setOnClickListener {
                Log.d(TAG, "Browse button clicked")
                openFilePicker()
            }

            btnBatal.setOnClickListener {
                showTes()
            }

            btnSimpan.setOnClickListener {
                Log.d(TAG, "Simpan button clicked")

                val namaTes = etNamaTesBaru.text.toString().trim()
                val deskripsi = etDeskripsiTes.text.toString().trim()

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

                val progressDialog = ProgressDialog(this).apply {
                    setMessage("Mengupload tes ke server...")
                    setCancelable(false)
                }
                progressDialog.show()

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

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val request = TambahTesRequest(
                                nama_tes = namaTes,
                                deskripsi_tes = deskripsi,
                                csv_content = csvContent
                            )

                            Log.d(TAG, "Mengirim request ke API...")
                            val response = ApiClient.apiService.tambahTes(request)

                            withContext(Dispatchers.Main) {
                                progressDialog.dismiss()

                                if (response.isSuccessful) {
                                    val result = response.body()
                                    Log.d(TAG, "API Response Status: ${response.code()}")

                                    if (result != null) {
                                        if (result.status == "success") {
                                            Toast.makeText(
                                                this@DashboardActivity,
                                                "✅ ${result.message}\nID Tes: ${result.tes_id}",
                                                Toast.LENGTH_LONG
                                            ).show()

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
                                Toast.makeText(
                                    this@DashboardActivity,
                                    "❌ Timeout: Koneksi ke server terlalu lama",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } catch (e: java.net.UnknownHostException) {
                            withContext(Dispatchers.Main) {
                                progressDialog.dismiss()
                                Toast.makeText(
                                    this@DashboardActivity,
                                    "❌ Tidak dapat terhubung ke server. Cek koneksi internet",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                progressDialog.dismiss()
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
            val tvTotalSoal = tesView.findViewById<TextView>(R.id.tvTotalSoal)
            val tvJenisTes = tesView.findViewById<TextView>(R.id.tvJenisTes)
            val kelolaTesBKLayout = tesView.findViewById<LinearLayout>(R.id.kelolaTesBKLayout)
            val tambahTesBaruLayout = tesView.findViewById<LinearLayout>(R.id.tambahTesBaruLayout)
            val rvDaftarTes = tesView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvDaftarTes)
            val progressBarInitial = tesView.findViewById<ProgressBar>(R.id.progressBarInitial)
            val mainLayout = tesView.findViewById<LinearLayout>(R.id.kelolaTesMainLayout)
            val tvErrorTes = tesView.findViewById<TextView>(R.id.tvErrorTes)
            val swipeRefreshTes = tesView.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshTes)
            val tvTambahTesBaru = tesView.findViewById<TextView>(R.id.tvTambahTesBaru)
            val tvKelolaTesBK = tesView.findViewById<TextView>(R.id.tvKelolaTesBK)

            rvDaftarTes.layoutManager = LinearLayoutManager(this)
            rvDaftarTes.setHasFixedSize(true)

            swipeRefreshTes.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
            )

            swipeRefreshTes.setOnRefreshListener {
                kelolaTesViewModel.fetchKelolaTesData()
            }

            kelolaTesBKLayout.setOnClickListener {
                showKelolaSoalTes()
            }

            tvKelolaTesBK?.setOnClickListener {
                showKelolaSoalTes()
            }

            tambahTesBaruLayout.setOnClickListener {
                showTambahTesForm()
            }

            tvTambahTesBaru.setOnClickListener {
                showTambahTesForm()
            }

            kelolaTesViewModel.kelolaTesData.observe(this) { response ->
                swipeRefreshTes.isRefreshing = false
                progressBarInitial.visibility = View.GONE

                if (response != null && response.success && response.data != null) {
                    val data = response.data

                    mainLayout.visibility = View.VISIBLE
                    tvErrorTes.visibility = View.GONE

                    tvTotalSoal.text = data.totalSoal.toString()
                    tvJenisTes.text = data.jenisTes.toString()

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
                    mainLayout.visibility = View.VISIBLE
                    tvErrorTes.text = "Error: ${response.message}"
                    tvErrorTes.visibility = View.VISIBLE

                    tvTotalSoal.text = "0"
                    tvJenisTes.text = "0"
                }
            }

            kelolaTesViewModel.isLoading.observe(this) { isLoading ->
                if (isLoading && !swipeRefreshTes.isRefreshing) {
                    progressBarInitial.visibility = View.VISIBLE
                    mainLayout.visibility = View.GONE
                }

                swipeRefreshTes.isEnabled = !isLoading
            }

            kelolaTesViewModel.errorMessage.observe(this) { errorMessage ->
                swipeRefreshTes.isRefreshing = false
                progressBarInitial.visibility = View.GONE

                if (errorMessage != null) {
                    mainLayout.visibility = View.VISIBLE
                    tvErrorTes.text = "Error: $errorMessage"
                    tvErrorTes.visibility = View.VISIBLE

                    tvTotalSoal.text = "0"
                    tvJenisTes.text = "0"
                }
            }

            progressBarInitial.visibility = View.VISIBLE
            mainLayout.visibility = View.GONE

            kelolaTesViewModel.fetchKelolaTesData()

        } catch (e: Exception) {
            Log.e(TAG, "Error in setupKelolaTesContent: ${e.message}", e)
            Toast.makeText(this, "Error setup: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showGuru() {
        fragmentContainer.removeAllViews()
        val guruView = layoutInflater.inflate(R.layout.kelolaguru, null)
        fragmentContainer.addView(guruView)

        navigationView.setCheckedItem(R.id.nav_guru)
        titleText.text = "Kelola Guru"

        setupKelolaGuruContent(guruView)

        Toast.makeText(this, "Kelola Data Guru", Toast.LENGTH_SHORT).show()
    }

    private fun setupBackPressedHandler() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else if (titleText.text.toString().startsWith("Tambah Tes Baru")) {
                    showTes()
                } else if (titleText.text.toString().startsWith("Kelola Tes BK")) {
                    showTes()
                } else if (titleText.text.toString().startsWith("Edit Tes:")) {
                    showKelolaSoalTes()
                } else if (titleText.text.toString().startsWith("Tambah Soal -")) {
                    currentTes?.let {
                        showEditTesForm(it)
                    }
                } else if (titleText.text.toString() == "Kelola Tes") {
                    if (System.currentTimeMillis() - backPressedTime < 2000) {
                        finish()
                    } else {
                        Toast.makeText(this@DashboardActivity,
                            "Tekan kembali sekali lagi untuk keluar",
                            Toast.LENGTH_SHORT).show()
                        backPressedTime = System.currentTimeMillis()
                    }
                } else {
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
     * Setup konten kelola guru dengan data dari API
     */
    private fun setupKelolaGuruContent(guruView: View) {
        Log.d(TAG, "setupKelolaGuruContent called")

        try {
            val tvJumlahGuru = guruView.findViewById<TextView>(R.id.tv_jumlah_guru)
            val tvAkunAktif = guruView.findViewById<TextView>(R.id.tv_akun_aktif)
            val tvAkunNonaktif = guruView.findViewById<TextView>(R.id.tv_akun_nonaktif)
            val btnTambahGuru = guruView.findViewById<Button>(R.id.btn_tambah_guru)
            val rvGuru = guruView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvGuru)
            val emptyStateLayout = guruView.findViewById<LinearLayout>(R.id.emptyStateLayout)
            val loadingProgress = guruView.findViewById<ProgressBar>(R.id.loadingProgress)
            val tvInfoJumlah = guruView.findViewById<TextView>(R.id.tvInfoJumlah)

            rvGuru.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
            rvGuru.setHasFixedSize(true)

            val adapter = GuruAdapter(emptyList()) { guru ->
                showGuruDetailDialog(guru)
            }
            rvGuru.adapter = adapter

            btnTambahGuru?.setOnClickListener {
                showTambahGuruForm()
            }

            loadingProgress.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
            rvGuru.visibility = View.VISIBLE

            fetchDataGuru(
                tvJumlahGuru,
                tvAkunAktif,
                tvAkunNonaktif,
                adapter,
                emptyStateLayout,
                loadingProgress,
                rvGuru,
                tvInfoJumlah
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error in setupKelolaGuruContent: ${e.message}", e)
            Toast.makeText(this, "Error setup: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Dialog custom untuk detail guru dengan tombol aksi
     */
    private fun showGuruDetailDialog(guru: Guru) {
        try {
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.dialog_detail_guru)
            dialog.setCancelable(true)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.setDimAmount(0.7f)

            val tvNama = dialog.findViewById<TextView>(R.id.tvNama)
            val tvTelepon = dialog.findViewById<TextView>(R.id.tvTelepon)
            val tvAlamat = dialog.findViewById<TextView>(R.id.tvAlamat)
            val tvStatus = dialog.findViewById<TextView>(R.id.tvStatus)
            val tvUsername = dialog.findViewById<TextView>(R.id.tvUsername)
            val tvEmail = dialog.findViewById<TextView>(R.id.tvEmail)
            val btnEdit = dialog.findViewById<Button>(R.id.btnEdit)
            val btnHapus = dialog.findViewById<Button>(R.id.btnHapus)
            val btnStatus = dialog.findViewById<Button>(R.id.btnStatus)
            val btnClose = dialog.findViewById<ImageView>(R.id.btnClose)

            tvNama.text = guru.nama
            tvTelepon.text = guru.telepon
            tvAlamat.text = guru.alamat
            tvUsername.text = guru.username ?: "-"
            tvEmail.text = guru.email ?: "-"

            val statusText = if (guru.status == "Aktif") "Aktif" else "Nonaktif"
            tvStatus.text = statusText

            if (guru.status == "Aktif") {
                tvStatus.setBackgroundResource(R.drawable.bg_status_aktif)
                tvStatus.setTextColor(Color.WHITE)
                btnStatus.text = "⛔ Nonaktifkan"
                btnStatus.setBackgroundResource(R.drawable.buttonbatal)
            } else {
                tvStatus.setBackgroundResource(R.drawable.bg_status_nonaktif)
                tvStatus.setTextColor(Color.WHITE)
                btnStatus.text = "✅ Aktifkan"
                btnStatus.setBackgroundResource(R.drawable.button_green)
            }

            btnEdit.setOnClickListener {
                dialog.dismiss() // Tutup dialog
                showEditGuruForm(guru) // Panggil fungsi edit
            }

            btnHapus.setOnClickListener {
                dialog.dismiss()
                showDeleteConfirmationDialog(guru)
            }

            btnStatus.setOnClickListener {
                val currentStatus = if (guru.status == "Aktif") "Aktif" else "Nonaktif"
                val newStatus = if (guru.status == "Aktif") "Nonaktif" else "Aktif"
                val actionText = if (guru.status == "Aktif") "menonaktifkan" else "mengaktifkan"

                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Konfirmasi Ubah Status")
                    .setMessage(
                        """
                    Apakah Anda yakin ingin $actionText akun ini?
                    
                    👤 ${guru.nama}
                    📞 ${guru.telepon}
                    
                    Status: $currentStatus → $newStatus
                    
                    Guru tidak dapat login jika status Nonaktif.
                    """.trimIndent()
                    )
                    .setPositiveButton("Ya, Ubah Status") { confirmDialog, _ ->
                        performUpdateStatus(guru.idGuru, guru.nama, currentStatus, newStatus, dialog)
                        confirmDialog.dismiss()
                    }
                    .setNegativeButton("Batal") { confirmDialog, _ ->
                        confirmDialog.dismiss()
                    }
                    .show()
            }

            btnClose.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()

            val displayMetrics = resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.9).toInt()
            val height = ViewGroup.LayoutParams.WRAP_CONTENT

            dialog.window?.setLayout(width, height)
            dialog.window?.setGravity(Gravity.CENTER)

        } catch (e: Exception) {
            Log.e(TAG, "Error showing guru detail dialog: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Fungsi untuk melakukan update status ke server
     */
    private fun performUpdateStatus(
        idGuru: Int,
        namaGuru: String,
        currentStatus: String,
        newStatus: String,  // Parameter ini hanya untuk UI, tidak dikirim ke server
        dialog: Dialog? = null
    ) {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Mengubah status akun...")
            setCancelable(false)
        }
        progressDialog.show()

        // Buat request sesuai dengan format yang diharapkan server
        // HANYA kirim id_guru, server akan otomatis membalik status
        val request = UpdateStatusGuruRequest(
            id_guru = idGuru
            // action = "ubah_status" akan otomatis dari default value
        )

        // Debug: lihat request yang dikirim
        val gson = com.google.gson.Gson()
        val jsonRequest = gson.toJson(request)
        Log.d(TAG, "📋 Request ke API: $jsonRequest")
        Log.d(TAG, "📋 Server akan otomatis mengubah status dari '$currentStatus' ke kebalikannya")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Mengirim request update status guru...")
                Log.d(TAG, "ID Guru: $idGuru, Nama: $namaGuru")

                val response = ApiClient.apiService.updateStatusGuru(request)

                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()

                    if (response.isSuccessful) {
                        val result = response.body()

                        // Debug: lihat response dari server
                        Log.d(TAG, "📥 Response Code: ${response.code()}")
                        Log.d(TAG, "📥 Response: $result")

                        if (result != null) {
                            // Perhatikan: API Anda mengembalikan "status" bukan "success"
                            if (result.status == "success") {
                                val message = result.message ?: "Status guru berhasil diubah"

                                Toast.makeText(
                                    this@DashboardActivity,
                                    "✅ $message",
                                    Toast.LENGTH_LONG
                                ).show()

                                Log.d(TAG, "Status guru berhasil diubah: $namaGuru")

                                dialog?.dismiss()

                                // Refresh data guru setelah 1 detik
                                Handler(Looper.getMainLooper()).postDelayed({
                                    refreshGuruData()
                                }, 1000)

                            } else {
                                val errorMsg = result.message ?: "Gagal mengubah status"
                                Toast.makeText(
                                    this@DashboardActivity,
                                    "❌ $errorMsg",
                                    Toast.LENGTH_LONG
                                ).show()

                                Log.e(TAG, "API Error Response: $errorMsg")
                            }
                        } else {
                            Toast.makeText(
                                this@DashboardActivity,
                                "❌ Tidak ada response dari server",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        val errorCode = response.code()
                        val errorBody = try {
                            response.errorBody()?.string() ?: "No error body"
                        } catch (e: Exception) {
                            "Cannot read error body"
                        }

                        Log.e(TAG, "❌ API Error: $errorCode - $errorBody")

                        // Parse error body
                        try {
                            val errorJson = com.google.gson.Gson().fromJson(errorBody, Map::class.java)
                            val errorMsg = errorJson["message"]?.toString() ?: "Error $errorCode"
                            Toast.makeText(
                                this@DashboardActivity,
                                "❌ $errorMsg",
                                Toast.LENGTH_LONG
                            ).show()
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@DashboardActivity,
                                "❌ Error $errorCode: ${response.message()}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Log.e(TAG, "❌ Error updating status guru: ${e.message}", e)

                    val errorMsg = when (e) {
                        is java.net.UnknownHostException -> "Tidak dapat terhubung ke server"
                        is java.net.SocketTimeoutException -> "Timeout, coba lagi"
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
    }
    /**
     * Refresh data guru setelah update
     */
    private fun refreshGuruData() {
        try {
            // Periksa apakah sedang di halaman kelola guru
            if (titleText.text.toString() == "Kelola Guru") {
                // Cari view yang sesuai
                val currentView = fragmentContainer.getChildAt(0)
                if (currentView != null) {
                    val tvJumlahGuru = currentView.findViewById<TextView>(R.id.tv_jumlah_guru)
                    if (tvJumlahGuru != null) {
                        // Setup ulang konten guru
                        setupKelolaGuruContent(currentView)
                        Log.d(TAG, "Data guru berhasil diperbarui")
                    } else {
                        // Jika tidak ditemukan, panggil showGuru() untuk refresh
                        showGuru()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing guru data: ${e.message}")
            // Fallback: panggil showGuru()
            showGuru()
        }
    }
    /**
     * Fungsi untuk mengambil data guru dari API - TANPA BATASAN 5 DATA
     */
    private fun fetchDataGuru(
        tvJumlahGuru: TextView,
        tvAkunAktif: TextView,
        tvAkunNonaktif: TextView,
        adapter: GuruAdapter,
        emptyStateLayout: LinearLayout,
        loadingProgress: ProgressBar,
        rvGuru: androidx.recyclerview.widget.RecyclerView,
        tvInfoJumlah: TextView
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Mengambil SEMUA data guru dari API...")

                val response = ApiClient.apiService.getDataGuru()
                Log.d(TAG, "Response code: ${response.code()}")

                withContext(Dispatchers.Main) {
                    loadingProgress.visibility = View.GONE

                    if (response.isSuccessful) {
                        val result = response.body()
                        Log.d(TAG, "Response body ada: ${result != null}")

                        if (result != null && result.success) {
                            val data = result.data

                            Log.d(TAG, "Data guru diterima:")
                            Log.d(TAG, "Total guru: ${data.statistik.totalGuru}")
                            Log.d(TAG, "Aktif: ${data.statistik.akunAktif}")
                            Log.d(TAG, "Nonaktif: ${data.statistik.akunNonaktif}")
                            Log.d(TAG, "Jumlah daftar guru: ${data.daftarGuru.size}")

                            tvJumlahGuru.text = data.statistik.totalGuru.toString()
                            tvAkunAktif.text = data.statistik.akunAktif.toString()
                            tvAkunNonaktif.text = data.statistik.akunNonaktif.toString()

                            val daftarGuru = data.daftarGuru

                            if (daftarGuru.isNotEmpty()) {
                                adapter.updateData(daftarGuru)
                                emptyStateLayout.visibility = View.GONE
                                rvGuru.visibility = View.VISIBLE

                                tvInfoJumlah.text = "Menampilkan SEMUA ${data.daftarGuru.size} data guru"

                                Log.d(TAG, "✓ Menampilkan ${daftarGuru.size} data guru (SEMUA DATA)")
                            } else {
                                emptyStateLayout.visibility = View.VISIBLE
                                rvGuru.visibility = View.GONE
                                tvInfoJumlah.text = "Tidak ada data guru"
                            }

                            Toast.makeText(
                                this@DashboardActivity,
                                "✅ ${daftarGuru.size} data guru dimuat",
                                Toast.LENGTH_SHORT
                            ).show()

                        } else {
                            val errorMsg = result?.message ?: "Data tidak valid"

                            emptyStateLayout.visibility = View.VISIBLE
                            rvGuru.visibility = View.GONE
                            tvInfoJumlah.text = "Error memuat data"

                            tvJumlahGuru.text = "0"
                            tvAkunAktif.text = "0"
                            tvAkunNonaktif.text = "0"

                            Toast.makeText(
                                this@DashboardActivity,
                                "❌ $errorMsg",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        val errorCode = response.code()

                        emptyStateLayout.visibility = View.VISIBLE
                        rvGuru.visibility = View.GONE
                        tvInfoJumlah.text = "Error memuat data"

                        tvJumlahGuru.text = "0"
                        tvAkunAktif.text = "0"
                        tvAkunNonaktif.text = "0"

                        Toast.makeText(
                            this@DashboardActivity,
                            "❌ Error $errorCode: ${response.message()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingProgress.visibility = View.GONE

                    emptyStateLayout.visibility = View.VISIBLE
                    rvGuru.visibility = View.GONE
                    tvInfoJumlah.text = "Error koneksi"

                    tvJumlahGuru.text = "0"
                    tvAkunAktif.text = "0"
                    tvAkunNonaktif.text = "0"

                    Toast.makeText(
                        this@DashboardActivity,
                        "❌ Network error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    /**
     * FUNGSI BARU: Menampilkan form tambah guru
     */
    private fun showTambahGuruForm() {
        Log.d(TAG, "showTambahGuruForm called")

        fragmentContainer.removeAllViews()
        val tambahGuruView = layoutInflater.inflate(R.layout.formkelolaguru, null)
        fragmentContainer.addView(tambahGuruView)

        navigationView.setCheckedItem(R.id.nav_guru)
        titleText.text = "Tambah Data Guru"

        setupTambahGuruForm(tambahGuruView, isEditMode = false, guru = null)
    }

    /**
     * FUNGSI BARU: Menampilkan form edit guru
     */
    private fun showEditGuruForm(guru: Guru) {
        Log.d(TAG, "showEditGuruForm called for guru: ${guru.nama} (ID: ${guru.idGuru})")

        fragmentContainer.removeAllViews()
        val editGuruView = layoutInflater.inflate(R.layout.formkelolaguru, null)
        fragmentContainer.addView(editGuruView)

        navigationView.setCheckedItem(R.id.nav_guru)
        titleText.text = "Edit Data Guru"

        setupTambahGuruForm(editGuruView, isEditMode = true, guru = guru)
    }

    /**
     * FUNGSI UTAMA: Setup form guru (untuk tambah dan edit)
     */
    private fun setupTambahGuruForm(guruView: View, isEditMode: Boolean = false, guru: Guru? = null) {
        Log.d(TAG, "setupTambahGuruForm called - isEditMode: $isEditMode, guru: ${guru?.nama}")

        try {
            val tvHeader = guruView.findViewById<TextView>(R.id.tvHeader)
            val btnBatal = guruView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBatal)
            val btnSimpan = guruView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSimpan)
            val etNama = guruView.findViewById<EditText>(R.id.etNamaGuru)
            val etTelepon = guruView.findViewById<EditText>(R.id.etTeleponGuru)
            val etAlamat = guruView.findViewById<EditText>(R.id.etAlamatGuru)

            // Setup berdasarkan mode
            if (isEditMode && guru != null) {
                // MODE EDIT
                tvHeader.text = "Edit Data Guru"
                btnSimpan.text = "Update"

                // Isi data guru ke form
                etNama.setText(guru.nama)
                etTelepon.setText(guru.telepon)
                etAlamat.setText(guru.alamat)

            } else {
                // MODE TAMBAH
                tvHeader.text = "Tambah Data Guru"
                btnSimpan.text = "Simpan"
            }

            // Setup tombol Batal
            btnBatal.setOnClickListener {
                Log.d(TAG, "Tombol Batal diklik")
                showGuru() // Kembali ke halaman kelola guru
            }

            // Setup tombol Simpan/Update
            btnSimpan.setOnClickListener {
                Log.d(TAG, "Tombol ${btnSimpan.text} diklik")

                // Validasi input
                val nama = etNama.text.toString().trim()
                val telepon = etTelepon.text.toString().trim()
                val alamat = etAlamat.text.toString().trim()

                // Reset error
                etNama.error = null
                etTelepon.error = null
                etAlamat.error = null

                var hasError = false

                // Validasi Nama
                if (nama.isEmpty()) {
                    etNama.error = "Nama tidak boleh kosong"
                    etNama.requestFocus()
                    hasError = true
                } else if (nama.length < 3) {
                    etNama.error = "Nama minimal 3 karakter"
                    etNama.requestFocus()
                    hasError = true
                }

                // Validasi Telepon
                if (telepon.isEmpty()) {
                    etTelepon.error = "Telepon tidak boleh kosong"
                    if (!hasError) {
                        etTelepon.requestFocus()
                        hasError = true
                    }
                } else {
                    // Validasi hanya angka
                    val cleanTelepon = telepon.replace("[^0-9]".toRegex(), "")
                    if (cleanTelepon.length < 10 || cleanTelepon.length > 15) {
                        etTelepon.error = "Telepon harus 10-15 digit angka"
                        if (!hasError) {
                            etTelepon.requestFocus()
                            hasError = true
                        }
                    }
                }

                // Validasi Alamat
                if (alamat.isEmpty()) {
                    etAlamat.error = "Alamat tidak boleh kosong"
                    if (!hasError) {
                        etAlamat.requestFocus()
                        hasError = true
                    }
                } else if (alamat.length < 5) {
                    etAlamat.error = "Alamat terlalu pendek"
                    if (!hasError) {
                        etAlamat.requestFocus()
                        hasError = true
                    }
                }

                if (hasError) {
                    return@setOnClickListener
                }

                // Bersihkan telepon dari karakter non-angka
                val cleanTelepon = telepon.replace("[^0-9]".toRegex(), "")

                if (isEditMode && guru != null) {
                    // MODE EDIT: Panggil fungsi update (TANPA STATUS)
                    updateGuruInDatabase(guru.idGuru, nama, cleanTelepon, alamat)
                } else {
                    // MODE TAMBAH: Panggil fungsi simpan
                    saveGuruToDatabase(nama, cleanTelepon, alamat)
                }
            }

            // Fokus ke field nama
            if (!isEditMode) {
                etNama.requestFocus()
            }

            Log.d(TAG, "Form guru berhasil di-setup (Mode: ${if (isEditMode) "Edit" else "Tambah"})")

        } catch (e: Exception) {
            Log.e(TAG, "Error in setupTambahGuruForm: ${e.message}", e)
            Toast.makeText(this, "Gagal memuat form: ${e.message}", Toast.LENGTH_SHORT).show()
            showGuru() // Kembali ke halaman guru
        }
    }


    /**
     * FUNGSI BARU: Menyimpan data guru baru ke database via API
     */
    private fun saveGuruToDatabase(nama: String, telepon: String, alamat: String) {
        // Tampilkan ProgressDialog
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Menyimpan data guru...")
            setCancelable(false)
        }
        progressDialog.show()

        // Buat request
        val request = TambahGuruRequest(
            nama = nama,
            telepon = telepon,
            alamat = alamat
        )

        Log.d(TAG, "Request untuk API: nama=$nama, telepon=$telepon, alamat=$alamat")

        // Kirim ke API
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Mengirim request tambah guru...")
                val response = ApiClient.apiService.tambahGuru(request)

                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()

                    if (response.isSuccessful) {
                        val result = response.body()

                        if (result != null) {
                            if (result.success) {
                                Toast.makeText(
                                    this@DashboardActivity,
                                    "✅ ${result.message}",
                                    Toast.LENGTH_LONG
                                ).show()

                                Log.d(TAG, "Data guru berhasil disimpan: ${result.data}")

                                // Kembali ke halaman kelola guru dan refresh data
                                Handler(Looper.getMainLooper()).postDelayed({
                                    showGuru()
                                }, 1500)

                            } else {
                                val errorMsg = result.error ?: "Gagal menyimpan data"
                                Toast.makeText(
                                    this@DashboardActivity,
                                    "❌ $errorMsg",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                this@DashboardActivity,
                                "❌ Tidak ada response dari server",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        val errorCode = response.code()
                        val errorBody = response.errorBody()?.string() ?: "No error body"

                        Log.e(TAG, "API Error: $errorCode - $errorBody")

                        Toast.makeText(
                            this@DashboardActivity,
                            "❌ Error $errorCode: ${response.message()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Log.e(TAG, "Error saving guru: ${e.message}", e)

                    val errorMsg = when (e) {
                        is java.net.UnknownHostException -> "Tidak dapat terhubung ke server"
                        is java.net.SocketTimeoutException -> "Timeout, coba lagi"
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
    }

    /**
     * FUNGSI BARU: Update data guru ke database via API
     */
    private fun updateGuruInDatabase(idGuru: Int, nama: String, telepon: String, alamat: String) {
        // Tampilkan ProgressDialog
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Memperbarui data guru...")
            setCancelable(false)
        }
        progressDialog.show()

        // Buat request update TANPA STATUS
        val request = UpdateGuruRequest(
            id_guru = idGuru,
            nama = nama,
            telepon = telepon,
            alamat = alamat
        )

        Log.d(TAG, "Request untuk update guru: ID=$idGuru, nama=$nama")

        // Kirim ke API
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Mengirim request update guru...")
                val response = ApiClient.apiService.updateGuru(request)

                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()

                    if (response.isSuccessful) {
                        val result = response.body()

                        if (result != null) {
                            if (result.success) {
                                Toast.makeText(
                                    this@DashboardActivity,
                                    "✅ ${result.message}",
                                    Toast.LENGTH_LONG
                                ).show()

                                Log.d(TAG, "Data guru berhasil diupdate: ${result.data}")

                                // Kembali ke halaman kelola guru dan refresh data
                                Handler(Looper.getMainLooper()).postDelayed({
                                    showGuru()
                                }, 1500)

                            } else {
                                val errorMsg = result.error ?: "Gagal update data"
                                Toast.makeText(
                                    this@DashboardActivity,
                                    "❌ $errorMsg",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                this@DashboardActivity,
                                "❌ Tidak ada response dari server",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        val errorCode = response.code()
                        val errorBody = response.errorBody()?.string() ?: "No error body"

                        Log.e(TAG, "API Error: $errorCode - $errorBody")

                        Toast.makeText(
                            this@DashboardActivity,
                            "❌ Error $errorCode: ${response.message()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Log.e(TAG, "Error updating guru: ${e.message}", e)

                    val errorMsg = when (e) {
                        is java.net.UnknownHostException -> "Tidak dapat terhubung ke server"
                        is java.net.SocketTimeoutException -> "Timeout, coba lagi"
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
    }

    /**
     * FUNGSI BARU: Menampilkan dialog konfirmasi hapus guru
     */
    private fun showDeleteConfirmationDialog(guru: Guru) {
        try {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Hapus Data Guru")
                .setMessage(
                    """
                Apakah Anda yakin ingin menghapus data guru ini?
                
                👤 Nama: ${guru.nama}
                📞 Telepon: ${guru.telepon}
                📍 Alamat: ${guru.alamat}
                
                Data yang dihapus tidak dapat dikembalikan.
                """.trimIndent()
                )
                .setPositiveButton("Ya, Hapus") { dialog, which ->
                    deleteGuruFromDatabase(guru.idGuru, guru.nama)
                }
                .setNegativeButton("Batal") { dialog, which ->
                    dialog.dismiss()
                    // Tampilkan kembali dialog detail (opsional)
                    showGuruDetailDialog(guru)
                }
                .setIcon(ContextCompat.getDrawable(this, android.R.drawable.ic_delete))
                .setCancelable(false)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing delete confirmation dialog: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * FUNGSI BARU: Menghapus data guru dari database via API
     */
    private fun deleteGuruFromDatabase(idGuru: Int, namaGuru: String) {
        // Tampilkan ProgressDialog
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Menghapus data guru...")
            setCancelable(false)
        }
        progressDialog.show()

        // Buat request hapus
        val request = HapusGuruRequest(
            id_guru = idGuru
        )

        Log.d(TAG, "Request untuk hapus guru: ID=$idGuru, nama=$namaGuru")

        // Kirim ke API
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Mengirim request hapus guru...")
                val response = ApiClient.apiService.hapusGuru(request)

                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()

                    if (response.isSuccessful) {
                        val result = response.body()

                        if (result != null) {
                            if (result.success) {
                                Toast.makeText(
                                    this@DashboardActivity,
                                    "✅ ${result.message}",
                                    Toast.LENGTH_LONG
                                ).show()

                                Log.d(TAG, "Data guru berhasil dihapus: $namaGuru")

                                // Refresh data guru
                                Handler(Looper.getMainLooper()).postDelayed({
                                    showGuru()
                                }, 1000)

                            } else {
                                val errorMsg = result.error ?: "Gagal menghapus data"
                                Toast.makeText(
                                    this@DashboardActivity,
                                    "❌ $errorMsg",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                this@DashboardActivity,
                                "❌ Tidak ada response dari server",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        val errorCode = response.code()
                        val errorBody = response.errorBody()?.string() ?: "No error body"

                        Log.e(TAG, "API Error: $errorCode - $errorBody")

                        Toast.makeText(
                            this@DashboardActivity,
                            "❌ Error $errorCode: ${response.message()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Log.e(TAG, "Error deleting guru: ${e.message}", e)

                    val errorMsg = when (e) {
                        is java.net.UnknownHostException -> "Tidak dapat terhubung ke server"
                        is java.net.SocketTimeoutException -> "Timeout, coba lagi"
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
    }

    /**
     * Fungsi untuk membuka file picker
     */
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)

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
    private fun readCSVFile(uri: Uri): String? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()

                val encodings = listOf("UTF-8", "ISO-8859-1", "Windows-1252")
                var content: String? = null

                for (encoding in encodings) {
                    try {
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
                        if (testContent.isNotEmpty() && !testContent.contains("�")) {
                            content = testContent
                            Log.d(TAG, "Successfully read CSV with encoding: $encoding")
                            break
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed with encoding $encoding: ${e.message}")
                    }
                }

                if (content == null) {
                    content = String(bytes, Charsets.UTF_8)
                    Log.d(TAG, "Using UTF-8 fallback")
                }

                if (content != null) {
                    Log.d(TAG, "CSV Content loaded: ${content.length} characters")

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
                    selectedFileUri = uri
                    selectedFileName = getFileNameFromUri(uri)

                    val tambahTesView = fragmentContainer.getChildAt(0)
                    val tvFileStatus = tambahTesView.findViewById<TextView?>(R.id.tv_file_status)

                    if (tvFileStatus != null && selectedFileName != null) {
                        tvFileStatus.text = "$selectedFileName (Dipilih)"
                        tvFileStatus.setTextColor(Color.parseColor("#4CAF50"))

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
     */
    private fun getFileNameFromUri(uri: Uri): String? {
        var fileName: String? = null

        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    fileName = cursor.getString(displayNameIndex)
                }
            }
        }

        if (fileName.isNullOrEmpty()) {
            val path = uri.path
            if (path != null) {
                fileName = path.substringAfterLast("/")
            }
        }

        return fileName ?: "unknown.csv"
    }
}