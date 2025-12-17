// File: app/src/main/java/com/example/login/api/ApiService.kt
package com.example.login.api

import com.example.login.models.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

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

    // API untuk hapus tes
    @POST("api_hapus_tes.php")
    suspend fun hapusTes(
        @Body request: HapusTesRequest
    ): Response<HapusTesResponse>

    // API untuk mendapatkan soal berdasarkan ID tes
    @GET("api_get_soal_by_tes.php")
    suspend fun getSoalByTes(
        @Query("id_tes") idTes: Int
    ): Response<SoalTesResponse>

    // API untuk mendapatkan data guru
    @GET("api_get_guru.php")
    suspend fun getDataGuru(): Response<GuruResponse>

    // API untuk update status guru - OPSI 1: JSON Body
    @PUT("api_update_status_guru.php")
    suspend fun updateStatusGuru(
        @Body request: UpdateStatusGuruRequest
    ): Response<UpdateStatusGuruResponse>

    // OPSI 2: FormUrlEncoded (jika API menerima form data)
    /*
    @FormUrlEncoded
    @POST("api_update_status_guru.php")
    suspend fun updateStatusGuru(
        @Field("id_guru") idGuru: Int,
        @Field("action") action: String = "ubah_status"
    ): Response<UpdateStatusGuruResponse>
    */
}