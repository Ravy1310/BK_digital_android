package com.example.login.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import android.widget.LinearLayout
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
    private lateinit var fragmentContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        profileIcon = findViewById(R.id.profile_icon)
        fragmentContainer = findViewById(R.id.fragment_container)

        // Setup navigation
        navigationView.setNavigationItemSelectedListener(this)
        navigationView.setCheckedItem(R.id.nav_dashboard)

        // Menu icon click to open drawer
        val menuIcon = findViewById<ImageView>(R.id.menu_icon)
        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Profile icon click for popup menu
        profileIcon.setOnClickListener { view ->
            showProfileMenu(view)
        }

        // Tampilkan dashboard sebagai default
        showDashboardContent()

        // Setup back button handler
        setupBackPressedHandler()
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
            }
            R.id.nav_siswa -> {
                Toast.makeText(this, "Kelola Data Siswa", Toast.LENGTH_SHORT).show()
                navigationView.setCheckedItem(R.id.nav_siswa)
            }
            R.id.nav_guru -> {
                Toast.makeText(this, "Kelola Data Guru", Toast.LENGTH_SHORT).show()
                navigationView.setCheckedItem(R.id.nav_guru)
            }
            R.id.nav_tes -> {
                // FOKUS: Ketika menu Tes diklik, tampilkan kelolasoaltes.xml
                showKelolaTesContent()
                navigationView.setCheckedItem(R.id.nav_tes)
            }
        }

        // Tutup drawer setelah item dipilih
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    /**
     * Fungsi SEDERHANA untuk menampilkan konten dashboard default
     */
    private fun showDashboardContent() {
        fragmentContainer.removeAllViews()

        // Inflate dengan attachToRoot = false
        val dashboardView = layoutInflater.inflate(
            R.layout.dasboarddlagii,
            fragmentContainer,
            false
        )
        fragmentContainer.addView(dashboardView)
        Toast.makeText(this, "Dashboard", Toast.LENGTH_SHORT).show()
    }
    /**
     * Fungsi SEDERHANA untuk menampilkan konten kelola soal tes
     * FOKUS: Hanya ganti content ke kelolasoaltes.xml
     */
    private fun showKelolaTesContent() {
        // 1. Hapus semua view yang ada di container
        fragmentContainer.removeAllViews()

        // 2. Inflate layout kelola soal tes
        val kelolaTesView = layoutInflater.inflate(R.layout.kelolasoaltes, null)

        // 3. Tambahkan ke container
        fragmentContainer.addView(kelolaTesView)

        // 4. Tampilkan pesan
        Toast.makeText(this, "Kelola Tes", Toast.LENGTH_SHORT).show()
    }

    private fun setupBackPressedHandler() {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun logoutUser() {
        try {
            Firebase.auth.signOut()

            val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.clear()
            editor.apply()

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