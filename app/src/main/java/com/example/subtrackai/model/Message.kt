package com.example.subtrackai.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Message(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    @ServerTimestamp val timestamp: Date? = null
)
