package com.example.login.api

import com.example.login.api.models.Answer
import com.example.login.api.models.ApiResponse
import com.example.login.api.models.DashboardData
import com.example.login.api.models.LoginRequest
import com.example.login.api.models.LoginResponse
import com.example.login.api.models.RegisterRequest
import com.example.login.api.models.RegisterResponse
import com.example.login.api.models.Test
import com.example.login.api.models.TestResult
import com.example.login.api.models.UserData
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    // ========== AUTH ENDPOINTS ==========
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("auth/logout")
    suspend fun logout(@Header("Authorization") token: String): Response<ApiResponse<Unit>>

    // ========== DASHBOARD ENDPOINTS ==========
    @GET("dashboard")
    suspend fun getDashboardData(@Header("Authorization") token: String): Response<ApiResponse<DashboardData>>

    // ========== PROFILE ENDPOINTS ==========
    @GET("profile")
    suspend fun getProfile(@Header("Authorization") token: String): Response<ApiResponse<UserData>>

    // ========== TEST ENDPOINTS ==========
    @GET("api/tests")
    suspend fun getTests(@Header("Authorization") token: String): Response<ApiResponse<List<Test>>>

    @POST("api/tests/{id}/submit")
    suspend fun submitTest(
        @Header("Authorization") token: String,
        @Path("id") testId: Int,
        @Body answers: List<Answer>
    ): Response<ApiResponse<TestResult>>
}