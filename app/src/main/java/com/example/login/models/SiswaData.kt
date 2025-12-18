package com.example.login.models
import com.google.gson.annotations.SerializedName

data class SiswaData(
    @SerializedName("id_siswa") val id_siswa: String,
    @SerializedName("nama") val nama: String,
    @SerializedName("kelas") val kelas: String,
    @SerializedName("tahun_masuk") val tahun_masuk: String,
    @SerializedName("jenis_kelamin") val jenis_kelamin: String,
    @SerializedName("created_at") val created_at: String? = null,
    @SerializedName("updated_at") val updated_at: String? = null
)

data class SiswaResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<SiswaData>? = null,
    @SerializedName("count") val count: Int = 0,
    @SerializedName("error") val error: String? = null
)
