package com.pigeonpost.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Conversation(
    val id: String = "",
    @SerialName("other_user_id")
    val otherUserId: String = "",
    @SerialName("other_user_name")
    val otherUserName: String = "",
    @SerialName("last_message")
    val lastMessage: String = "",
    @SerialName("last_message_time")
    val lastMessageTime: Long = 0L,
    @SerialName("unread_count")
    val unreadCount: Int = 0,
    @SerialName("last_message_status")
    val lastMessageStatus: MessageStatus = MessageStatus.DELIVERED
)
