package com.example.login.models
// File: app/src/main/java/com/example/login/models/UpdateStatusGuruRequest.kt


import com.google.gson.annotations.SerializedName

data class UpdateStatusGuruRequest(
    @SerializedName("id_guru")
    val id_guru: Int,

    @SerializedName("action")
    val action: String = "ubah_status"
)