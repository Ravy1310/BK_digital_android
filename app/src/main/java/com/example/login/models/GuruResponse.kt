package com.example.login.models


import com.google.gson.annotations.SerializedName

data class GuruResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: GuruData
)

data class GuruData(
    @SerializedName("statistik")
    val statistik: StatistikGuru,

    @SerializedName("daftar_guru")
    val daftarGuru: List<Guru>
)

data class StatistikGuru(
    @SerializedName("total_guru")
    val totalGuru: Int,

    @SerializedName("akun_aktif")
    val akunAktif: Int,

    @SerializedName("akun_nonaktif")
    val akunNonaktif: Int
)

data class Guru(
    @SerializedName("id_guru")
    val idGuru: Int,

    @SerializedName("nama")
    val nama: String,

    @SerializedName("telepon")
    val telepon: String,

    @SerializedName("alamat")
    val alamat: String,

    @SerializedName("status")
    val status: String, // "Aktif" atau "Nonaktif"

    @SerializedName("tanggal_daftar")
    val tanggalDaftar: String,

    @SerializedName("username")
    val username: String?,

    @SerializedName("email")
    val email: String?
)