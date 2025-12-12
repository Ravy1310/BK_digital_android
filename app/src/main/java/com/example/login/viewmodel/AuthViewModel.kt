package com.example.login.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope  // ✅ IMPORT INI
import com.example.login.api.ApiClient
import com.example.login.api.models.LoginResponse
import kotlinx.coroutines.launch  // ✅ IMPORT INI
import retrofit2.HttpException
import java.io.IOException

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    // LiveData untuk UI
    private val _loginResult = MutableLiveData<LoginResponse?>()
    val loginResult: LiveData<LoginResponse?> = _loginResult

    private val _registerResult = MutableLiveData<String?>()
    val registerResult: LiveData<String?> = _registerResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun login(email: String, password: String) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {  // ✅ SEKARANG viewModelScope HARUSNYA TIDAK ERROR
            try {
                val apiService = ApiClient.getApiService(getApplication())
                val response = apiService.login(
                    com.example.login.api.models.LoginRequest(email, password)
                )

                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    _loginResult.value = loginResponse

                    if (loginResponse?.success == true) {
                        // Login berhasil
                        _errorMessage.value = null
                    } else {
                        _errorMessage.value = loginResponse?.message ?: "Login failed"
                    }
                } else {
                    _errorMessage.value = "Login failed: ${response.code()}"
                }
            } catch (e: IOException) {
                _errorMessage.value = "Network error: ${e.message}"
            } catch (e: HttpException) {
                _errorMessage.value = "HTTP error: ${e.code()}"
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(name: String, email: String, password: String, confirmPassword: String) {
        // Validasi input
        if (password != confirmPassword) {
            _errorMessage.value = "Passwords don't match"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val apiService = ApiClient.getApiService(getApplication())
                val response = apiService.register(
                    com.example.login.api.models.RegisterRequest(
                        name = name,
                        email = email,
                        password = password,
                        passwordConfirmation = confirmPassword
                    )
                )

                if (response.isSuccessful) {
                    val registerResponse = response.body()
                    if (registerResponse?.success == true) {
                        _registerResult.value = "Registration successful!"
                        // Auto login setelah register
                        login(email, password)
                    } else {
                        _errorMessage.value = registerResponse?.message ?: "Registration failed"
                    }
                } else {
                    _errorMessage.value = "Registration failed: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}