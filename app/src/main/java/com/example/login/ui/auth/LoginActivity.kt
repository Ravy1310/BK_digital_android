package com.example.login.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.login.R
import com.example.login.api.ApiClient
import com.example.login.api.models.LoginRequest
import com.example.login.ui.dashboard.DashboardActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var passwordToggle: ImageView
    private lateinit var loginButton: Button
    private lateinit var googleLoginButton: LinearLayout
    private lateinit var registerTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var forgotPasswordTextView: TextView  // ✅ TAMBAHKAN INI

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private val TAG = "LoginActivity"

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth
        initializeViews()
        setupGoogleSignIn()
        setupClickListeners()

        // ✅ TEST API CONNECTION SAAT APP START
        testApiConnection()
    }

    private fun initializeViews() {
        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        passwordToggle = findViewById(R.id.passwordToggle)
        loginButton = findViewById(R.id.loginButton)
        googleLoginButton = findViewById(R.id.googleLoginButton)
        registerTextView = findViewById(R.id.registerTextView)
        progressBar = findViewById(R.id.progressBar)
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView)  // ✅ INISIALISASI
    }

    private fun setupClickListeners() {
        passwordToggle.setOnClickListener { togglePasswordVisibility() }
        loginButton.setOnClickListener { loginWithEmailPassword() }
        googleLoginButton.setOnClickListener { signInWithGoogle() }

        // ✅ INTENT KE REGISTER ACTIVITY
        registerTextView.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            // Tambahkan animasi jika mau
            // overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // ✅ INTENT KE FORGOT PASSWORD (jika ada activity-nya)
        forgotPasswordTextView.setOnClickListener {
            Toast.makeText(this, "Forgot Password clicked", Toast.LENGTH_SHORT).show()
            // Jika ada ForgotPasswordActivity:
            // val intent = Intent(this, ForgotPasswordActivity::class.java)
            // startActivity(intent)
        }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun togglePasswordVisibility() {
        if (passwordEditText.inputType == 129) {
            passwordEditText.inputType = 1
            passwordToggle.setImageResource(android.R.drawable.ic_lock_idle_lock)
        } else {
            passwordEditText.inputType = 129
            passwordToggle.setImageResource(android.R.drawable.ic_lock_lock)
        }
        passwordEditText.setSelection(passwordEditText.text.length)
    }

    private fun loginWithEmailPassword() {
        val email = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        // Validasi input
        if (email.isEmpty()) {
            usernameEditText.error = "Email harus diisi"
            usernameEditText.requestFocus()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            usernameEditText.error = "Format email tidak valid"
            usernameEditText.requestFocus()
            return
        }

        if (password.isEmpty()) {
            passwordEditText.error = "Password harus diisi"
            passwordEditText.requestFocus()
            return
        }

        // ✅ OPTION 1: PAKAI API BACKEND (RECOMMENDED)
        loginWithApi(email, password)

        // ✅ OPTION 2: PAKAI FIREBASE (Jika mau tetap pakai Firebase)
        // loginWithFirebase(email, password)
    }

    // ✅ METHOD BARU: LOGIN DENGAN API BACKEND
    private fun loginWithApi(email: String, password: String) {
        progressBar.visibility = View.VISIBLE
        loginButton.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiService = ApiClient.getApiService(this@LoginActivity)
                val response = apiService.login(LoginRequest(email, password))

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    loginButton.isEnabled = true

                    if (response.isSuccessful && response.body() != null) {
                        val loginResponse = response.body()!!

                        if (loginResponse.success) {
                            // Login berhasil via API
                            Toast.makeText(
                                this@LoginActivity,
                                "Login berhasil via API!",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Simpan token dan user data
                            saveApiUserData(loginResponse)

                            // Navigate ke Dashboard
                            navigateToDashboard()
                        } else {
                            Toast.makeText(
                                this@LoginActivity,
                                "Login gagal: ${loginResponse.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            "Error: ${response.code()} - ${response.message()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    loginButton.isEnabled = true
                    Toast.makeText(
                        this@LoginActivity,
                        "Network error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()

                    // Fallback ke Firebase jika API gagal
                    loginWithFirebase(email, password)
                }
            }
        }
    }

    // ✅ METHOD BARU: SIMPAN DATA USER DARI API
    private fun saveApiUserData(loginResponse: com.example.login.api.models.LoginResponse) {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val editor = sharedPref.edit()

        editor.putBoolean("is_logged_in", true)
        editor.putString("auth_type", "api")  // Tandai login via API

        // Simpan token jika ada
        loginResponse.token?.let { token ->
            editor.putString("api_token", token)
        }

        // Simpan user data jika ada
        loginResponse.user?.let { user ->
            editor.putString("user_name", user.name)
            editor.putString("user_email", user.email)
            editor.putInt("user_id", user.id)
        }

        editor.apply()

        // Simpan juga di TokenManager jika ada
        loginResponse.token?.let { token ->
            com.example.login.utils.TokenManager.saveToken(this, token)
        }
    }

    // ✅ METHOD LAMA: LOGIN DENGAN FIREBASE
    private fun loginWithFirebase(email: String, password: String) {
        loginButton.isEnabled = false
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login berhasil via Firebase!", Toast.LENGTH_SHORT).show()
                    saveFirebaseUserData(email)
                    navigateToDashboard()
                } else {
                    Toast.makeText(this, "Login gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
                loginButton.isEnabled = true
            }
    }

    private fun saveFirebaseUserData(email: String) {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putBoolean("is_logged_in", true)
        editor.putString("auth_type", "firebase")  // Tandai login via Firebase
        editor.putString("user_name", email.split("@")[0])
        editor.putString("user_email", email)
        editor.apply()
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        saveFirebaseUserData(it.email ?: "Google User")
                    }
                    navigateToDashboard()
                }
            }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish()

        // Tambahkan animasi transisi
        // overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    // ✅ METHOD BARU: TEST API CONNECTION
    private fun testApiConnection() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiService = ApiClient.getApiService(this@LoginActivity)
                Log.d(TAG, "✅ ApiClient created successfully")

                // Optional: Test dengan dummy request
                // val response = apiService.login(LoginRequest("test", "test"))
                // Log.d(TAG, "Test response code: ${response.code()}")

            } catch (e: Exception) {
                Log.e(TAG, "❌ ApiClient error: ${e.message}")
            }
        }
    }

    override fun onStart() {
        super.onStart()

        // Cek jika sudah login
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        if (sharedPref.getBoolean("is_logged_in", false)) {
            // Auto redirect ke Dashboard
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Atau cek dengan TokenManager
        if (com.example.login.utils.TokenManager.isLoggedIn(this)) {
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}