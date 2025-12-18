// File: app/src/main/java/com/example/login/models/SoalModels.kt
package com.example.login.models

import com.google.gson.annotations.SerializedName

// ==================== MODELS UNTUK TAMBAH SOAL ====================

// Model untuk request tambah soal
data class TambahSoalRequest(
    @SerializedName("id_tes") val id_tes: Int,
    @SerializedName("pertanyaan") val pertanyaan: String,
    @SerializedName("opsi") val opsi: List<String>,
    @SerializedName("bobot") val bobot: List<Int>
)

// Model untuk response tambah soal
data class TambahSoalResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: TambahSoalData? = null,
    @SerializedName("error") val error: String? = null
)

data class TambahSoalData(
    @SerializedName("id_soal") val id_soal: Int,
    @SerializedName("id_tes") val id_tes: Int,
    @SerializedName("pertanyaan") val pertanyaan: String,
    @SerializedName("jumlah_opsi") val jumlah_opsi: Int,
    @SerializedName("opsi_ids") val opsi_ids: List<Int>? = null,
    @SerializedName("nama_tes") val nama_tes: String? = null
)
data class HapusSoalRequest(
    @SerializedName("id_soal")
    val idSoal: Int
)

