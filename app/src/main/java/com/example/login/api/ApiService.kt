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

    // ==================== SISWA ====================
    // ==================== SISWA ====================
    @GET("api_get_siswa.php")
    suspend fun getDataSiswa(): Response<SiswaResponse>

    @POST("api_tambah_siswa.php")
    suspend fun tambahSiswa(
        @Body request: TambahSiswaRequest
    ): Response<TambahSiswaResponse>

    @GET("api_get_siswa.php")
    suspend fun getSiswaById(
        @Query("id_siswa") idSiswa: String
    ): Response<SiswaResponse>

    @POST("api_update_siswa.php")
    suspend fun updateSiswa(
        @Body request: UpdateSiswaRequest
    ): Response<UpdateSiswaResponse>
    // Di bagian SISWA, tambahkan:
    @POST("api_hapus_siswa.php")
    suspend fun hapusSiswa(
        @Body request: HapusSiswaRequest
    ): Response<HapusSiswaResponse>

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

    @POST("api_update_soal_dan_opsi.php")
    suspend fun updateSoalComplete(@Body request: UpdateSoalCompleteRequest): Response<UpdateSoalCompleteResponse>

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

    @POST("api_tambah_guru.php")
    suspend fun tambahGuru(
        @Body request: TambahGuruRequest
    ): Response<TambahGuruResponse>

    @PUT("api_update_guru.php")
    suspend fun updateGuru(
        @Body request: UpdateGuruRequest
    ): Response<UpdateGuruResponse>

    @POST("api_hapus_guru.php")
    suspend fun hapusGuru(
        @Body request: HapusGuruRequest
    ): Response<HapusGuruResponse>
}