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
                title = { Text(if (profilePost) "Post to Profile" else "Create Post", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (content.isNotBlank()) {
                                feedViewModel.createPost(content, authorName, profilePost)
                                onBack()
                            }
                        },
                        enabled = content.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepPurple),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Post", color = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = DeepPurple.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(ProfileIcons.getIcon(userProfile?.profileIcon), contentDescription = null, tint = DeepPurple)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(authorName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("What's on your mind?", fontSize = 18.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 18.sp)
            )

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

            // Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ToolButton(icon = Icons.Default.Image, label = "Photo", tint = Color(0xFF4CAF50)) {
                    android.widget.Toast.makeText(context, "Photo feature coming soon!", android.widget.Toast.LENGTH_SHORT).show()
                }
                ToolButton(icon = Icons.Default.PersonAdd, label = "Tag", tint = Color(0xFF2196F3)) {
                    android.widget.Toast.makeText(context, "Tagging feature coming soon!", android.widget.Toast.LENGTH_SHORT).show()
                }
                ToolButton(icon = Icons.Default.EmojiEmotions, label = "Feeling", tint = Color(0xFFFFC107)) {
                    android.widget.Toast.makeText(context, "Feelings coming soon!", android.widget.Toast.LENGTH_SHORT).show()
                }
                ToolButton(icon = Icons.Default.LocationOn, label = "Check In", tint = Color(0xFFF44336)) {
                    android.widget.Toast.makeText(context, "Check-in coming soon!", android.widget.Toast.LENGTH_SHORT).show()
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
