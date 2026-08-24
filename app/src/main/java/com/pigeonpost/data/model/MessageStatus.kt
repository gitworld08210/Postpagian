package com.pigeonpost.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class MessageStatus {
    FLYING,
    DELIVERED,
    LOST
}
