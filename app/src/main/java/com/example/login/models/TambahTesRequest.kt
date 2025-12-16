package com.example.login.models
// File: app/src/main/java/com/example/login/models/TambahTesRequest.kt


data class TambahTesRequest(
    val nama_tes: String,
    val deskripsi_tes: String,
    val csv_content: String
)