package com.example.subtrackai.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    @SerialName("id") val id: String? = null,
    @SerialName("post_id") val postId: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("author_name") val authorName: String = "",
    @SerialName("text") val text: String = "",
    @SerialName("profile_icon") val profileIcon: String = "Person",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("parent_comment_id") val parentCommentId: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
