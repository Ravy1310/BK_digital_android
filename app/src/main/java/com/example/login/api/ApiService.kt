// File: app/src/main/java/com/example/login/api/ApiService.kt
package com.example.login.api

import com.example.login.models.DashboardResponse
import com.example.login.models.KelolaTesResponse
import retrofit2.Response
import retrofit2.http.GET

interface ApiService {
    @GET("api_dashboard.php")
    suspend fun getDashboardData(): Response<DashboardResponse>

    @GET("api_kelola_tes.php")
    suspend fun getKelolaTesData(): Response<KelolaTesResponse>
}