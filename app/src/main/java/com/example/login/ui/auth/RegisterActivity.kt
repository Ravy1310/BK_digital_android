package com.example.login.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.login.ui.dashboard.DashboardActivity
import com.example.login.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {

    // UI Components
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var googleRegisterButton: LinearLayout
    private lateinit var loginLink: TextView

    // Firebase & Google
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    // Launcher untuk Google Sign In
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In sukses, autentikasi ke Firebase
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign up failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = Firebase.auth

        initializeViews()
        setupGoogleSignIn()
        setupListeners()
    }

    private fun initializeViews() {
        emailEditText = findViewById(R.id.regEmailEditText)
        passwordEditText = findViewById(R.id.regPasswordEditText)
        confirmPasswordEditText = findViewById(R.id.regConfirmPasswordEditText)
        registerButton = findViewById(R.id.registerButton)
        googleRegisterButton = findViewById(R.id.googleRegisterButton)
        loginLink = findViewById(R.id.loginLink)
    }

    private fun setupGoogleSignIn() {
        // Konfigurasi Google Sign In (Sama seperti di LoginActivity)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupListeners() {
        // 1. Tombol Daftar Manual
        registerButton.setOnClickListener {
            performManualRegister()
        }

        // 2. Tombol Daftar Google
        googleRegisterButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        // 3. Link ke Halaman Login
        loginLink.setOnClickListener {
            finish() // Menutup activity ini akan kembali ke LoginActivity di belakangnya
        }
    }

    private fun performManualRegister() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val confirmPassword = confirmPasswordEditText.text.toString().trim()

        // Validasi Input
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.error = "Email tidak valid"
            return
        }
        if (password.length < 6) {
            passwordEditText.error = "Password minimal 6 karakter"
            return
        }
        if (password != confirmPassword) {
            confirmPasswordEditText.error = "Password tidak sama"
            return
        }

        // Proses Register Firebase
        registerButton.isEnabled = false
        registerButton.text = "Loading..."

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Registrasi Berhasil!", Toast.LENGTH_SHORT).show()

                    // Simpan data sederhana ke SharedPreferences (Opsional)
                    val user = auth.currentUser
                    saveUserData(user?.email ?: "")

                    // Pindah ke Dashboard
                    navigateToDashboard()
                } else {
                    Toast.makeText(this, "Gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    registerButton.isEnabled = true
                    registerButton.text = "Daftar Sekarang"
                }
            }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Di Firebase, Sign In dengan kredensial baru otomatis dianggap Register
                    Toast.makeText(this, "Daftar dengan Google berhasil!", Toast.LENGTH_SHORT).show()
                    val user = auth.currentUser
                    saveUserData(user?.email ?: "")
                    navigateToDashboard()
                } else {
                    Toast.makeText(this, "Gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserData(email: String) {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putBoolean("is_logged_in", true)
        editor.putString("user_email", email)
        editor.apply()
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        // Clear task agar user tidak bisa kembali ke halaman register dengan tombol back
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}