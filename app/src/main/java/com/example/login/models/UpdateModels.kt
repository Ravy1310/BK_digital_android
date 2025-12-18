package com.example.login.models

data class UpdateSoalCompleteRequest(
    val id_soal: Int,
    val pertanyaan: String,
    val opsi_list: List<OpsiUpdateItem>
)

data class OpsiUpdateItem(
    val id_opsi: Int,
    val opsi: String,
    val bobot: Int
)

data class UpdateSoalCompleteResponse(
    val success: Boolean,
    val message: String,
    val error: String? = null
)