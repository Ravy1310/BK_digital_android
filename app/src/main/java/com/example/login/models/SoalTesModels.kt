// File: app/src/main/java/com/example/login/models/SoalTesModels.kt
package com.example.login.models

import com.google.gson.annotations.SerializedName

data class SoalTesResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("data")
    val data: SoalTesData? = null,

    @SerializedName("error")
    val error: String? = null
)

data class SoalTesData(
    @SerializedName("tes")
    val tes: TesData? = null,

    @SerializedName("soal_list")
    val soal_list: List<SoalData> = emptyList(),

    @SerializedName("jumlah_soal")
    val jumlah_soal: Int = 0
)

data class TesData(
    @SerializedName("id_tes")
    val id_tes: Int = 0,

    @SerializedName("kategori_tes")
    val kategori_tes: String = "",

    @SerializedName("deskripsi_tes")
    val deskripsi_tes: String = "",

    @SerializedName("status")
    val status: String = ""
)

data class SoalData(
    @SerializedName("id_soal")
    val id_soal: Int = 0,

    @SerializedName("pertanyaan")
    val pertanyaan: String = "",

    @SerializedName("id_tes")
    val id_tes: Int = 0,

    @SerializedName("opsi_list")
    val opsi_list: List<OpsiData> = emptyList()
)

data class OpsiData(
    @SerializedName("id_opsi")
    val id_opsi: Int = 0,

    // Semua kemungkinan nama field
    @SerializedName("opsi_text")
    val opsi_text: String? = null,

    @SerializedName("opsi")
    val opsi: String? = null,

    @SerializedName("jawaban")
    val jawaban: String? = null,

    @SerializedName("teks_opsi")
    val teks_opsi: String? = null,

    @SerializedName("bobot")
    val bobot: Int = 0
)


// ==================== MODEL UPDATE SOAL ====================


data class UpdateSoalResponse(
    val success: Boolean,
    val message: String,
    val error: String? = null
)



data class UpdateOpsiResponse(
    val success: Boolean,
    val message: String,
    val error: String? = null
)

/**
 * Request untuk hapus soal
 */




