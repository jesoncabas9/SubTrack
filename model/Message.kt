package com.example.subtrackai.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    @SerialName("id") val id: String? = null,
    @SerialName("sender_id") val senderId: String = "",
    @SerialName("receiver_id") val receiverId: String = "",
    @SerialName("text") val content: String = "",
    @SerialName("timestamp") val timestamp: String? = null,
    @SerialName("is_read") val isRead: Boolean = false
)
