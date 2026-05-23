package com.example.subtrackai.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Subscription(
    @SerialName("id") val id: String? = null, // Nullable so DB generates UUID
    @SerialName("user_id") val userId: String? = null,
    @SerialName("name") val name: String = "",
    @SerialName("price") val price: Double = 0.0,
    @SerialName("billing_cycle") val billingCycle: String? = "Monthly",
    @SerialName("renewal_date") val renewalDate: String? = "",
    @SerialName("category") val category: String? = "Streaming",
    @SerialName("is_trial") val isTrial: Boolean = false
)
