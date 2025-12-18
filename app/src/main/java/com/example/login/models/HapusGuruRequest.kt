package com.example.login.models

import com.google.gson.annotations.SerializedName

data class HapusGuruRequest(
    @SerializedName("id_guru")
    val id_guru: Int
)

data class HapusGuruResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("error")
    val error: String? = null
)