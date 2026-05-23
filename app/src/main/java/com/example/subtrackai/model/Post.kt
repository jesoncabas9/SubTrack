package com.example.subtrackai.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Post(
    @SerialName("id") val id: String? = null, // Nullable so DB generates UUID
    @SerialName("user_id") val userId: String = "",
    @SerialName("author_name") val authorName: String = "",
    @SerialName("content") val content: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("likes") val likes: Int = 0,
    @SerialName("liked_by") val likedBy: List<String> = emptyList(),
    @SerialName("comments_enabled") val commentsEnabled: Boolean = true,
    @SerialName("shared") val shared: Boolean = false,
    @SerialName("original_post_id") val originalPostId: String? = null,
    @SerialName("original_author_name") val originalAuthorName: String? = null,
    @SerialName("original_author_id") val originalAuthorId: String? = null,
    @SerialName("profile_post") val profilePost: Boolean = false
)
