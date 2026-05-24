package com.example.subtrackai.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.subtrackai.ui.theme.DeepPurple
import com.example.subtrackai.util.ProfileIcons
import com.example.subtrackai.viewmodel.AuthViewModel
import com.example.subtrackai.viewmodel.FeedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    feedViewModel: FeedViewModel,
    authViewModel: AuthViewModel,
    socialViewModel: com.example.subtrackai.viewmodel.SocialViewModel,
    onBack: () -> Unit,
    profilePost: Boolean = false
) {
    var content by remember { mutableStateOf("") }
    val userProfile by socialViewModel.userProfile.collectAsState()
    val authorName = userProfile?.username ?: "User"
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { 
                    Column {
                        Text(
                            text = if (profilePost) "Post to Profile" else "Share to Feed", 
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = if (profilePost) "Visible to visitors" else "Visible to everyone",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (content.isNotBlank()) {
                                feedViewModel.createPost(
                                    content = content, 
                                    authorName = authorName, 
                                    profilePost = profilePost,
                                    profileIcon = userProfile?.profileIcon,
                                    avatarUrl = userProfile?.avatarUrl
                                )
                                onBack()
                            }
                        },
                        enabled = content.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Post", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.example.subtrackai.ui.components.ProfileAvatar(
                    iconName = userProfile?.profileIcon,
                    avatarUrl = userProfile?.avatarUrl,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = userProfile?.fullName ?: "User", 
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "@$authorName", 
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { 
                    Text(
                        "What's happening?", 
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Professional Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = { /* Photo */ }) {
                    Icon(Icons.Default.Image, contentDescription = "Photo", tint = Color(0xFF4CAF50))
                }
                IconButton(onClick = { /* Tag */ }) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Tag", tint = Color(0xFF2196F3))
                }
                IconButton(onClick = { /* Feeling */ }) {
                    Icon(Icons.Default.EmojiEmotions, contentDescription = "Feeling", tint = Color(0xFFFFC107))
                }
                IconButton(onClick = { /* Location */ }) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Check In", tint = Color(0xFFF44336))
                }
            }
        }
    }
}

@Composable
fun ToolButton(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(icon, contentDescription = label, tint = tint)
        Text(label, fontSize = 10.sp, color = Color.Gray)
    }
}
