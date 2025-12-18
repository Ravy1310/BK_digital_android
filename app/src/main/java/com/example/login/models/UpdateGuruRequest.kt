package com.example.login.models

import com.google.gson.annotations.SerializedName

data class UpdateGuruRequest(
    @SerializedName("id_guru")
    val id_guru: Int,

    @SerializedName("nama")
    val nama: String,

    @SerializedName("telepon")
    val telepon: String,

    @SerializedName("alamat")
    val alamat: String
)

data class UpdateGuruResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("error")
    val error: String? = null,

    @SerializedName("data")
    val data: GuruData? = null
)