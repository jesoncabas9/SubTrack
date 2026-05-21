package com.example.subtrackai.viewmodel

import androidx.lifecycle.ViewModel
import com.example.subtrackai.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PeerChatViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    fun startChat(otherUserId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        
        // Listen to messages where I am sender or receiver
        // (Simplified logic: in production use a composite ID or specific chat document)
        firestore.collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                val allMessages = snapshot?.toObjects(Message::class.java) ?: emptyList()
                _messages.value = allMessages.filter {
                    (it.senderId == currentUserId && it.receiverId == otherUserId) ||
                    (it.senderId == otherUserId && it.receiverId == currentUserId)
                }
            }
    }

    fun sendMessage(receiverId: String, text: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val message = Message(
            senderId = currentUserId,
            receiverId = receiverId,
            text = text
        )
        firestore.collection("messages").add(message)
    }
}
