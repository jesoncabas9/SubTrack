package com.example.subtrackai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.subtrackai.model.Message
import com.example.subtrackai.model.UserProfile
import com.example.subtrackai.ui.theme.DeepPurple
import com.example.subtrackai.viewmodel.PeerChatViewModel
import com.example.subtrackai.viewmodel.SocialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeerChatScreen(
    socialViewModel: SocialViewModel,
    chatViewModel: PeerChatViewModel,
    isDarkMode: Boolean
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var selectedUser by remember { mutableStateOf<UserProfile?>(null) }
    
    val messages by chatViewModel.messages.collectAsState()
    val friends by socialViewModel.friends.collectAsState()

    Scaffold(
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize()) {
            if (selectedUser == null) {
                // User Search / Friend List
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { 
                            searchQuery = it
                            if (it.length > 2) {
                                socialViewModel.searchUsers(it) { results -> searchResults = results }
                            } else if (it.isEmpty()) {
                                searchResults = emptyList()
                            }
                        },
                        placeholder = { Text("Search users to chat...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = if (searchQuery.isEmpty()) "My Friends" else "Search Results",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val displayList = if (searchQuery.isEmpty()) friends else searchResults
                        items(displayList) { user ->
                            UserListItem(user) {
                                selectedUser = user
                                chatViewModel.startChat(user.uid)
                            }
                        }
                    }
                }
            } else {
                // Active Chat
                Column(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        color = Color(0xFFFFF3E0),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Be careful of sharing personal information and scammers.",
                                fontSize = 11.sp,
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        reverseLayout = false,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(messages) { message ->
                            ChatBubble(message = message, isMe = message.senderId != selectedUser?.uid)
                        }
                    }
                    
                    ChatInput { text ->
                        chatViewModel.sendMessage(selectedUser!!.uid, text)
                    }
                }
            }
        }
    }
}

@Composable
fun UserListItem(user: UserProfile, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = DeepPurple.copy(alpha = 0.1f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = DeepPurple)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(user.username, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ChatBubble(message: Message, isMe: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isMe) DeepPurple else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 0.dp,
                bottomEnd = if (isMe) 0.dp else 16.dp
            )
        ) {
            Text(text = message.text, modifier = Modifier.padding(12.dp), fontSize = 15.sp)
        }
    }
}

@Composable
fun ChatInput(onSendMessage: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Type a message...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSendMessage(text)
                        text = ""
                    }
                },
                colors = IconButtonDefaults.iconButtonColors(containerColor = DeepPurple, contentColor = Color.White)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}
