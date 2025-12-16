// File: app/src/main/java/com/example/login/api/ApiService.kt
package com.example.login.api

import com.example.login.models.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query  // TAMBAHKAN INI

interface ApiService {
    @GET("api_dashboard.php")
    suspend fun getDashboardData(): Response<DashboardResponse>

    @GET("api_kelola_tes.php")
    suspend fun getKelolaTesData(): Response<KelolaTesResponse>

    @POST("api_tambah_tes_simple.php")
    suspend fun tambahTes(
        @Body request: TambahTesRequest
    ): Response<TambahTesResponse>

    @GET("api_kelola_soal.php")
    suspend fun getKelolaSoalData(): Response<KelolaSoalResponse>

    @POST("api_update_status_tes.php")
    suspend fun updateStatusTes(
        @Body request: UpdateStatusRequest
    ): Response<UpdateStatusResponse>

    // API untuk hapus tes - GUNAKAN MODEL KONKRET
    @POST("api_hapus_tes.php")
    suspend fun hapusTes(
        @Body request: HapusTesRequest  // Gunakan model konkret, bukan Map
    ): Response<HapusTesResponse>

    // API untuk mendapatkan soal berdasarkan ID tes
    @GET("api_get_soal_by_tes.php")
    suspend fun getSoalByTes(
        @Query("id_tes") idTes: Int
    ): Response<SoalTesResponse>


}