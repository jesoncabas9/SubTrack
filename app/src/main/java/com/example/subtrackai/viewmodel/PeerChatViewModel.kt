package com.example.subtrackai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subtrackai.model.Message
import com.example.subtrackai.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PeerChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    init {
        viewModelScope.launch {
            supabase.auth.sessionStatus.collect { status ->
                if (status !is SessionStatus.Authenticated) {
                    _messages.value = emptyList()
                }
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
        val message = Message(
            senderId = user.id,
            receiverId = receiverId,
            text = text
        )
        viewModelScope.launch {
            try {
                supabase.postgrest["messages"].insert(message)
                startChat(receiverId) 
            } catch (e: Exception) {
                Log.e("PeerChatViewModel", "Error sending message: ${e.message}", e)
            }
        }
    }
}
