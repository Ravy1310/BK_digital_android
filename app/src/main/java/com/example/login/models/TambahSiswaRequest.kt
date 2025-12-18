// TambahSiswaRequest.kt
package com.example.login.models

import com.google.gson.annotations.SerializedName

data class TambahSiswaRequest(
    @SerializedName("id_siswa") val id_siswa: String,
    @SerializedName("nama") val nama: String,
    @SerializedName("kelas") val kelas: String,
    @SerializedName("tahun_masuk") val tahun_masuk: String,
    @SerializedName("jenis_kelamin") val jenis_kelamin: String
)

data class TambahSiswaResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: SiswaData? = null,
    @SerializedName("error") val error: String? = null
)

