package com.example.login

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var passwordToggle: ImageView
    private lateinit var loginButton: Button
    private lateinit var googleLoginButton: LinearLayout // <-- Ubah dari Button ke LinearLayout

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    // Google Sign-In Launcher
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
        } else {
            Toast.makeText(this, "Google sign in cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth
        initializeViews()
        setupGoogleSignIn()
        setupClickListeners()
    }

    private fun initializeViews() {
        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        passwordToggle = findViewById(R.id.passwordToggle)
        loginButton = findViewById(R.id.loginButton)
        googleLoginButton = findViewById(R.id.googleLoginButton) // <-- LinearLayout
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupClickListeners() {
        passwordToggle.setOnClickListener { togglePasswordVisibility() }
        loginButton.setOnClickListener { loginWithEmailPassword() }
        googleLoginButton.setOnClickListener { signInWithGoogle() } // <-- Tetap bisa diklik
    }

    private fun togglePasswordVisibility() {
        if (passwordEditText.inputType == 129) { // 129 = textPassword
            passwordEditText.inputType = 1 // 1 = text
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

        if (TextUtils.isEmpty(email)) {
            usernameEditText.error = "Email harus diisi"
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            usernameEditText.error = "Format email tidak valid"
            return
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.error = "Password harus diisi"
            return
        }

        if (password.length < 6) {
            passwordEditText.error = "Password minimal 6 karakter"
            return
        }

        loginButton.isEnabled = false
        (loginButton as? Button)?.text = "Loading..."

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login berhasil!", Toast.LENGTH_SHORT).show()
                    saveUserData(email)
                    navigateToDashboard()
                } else {
                    Toast.makeText(this, "Login gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
                loginButton.isEnabled = true
                (loginButton as? Button)?.text = "LOGIN"
            }
    }

    private fun saveUserData(email: String) {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putBoolean("is_logged_in", true)
        editor.putString("user_name", email.split("@")[0])
        editor.putString("user_email", email) // Simpan email juga
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
                    Toast.makeText(this, "Login dengan Google berhasil!", Toast.LENGTH_SHORT).show()
                    val user = auth.currentUser
                    user?.let {
                        saveUserData(it.email ?: it.displayName ?: "Google User")
                    }
                    navigateToDashboard()
                } else {
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish()
    }
    override fun onStart() {
        super.onStart()

        // Cek apakah user sudah login
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)

        if (isLoggedIn) {
            // User sudah login, langsung redirect ke dashboard
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}