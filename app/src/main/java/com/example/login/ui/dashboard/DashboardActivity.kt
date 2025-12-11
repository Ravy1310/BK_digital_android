package com.example.login.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.login.R
import com.example.login.ui.auth.LoginActivity
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class DashboardActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var profileIcon: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Ganti ke layout dengan drawer

        // Initialize views
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)

        // Profile icon di header (bukan di navigation drawer)
        profileIcon = findViewById(R.id.profile_icon)

        // Setup navigation
        navigationView.setNavigationItemSelectedListener(this)

        // Set default selected item
        navigationView.setCheckedItem(R.id.nav_dashboard)

        // Setup header dengan data user
        setupNavHeader()

        // Menu icon click to open drawer
        val menuIcon = findViewById<ImageView>(R.id.menu_icon)
        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Profile icon click for popup menu
        profileIcon.setOnClickListener { view ->
            showProfileMenu(view)
        }

        // Setup click listeners untuk konten dashboard
        setupDashboardListeners()

        // Setup back button handler (CARA BARU)
        setupBackPressedHandler()
    }

    private fun setupBackPressedHandler() {
        // Create callback for handling back button press
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Tutup drawer jika terbuka, jika tidak, biarkan sistem menutup activity
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    // Biarkan sistem menangani back press
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }

        // Add callback to the dispatcher
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun setupNavHeader() {
        // Ambil data user dari SharedPreferences
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userName = sharedPref.getString("user_name", "Guest") ?: "Guest"
        val userEmail = sharedPref.getString("user_email", "user@example.com") ?: "user@example.com"

        // Update header navigation drawer
        val headerView = navigationView.getHeaderView(0)
        val tvUserName = headerView.findViewById<TextView>(R.id.tv_user_name)
        val tvUserEmail = headerView.findViewById<TextView>(R.id.tv_user_email)

        tvUserName?.text = userName
        tvUserEmail?.text = userEmail
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

    private fun setupDashboardListeners() {
        // Setup click listeners untuk elemen di dashboard

        // "Lihat Semua" text
        val tvLihatSemua = findViewById<TextView>(R.id.tv_lihat_semua)
        tvLihatSemua?.setOnClickListener {
            Toast.makeText(this, "Lihat semua tes", Toast.LENGTH_SHORT).show()
            // Intent ke halaman semua tes
            // startActivity(Intent(this, AllTestsActivity::class.java))
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks
        when (item.itemId) {
            R.id.nav_dashboard -> {
                // Sudah di dashboard
                Toast.makeText(this, "Dashboard", Toast.LENGTH_SHORT).show()
            }

            R.id.nav_siswa -> {
                Toast.makeText(this, "Kelola Data Siswa", Toast.LENGTH_SHORT).show()
                // Intent ke SiswaActivity
                // val intent = Intent(this, SiswaActivity::class.java)
                // startActivity(intent)
            }

            R.id.nav_guru -> {
                Toast.makeText(this, "Kelola Data Guru", Toast.LENGTH_SHORT).show()
                // Intent ke GuruActivity
                // val intent = Intent(this, GuruActivity::class.java)
                // startActivity(intent)
            }

            R.id.nav_tes -> {
                Toast.makeText(this, "Kelola Tes", Toast.LENGTH_SHORT).show()
                // Intent ke TesActivity
                // val intent = Intent(this, TesActivity::class.java)
                // startActivity(intent)
            }

            R.id.nav_settings -> {
                Toast.makeText(this, "Pengaturan", Toast.LENGTH_SHORT).show()
                // Intent ke SettingsActivity
                // val intent = Intent(this, SettingsActivity::class.java)
                // startActivity(intent)
            }

            R.id.nav_help -> {
                Toast.makeText(this, "Bantuan", Toast.LENGTH_SHORT).show()
                // Intent ke HelpActivity
                // val intent = Intent(this, HelpActivity::class.java)
                // startActivity(intent)
            }
        }

        // Tutup drawer setelah item dipilih
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun logoutUser() {
        try {
            // 1. Logout dari Firebase
            Firebase.auth.signOut()

            // 2. Hapus semua data dari SharedPreferences
            val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.clear()
            editor.apply()

            // 3. Tampilkan pesan
            Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()

            // 4. Redirect ke LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()

        } catch (e: Exception) {
            Toast.makeText(this, "Error saat logout: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


}