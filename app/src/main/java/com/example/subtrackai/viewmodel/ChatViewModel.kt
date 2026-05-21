package com.example.subtrackai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subtrackai.model.Subscription
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class ChatViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage("Hello! I'm your SubTrack Financial Consultant. How can I help you optimize your subscriptions today?", false))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    fun sendMessage(text: String, subscriptions: List<Subscription>) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(text, true)
        _messages.value = _messages.value + userMessage

        viewModelScope.launch {
            _isTyping.value = true
            delay(1500) // Simulate AI thinking
            
            val response = generateConsultantResponse(text.lowercase(), subscriptions)
            _messages.value = _messages.value + ChatMessage(response, false)
            _isTyping.value = false
        }
    }

    private fun generateConsultantResponse(query: String, subscriptions: List<Subscription>): String {
        val monthlyTotal = subscriptions.sumOf { if (it.billingCycle == "Yearly") it.price / 12.0 else it.price }
        
        return when {
            query.contains("save") || query.contains("optimization") || query.contains("advice") -> {
                if (monthlyTotal > 100) {
                    "Your monthly spend is quite high ($${"%.2f".format(monthlyTotal)}). I recommend auditing your streaming services. Many users pay for 3+ but only use 1 regularly. Try 'cycling' them month-by-month!"
                } else {
                    "You\u0027re doing well! To save further, look for 'Family Plans' or student discounts. Also, checking for annual billing can save you up to 20% on many services."
                }
            }
            query.contains("cancel") || query.contains("should i") -> {
                val expensive = subscriptions.maxByOrNull { it.price }
                if (expensive != null) {
                    "I see ${expensive.name} is your highest cost at $${expensive.price}. If you haven\u0027t used it in the last 14 days, it might be time to hit pause!"
                } else "You have a lean list! No immediate cancellations recommended."
            }
            query.contains("total") || query.contains("much") -> {
                "Your total monthly spend is $${"%.2f".format(monthlyTotal)}. Over a year, that\u0027s $${"%.2f".format(monthlyTotal * 12)}. Seeing it as a yearly number often helps in deciding what\u0027s truly worth it!"
            }
            query.contains("alternative") || query.contains("better way") -> {
                "For entertainment, consider free alternatives like Kanopy (via library card) or ad-supported tiers. For cloud storage, auditing duplicate services (like paying for both iCloud and Google One) is a quick win."
            }
            query.contains("hello") || query.contains("hi") -> {
                "Hi! I\u0027m ready to analyze your $${"%.2f".format(monthlyTotal)} monthly spending. What financial aspect shall we discuss?"
            }
            else -> "I\u0027m here as your financial consultant. I can analyze your subscriptions, suggest ways to save, or help you decide which services are truly providing value."
        }
    }
}
