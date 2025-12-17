// File: app/src/main/java/com/example/login/models/UpdateStatusGuruResponse.kt
package com.example.login.models

import com.google.gson.annotations.SerializedName

data class UpdateStatusGuruResponse(
    @SerializedName("status")
    val status: String,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: UpdateStatusData? = null
)

data class UpdateStatusData(
    @SerializedName("new_status")
    val newStatus: String,

    @SerializedName("nama_guru")
    val namaGuru: String
)