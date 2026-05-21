package com.example.subtrackai.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class FriendRequest(
    @DocumentId val id: String = "",
    val fromId: String = "",
    val fromName: String = "",
    val toId: String = "",
    val status: String = "pending", // pending, accepted, rejected
    @ServerTimestamp val timestamp: Date? = null
)
