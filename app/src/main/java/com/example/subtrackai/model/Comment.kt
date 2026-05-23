package com.example.subtrackai.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    @SerialName("id") val id: String? = null,
    @SerialName("post_id") val postId: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("author_name") val authorName: String = "",
    @SerialName("content") val content: String = "",
    @SerialName("profile_icon") val profileIcon: String = "Person",
    @SerialName("parent_comment_id") val parentCommentId: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
