// File: app/src/main/java/com/example/login/models/DashboardResponse.kt
package com.example.login.models

import com.google.gson.annotations.SerializedName

data class DashboardResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: DashboardData
)

data class DashboardData(
    @SerializedName("jumlah_siswa")
    val jumlahSiswa: Int,

    @SerializedName("jumlah_guru")
    val jumlahGuru: Int,

    @SerializedName("jumlah_tes")
    val jumlahTes: Int,

    @SerializedName("tes_terpopuler")
    val tesTerpopuler: List<TesTerpopuler>
)

data class TesTerpopuler(
    @SerializedName("nama_tes")
    val namaTes: String,

    @SerializedName("jumlah_siswa")
    val jumlahSiswa: Int
)