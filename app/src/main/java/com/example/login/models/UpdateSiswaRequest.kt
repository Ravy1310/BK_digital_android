// UpdateSiswaRequest.kt
package com.example.login.models
import com.google.gson.annotations.SerializedName
data class UpdateSiswaRequest(
    @SerializedName("id_siswa") val id_siswa: String,
    @SerializedName("nama") val nama: String,
    @SerializedName("kelas") val kelas: String,
    @SerializedName("tahun_masuk") val tahun_masuk: String,
    @SerializedName("jenis_kelamin") val jenis_kelamin: String
)



data class UpdateSiswaResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: SiswaData?,
    @SerializedName("error") val error: String?
)



data class SiswaDetailResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: SiswaData?,
    @SerializedName("count") val count: Int?,
    @SerializedName("error") val error: String?
)