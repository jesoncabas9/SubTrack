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
    val parentCommentId: String? = null, // ID of the comment this is replying to
    @ServerTimestamp val timestamp: Date? = null
)
