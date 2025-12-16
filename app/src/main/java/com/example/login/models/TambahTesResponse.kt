package com.example.login.models
// File: app/src/main/java/com/example/login/models/TambahTesResponse.kt

data class TambahTesResponse(
    val status: String,
    val message: String,
    val tes_id: Int? = null,
    val nama_tes: String? = null
)