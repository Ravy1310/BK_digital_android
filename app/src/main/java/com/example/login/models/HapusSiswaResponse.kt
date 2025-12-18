package com.example.login.models

import com.google.gson.annotations.SerializedName

data class HapusSiswaResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: HapusSiswaData?,
    @SerializedName("error") val error: String?
)

data class HapusSiswaData(
    @SerializedName("id_siswa") val id_siswa: String?
)