package com.example.login.ui.test

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.login.api.ApiClient
import com.example.login.api.models.LoginRequest
import com.example.login.databinding.ActivityTestBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTestBinding
    private val TAG = "API_TEST"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnTestConnection.setOnClickListener {
            testApiConnection()
        }

        binding.btnTestLogin.setOnClickListener {
            testLoginApi()
        }

        // Auto test saat activity dimulai
        testApiConnection()
    }

    private fun testApiConnection() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // ✅ GUNAKAN apiService untuk test sebenarnya
                val apiService = ApiClient.getApiService(this@TestActivity)

                // Test 1: Cek apakah service bisa dibuat
                Log.d(TAG, "✅ ApiService created successfully")
                Log.d(TAG, "📡 Testing API connection to: http://10.0.2.2:8000/api/")

                // Test 2: Coba ping endpoint sederhana (jika ada)
                // val response = apiService.getSomePublicEndpoint()

                runOnUiThread {
                    Toast.makeText(
                        this@TestActivity,
                        "✅ API Service ready",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.tvResult.text = "✅ API Service ready\nBase URL: http://10.0.2.2:8000/api/"
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Connection error: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(
                        this@TestActivity,
                        "❌ Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.tvResult.text = "❌ Error: ${e.message}"
                }
            }
        }
    }

    private fun testLoginApi() {
        val testEmail = "test@example.com"
        val testPassword = "password123"

        binding.tvResult.text = "Testing login API..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // ✅ GUNAKAN apiService
                val apiService = ApiClient.getApiService(this@TestActivity)
                val response = apiService.login(LoginRequest(testEmail, testPassword))

                Log.d(TAG, "Login Test Response Code: ${response.code()}")
                Log.d(TAG, "Response Body: ${response.body()}")

                runOnUiThread {
                    if (response.isSuccessful) {
                        val result = response.body()
                        val message = when {
                            result?.success == true -> "✅ Login API works! User: ${result.user?.name}"
                            result?.success == false -> "⚠️ API works but login failed: ${result.message}"
                            else -> "📡 API Response: ${response.code()}"
                        }

                        Toast.makeText(this@TestActivity, message, Toast.LENGTH_LONG).show()
                        binding.tvResult.text = """
                            📡 API Test Result:
                            Code: ${response.code()}
                            Success: ${response.isSuccessful}
                            Message: ${result?.message}
                            Token: ${if (result?.token?.isNotEmpty() == true) "✓" else "✗"}
                        """.trimIndent()
                    } else {
                        Toast.makeText(
                            this@TestActivity,
                            "❌ Login failed. Code: ${response.code()}",
                            Toast.LENGTH_LONG
                        ).show()
                        binding.tvResult.text = "❌ HTTP ${response.code()}: ${response.message()}"
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Login test error: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(
                        this@TestActivity,
                        "❌ Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.tvResult.text = "❌ Exception: ${e.message}"
                }
            }
        }
    }

    // Tambahkan fungsi untuk test register juga
    private fun testRegisterApi() {
        val testName = "Test User"
        val testEmail = "testuser@example.com"
        val testPassword = "password123"
        val testConfirmPassword = "password123"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiService = ApiClient.getApiService(this@TestActivity)
                val response = apiService.register(
                    com.example.login.api.models.RegisterRequest(
                        name = testName,
                        email = testEmail,
                        password = testPassword,
                        passwordConfirmation = testConfirmPassword
                    )
                )

                runOnUiThread {
                    val message = if (response.isSuccessful) {
                        val result = response.body()
                        "Register API: ${result?.message ?: "Success"}"
                    } else {
                        "Register failed: ${response.code()}"
                    }

                    Toast.makeText(this@TestActivity, message, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Register test error", e)
            }
        }
    }
}