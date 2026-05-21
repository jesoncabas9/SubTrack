package com.example.subtrackai.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Comment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val authorName: String = "",
    val profileIcon: String = "Person",
    val text: String = "",
    @ServerTimestamp val timestamp: Date? = null
)
