package com.example.subtrackai.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FriendRequest(
    @SerialName("id") val id: String? = null,
    @SerialName("sender_id") val senderId: String = "",
    @SerialName("receiver_id") val receiverId: String = "",
    @SerialName("sender_name") val senderName: String = "",
    @SerialName("status") val status: String = "pending",
    @SerialName("created_at") val createdAt: String? = null
)
