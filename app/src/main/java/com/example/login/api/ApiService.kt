package com.example.login.api

import com.example.login.models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // ==================== DASHBOARD ====================
    @GET("api_dashboard.php")
    suspend fun getDashboardData(): Response<DashboardResponse>

    // ==================== KELOLA TES ====================
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

    @POST("api_hapus_tes.php")
    suspend fun hapusTes(
        @Body request: HapusTesRequest
    ): Response<HapusTesResponse>

    // ==================== SOAL ====================
    @GET("api_get_soal_by_tes.php")
    suspend fun getSoalByTes(
        @Query("id_tes") idTes: Int
    ): Response<SoalTesResponse>

    @POST("api_tambah_soal.php")
    suspend fun tambahSoal(
        @Body request: TambahSoalRequest
    ): Response<TambahSoalResponse>

    // ✅ ENDPOINT BARU: Update Soal
    @POST("api_update_soal_dan_opsi.php")
    suspend fun updateSoalComplete(@Body request: UpdateSoalCompleteRequest): Response<UpdateSoalCompleteResponse>


    // ✅ ENDPOINT BARU: Hapus Soal
    @POST("api_hapus_soal.php")
    suspend fun hapusSoal(
        @Body request: HapusSoalRequest
    ): Response<HapusSoalResponse>
    // ==================== GURU ====================
    @GET("api_get_guru.php")
    suspend fun getDataGuru(): Response<GuruResponse>

    @PUT("api_update_status_guru.php")
    suspend fun updateStatusGuru(
        @Body request: UpdateStatusGuruRequest
    ): Response<UpdateStatusGuruResponse>
}