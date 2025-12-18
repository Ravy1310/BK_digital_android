package com.example.login.models

import com.google.gson.annotations.SerializedName

data class HapusSiswaRequest(
    @SerializedName("id_siswa") val id_siswa: String
)