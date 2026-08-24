package com.pigeonpost.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String = "",
    val email: String = "",
    @SerialName("display_name")
    val displayName: String = "",
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("last_latitude")
    val lastLatitude: Double = 0.0,
    @SerialName("last_longitude")
    val lastLongitude: Double = 0.0
)
