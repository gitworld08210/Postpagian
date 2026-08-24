package com.pigeonpost.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String = "",
    @SerialName("sender_id")
    val senderId: String = "",
    @SerialName("receiver_id")
    val receiverId: String = "",
    val content: String = "",
    @SerialName("sent_at")
    val sentAt: Long = 0L,
    @SerialName("delivery_time")
    val deliveryTime: Long = 0L,
    val status: MessageStatus = MessageStatus.FLYING,
    @SerialName("sender_lat")
    val senderLat: Double = 0.0,
    @SerialName("sender_lng")
    val senderLng: Double = 0.0,
    @SerialName("receiver_lat")
    val receiverLat: Double = 0.0,
    @SerialName("receiver_lng")
    val receiverLng: Double = 0.0,
    @SerialName("pigeon_current_lat")
    val pigeonCurrentLat: Double = 0.0,
    @SerialName("pigeon_current_lng")
    val pigeonCurrentLng: Double = 0.0
)
