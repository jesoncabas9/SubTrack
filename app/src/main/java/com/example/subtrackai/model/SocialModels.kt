package com.example.subtrackai.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommentInsert(
    @SerialName("post_id") val postId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("author_name") val authorName: String,
    @SerialName("profile_icon") val profileIcon: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("text") val text: String,
    @SerialName("parent_comment_id") val parentCommentId: String? = null
)

@Serializable
data class CommentUpdate(
    @SerialName("text") val text: String
)

@Serializable
data class PostUpdate(
    @SerialName("content") val content: String? = null,
    @SerialName("comments_enabled") val commentsEnabled: Boolean? = null
)

@Serializable
data class ProfileUpdate(
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("profile_icon") val profileIcon: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_online") val isOnline: Boolean? = null,
    @SerialName("currency") val currency: String? = null,
    @SerialName("user_status") val userStatus: String? = null
)

@Serializable
data class FriendRequestInsert(
    @SerialName("sender_id") val senderId: String,
    @SerialName("receiver_id") val receiverId: String,
    @SerialName("sender_name") val senderName: String,
    @SerialName("status") val status: String = "pending"
)

@Serializable
data class FriendRequestUpdate(
    @SerialName("status") val status: String
)

@Serializable
data class PostInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("author_name") val authorName: String,
    @SerialName("content") val content: String,
    @SerialName("profile_icon") val profileIcon: String? = "Person",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("shared") val shared: Boolean = false,
    @SerialName("original_post_id") val originalPostId: String? = null,
    @SerialName("original_author_name") val originalAuthorName: String? = null,
    @SerialName("original_author_id") val originalAuthorId: String? = null,
    @SerialName("profile_post") val profilePost: Boolean = false
)

@Serializable
data class MessageInsert(
    @SerialName("sender_id") val senderId: String,
    @SerialName("receiver_id") val receiverId: String,
    @SerialName("text") val text: String
)
