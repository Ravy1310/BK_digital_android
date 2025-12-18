package com.example.login.models

import com.google.gson.annotations.SerializedName

data class HapusSoalResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: HapusSoalData?,

    @SerializedName("error")
    val error: String?
)

data class HapusSoalData(
    @SerializedName("id_soal")
    val idSoal: Int,

    @SerializedName("id_tes")
    val idTes: Int,

    @SerializedName("pertanyaan")
    val pertanyaan: String,

    @SerializedName("deleted_opsi_count")
    val deletedOpsiCount: Int
)