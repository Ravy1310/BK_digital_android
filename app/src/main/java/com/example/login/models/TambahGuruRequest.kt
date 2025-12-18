package com.example.login.models

import com.google.gson.annotations.SerializedName

data class TambahGuruRequest(
    @SerializedName("nama")
    val nama: String,

    @SerializedName("telepon")
    val telepon: String,

    @SerializedName("alamat")
    val alamat: String
)

// TambahGuruResponse.kt
data class TambahGuruResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String?,

    @SerializedName("error")
    val error: String?,

    @SerializedName("data")
    val data: GuruData? = null
)
