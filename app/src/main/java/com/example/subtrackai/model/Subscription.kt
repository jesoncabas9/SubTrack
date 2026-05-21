package com.example.subtrackai.model

import com.google.firebase.firestore.DocumentId

data class Subscription(
    @DocumentId val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val billingCycle: String = "Monthly", // "Monthly" or "Yearly"
    val renewalDate: String = "",
    val category: String = "Streaming", // e.g., "Streaming", "Gaming", "Music", "Tools"
    val isTrial: Boolean = false
)
