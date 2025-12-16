package com.example.login.models
// File: app/src/main/java/com/example/login/models/UpdateStatus.kt


import com.google.gson.annotations.SerializedName

data class UpdateStatusRequest(
    @SerializedName("id_tes") val idTes: Int,
    @SerializedName("action") val action: String // "aktif" atau "nonaktif"
)

data class UpdateStatusResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: UpdatedTesData?
)

data class UpdatedTesData(
    @SerializedName("id_tes") val idTes: Int,
    @SerializedName("kategori_tes") val kategoriTes: String,
    @SerializedName("deskripsi_tes") val deskripsiTes: String,
    @SerializedName("status") val status: String,
    @SerializedName("status_text") val statusText: String,
    @SerializedName("updated_at") val updatedAt: String
)