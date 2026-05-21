package com.example.subtrackai.model

data class UserProfile(
    val uid: String = "",
    val fullName: String = "",
    val username: String = "",
    val email: String = "",
    val bio: String = "",
    val profileIcon: String = "Person",
    val isOnline: Boolean = false,
    val userStatus: String = "Online", // Online, Do Not Disturb, Asleep, Offline
    val showSubscriptions: Boolean = true,
    val friendsCount: Int = 0,
    val currency: String = "$"
)
