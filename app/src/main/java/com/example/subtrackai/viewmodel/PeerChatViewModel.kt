package com.example.subtrackai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subtrackai.model.Message
import com.example.subtrackai.model.MessageInsert
import com.example.subtrackai.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

class PeerChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    init {
        viewModelScope.launch {
            supabase.auth.sessionStatus.collect { status ->
                if (status !is SessionStatus.Authenticated) {
                    _messages.value = emptyList()
                }
            }
        }
    }

    fun refreshChat(otherUserId: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val user = supabase.auth.currentUserOrNull() ?: return@launch
                val currentUserId = user.id
                
                val messagesList = supabase.postgrest["messages"]
                    .select {
                        filter {
                            or {
                                and {
                                    eq("sender_id", currentUserId)
                                    eq("receiver_id", otherUserId)
                                }
                                and {
                                    eq("sender_id", otherUserId)
                                    eq("receiver_id", currentUserId)
                                }
                            }
                        }
                        order("created_at", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                    }
                    .decodeList<com.example.subtrackai.model.Message>()
                
                _messages.value = messagesList
            } catch (e: Exception) {
                android.util.Log.e("PeerChatViewModel", "Error refreshing chat", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun startChat(receiverId: String) {
        val user = supabase.auth.currentUserOrNull() ?: return
        val currentUserId = user.id
        
        viewModelScope.launch {
            try {
                val chatMessages = supabase.postgrest["messages"]
                    .select {
                        filter {
                            or {
                                and {
                                    eq("sender_id", currentUserId)
                                    eq("receiver_id", receiverId)
                                }
                                and {
                                    eq("sender_id", receiverId)
                                    eq("receiver_id", currentUserId)
                                }
                            }
                        }
                        order("created_at", Order.ASCENDING)
                    }
                    .decodeList<Message>()
                _messages.value = chatMessages
            } catch (e: Exception) {
                Log.e("PeerChatViewModel", "Error fetching messages: ${e.message}", e)
            }
        }
    }

    fun sendMessage(receiverId: String, text: String) {
        val user = supabase.auth.currentUserOrNull() ?: return
        val message = MessageInsert(
            senderId = user.id,
            receiverId = receiverId,
            text = text
        )
        viewModelScope.launch {
            try {
                supabase.postgrest["messages"].insert(Json.encodeToJsonElement(message))
                startChat(receiverId) 
            } catch (e: Exception) {
                Log.e("PeerChatViewModel", "Error sending message: ${e.message}", e)
            }
        }
    }
}
