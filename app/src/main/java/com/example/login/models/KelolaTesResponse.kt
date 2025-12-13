package com.example.login.models


import com.google.gson.annotations.SerializedName

data class KelolaTesResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: KelolaTesData?
)

data class KelolaTesData(
    @SerializedName("total_soal") val totalSoal: Int,
    @SerializedName("jenis_tes") val jenisTes: Int,
    @SerializedName("daftar_tes") val daftarTes: List<DetailTes>,
    @SerializedName("timestamp") val timestamp: String?
)

data class DetailTes(
    @SerializedName("id_tes") val idTes: String,
    @SerializedName("nama_tes") val namaTes: String,
    @SerializedName("deskripsi") val deskripsi: String,
    @SerializedName("jumlah_soal") val jumlahSoal: Int,
    @SerializedName("kategori") val kategori: String,
    @SerializedName("status") val status: String, // aktif/nonaktif
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
)