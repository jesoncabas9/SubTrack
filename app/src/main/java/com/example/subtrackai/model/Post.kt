package com.example.subtrackai.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Post(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val content: String = "",
    @ServerTimestamp val timestamp: Date? = null,
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val commentsEnabled: Boolean = true,
    val isShared: Boolean = false,
    val originalPostId: String? = null,
    val originalAuthorName: String? = null,
    val profilePost: Boolean = false // True if posted directly to profile
)
