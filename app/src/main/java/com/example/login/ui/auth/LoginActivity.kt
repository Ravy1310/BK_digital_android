package com.example.login.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.login.R
import com.example.login.ui.dashboard.DashboardActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import android.view.View

class LoginActivity : AppCompatActivity() {

    // UI Components
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var passwordToggle: ImageView
    private lateinit var loginButton: Button
    private lateinit var googleLoginButton: LinearLayout
    private lateinit var registerTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var forgotPasswordTextView: TextView

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    // Google Sign In Launcher
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

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Initialize UI Components
        initializeViews()

        // Setup Google Sign In
        setupGoogleSignIn()

        // Setup Click Listeners
        setupClickListeners()
    }

    private fun initializeViews() {
        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        passwordToggle = findViewById(R.id.passwordToggle)
        loginButton = findViewById(R.id.loginButton)
        googleLoginButton = findViewById(R.id.googleLoginButton)
        registerTextView = findViewById(R.id.registerTextView)
        progressBar = findViewById(R.id.progressBar)
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView)
    }

    private fun setupClickListeners() {
        // Toggle Password Visibility
        passwordToggle.setOnClickListener { togglePasswordVisibility() }

        // Login Button
        loginButton.setOnClickListener { loginWithEmailPassword() }

        // Google Login Button
        googleLoginButton.setOnClickListener { signInWithGoogle() }

        // Register Text (Navigate to RegisterActivity)
        registerTextView.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Forgot Password Text
        forgotPasswordTextView.setOnClickListener {
            Toast.makeText(this, "Forgot Password clicked", Toast.LENGTH_SHORT).show()
            // Add ForgotPasswordActivity implementation if needed
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
        if (passwordEditText.inputType == 129) { // 129 = TYPE_TEXT_VARIATION_PASSWORD
            passwordEditText.inputType = 1 // TYPE_CLASS_TEXT
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

        // Input Validation
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

        // Start Firebase Authentication
        performFirebaseLogin(email, password)
    }

    private fun performFirebaseLogin(email: String, password: String) {
        // Show loading state
        progressBar.visibility = View.VISIBLE
        loginButton.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                // Hide loading state
                progressBar.visibility = View.GONE
                loginButton.isEnabled = true

                if (task.isSuccessful) {
                    // Login successful
                    Toast.makeText(this, "Login berhasil!", Toast.LENGTH_SHORT).show()

                    // Save user data to SharedPreferences
                    saveUserData(email)

                    // Navigate to Dashboard
                    navigateToDashboard()
                } else {
                    // Login failed
                    val errorMessage = task.exception?.message ?: "Login gagal"
                    Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun saveUserData(email: String) {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val editor = sharedPref.edit()

        // Basic user info
        editor.putBoolean("is_logged_in", true)
        editor.putString("user_email", email)
        editor.putString("user_name", email.split("@")[0]) // Extract name from email

        // Additional info if needed
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            editor.putString("user_id", user.uid)
            user.displayName?.let { name ->
                editor.putString("user_display_name", name)
            }
        }

        editor.apply()
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        // Show loading state
        progressBar.visibility = View.VISIBLE
        googleLoginButton.isEnabled = false

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                // Hide loading state
                progressBar.visibility = View.GONE
                googleLoginButton.isEnabled = true

                if (task.isSuccessful) {
                    // Google Sign In successful
                    val user = auth.currentUser

                    Toast.makeText(this, "Login dengan Google berhasil!", Toast.LENGTH_SHORT).show()

                    // Save user data
                    user?.let {
                        saveGoogleUserData(it.email ?: "Google User", it.displayName ?: "Google User")
                    }

                    // Navigate to Dashboard
                    navigateToDashboard()
                } else {
                    // Google Sign In failed
                    Toast.makeText(this, "Google sign in failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveGoogleUserData(email: String, displayName: String) {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val editor = sharedPref.edit()

        editor.putBoolean("is_logged_in", true)
        editor.putString("user_email", email)
        editor.putString("user_name", displayName)
        editor.putString("auth_provider", "google")

        editor.apply()
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        // Clear back stack so user can't go back to login with back button
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()

        // Optional: Add transition animation
        // overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    override fun onStart() {
        super.onStart()

        // Check if user is already logged in
        checkAutoLogin()
    }

    private fun checkAutoLogin() {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)

        // Alternatively check Firebase current user
        val currentUser = auth.currentUser

        if (isLoggedIn || currentUser != null) {
            // User is already logged in, redirect to dashboard
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    // Optional: Add back button handling
    private var backPressedTime: Long = 0
    override fun onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            super.onBackPressed()
            return
        } else {
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
        backPressedTime = System.currentTimeMillis()
    }
}