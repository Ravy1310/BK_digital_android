package com.example.login.api

import android.content.Context
import com.example.login.api.models.*
import com.example.login.utils.TokenManager
import kotlin.Result

class AuthRepository(
    private val apiService: ApiService,
    private val context: Context
) {

    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val response = apiService.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!
                if (loginResponse.success) {
                    // Simpan token
                    loginResponse.token?.let { token ->
                        TokenManager.saveToken(context, token)
                    }
                    Result.success(loginResponse)
                } else {
                    Result.failure(Exception(loginResponse.message))
                }
            } else {
                Result.failure(Exception("Login failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}