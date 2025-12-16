package com.example.login.models
// File: app/src/main/java/com/example/login/models/KelolaSoalResponse.kt


import com.google.gson.annotations.SerializedName

/**
 * Response model untuk data kelola soal tes
 */
data class KelolaSoalResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: KelolaSoalData?
)

/**
 * Data utama yang berisi daftar tes
 */
data class KelolaSoalData(
    @SerializedName("daftar_tes")
    val daftarTes: List<TesDetail>,

    @SerializedName("total_tes")
    val totalTes: Int,

    @SerializedName("total_soal")
    val totalSoal: Int
)

/**
 * Detail data untuk setiap tes
 */
data class TesDetail(
    @SerializedName("id_tes")
    val idTes: Int,

    @SerializedName("kategori_tes")
    val kategoriTes: String,

    @SerializedName("deskripsi_tes")
    val deskripsiTes: String,

    @SerializedName("status")
    val status: String, // "aktif" atau "nonaktif"

    @SerializedName("status_text")
    val statusText: String, // "Aktif" atau "Nonaktif"

    @SerializedName("status_color")
    val statusColor: String, // Warna dalam hex

    @SerializedName("status_background")
    val statusBackground: String, // Warna background dalam hex

    @SerializedName("jumlah_soal")
    val jumlahSoal: Int,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("updated_at")
    val updatedAt: String
)