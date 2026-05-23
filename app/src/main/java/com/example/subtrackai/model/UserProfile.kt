package com.example.subtrackai.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    @SerialName("id") val uid: String = "",
    @SerialName("full_name") val fullName: String = "",
    @SerialName("username") val username: String = "",
    @SerialName("email") val email: String = "",
    @SerialName("bio") val bio: String = "",
    @SerialName("profile_icon") val profileIcon: String = "Person",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_online") val isOnline: Boolean = false,
    @SerialName("user_status") val userStatus: String = "Online", // Online, Do Not Disturb, Asleep, Offline
    @SerialName("show_subscriptions") val showSubscriptions: Boolean = true,
    @SerialName("friends_count") val friendsCount: Int = 0,
    @SerialName("currency") val currency: String = "$"
)
