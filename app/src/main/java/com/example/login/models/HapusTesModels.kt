package com.example.login.models
// File: app/src/main/java/com/example/login/models/HapusTesModels.kt

data class HapusTesRequest(
    val id_tes: Int  // Gunakan underscore untuk match dengan API PHP
)

data class HapusTesResponse(
    val success: Boolean,
    val message: String,
    val data: HapusTesData?
)

data class HapusTesData(
    val id_tes: Int,
    val deleted_at: String
)