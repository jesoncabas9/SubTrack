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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.subtrackai.model.UserProfile
import com.example.subtrackai.ui.components.EditProfileDialog
import com.example.subtrackai.ui.theme.DeepPurple
import com.example.subtrackai.util.ProfileIcons
import com.example.subtrackai.viewmodel.AuthViewModel
import com.example.subtrackai.viewmodel.SocialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    socialViewModel: SocialViewModel,
    onBack: () -> Unit,
    onNavigateToCreatePost: () -> Unit,
    onNavigateToProfile: (String) -> Unit = {},
    visitorUserId: String? = null
) {
    val currentUserProfile by socialViewModel.userProfile.collectAsState()
    var visitorProfile by remember { mutableStateOf<UserProfile?>(null) }
    val myFriends by socialViewModel.friends.collectAsState()
    val visitorFriends by socialViewModel.visitorFriends.collectAsState()
    
    val isOwner = visitorUserId == null || visitorUserId == currentUserProfile?.uid
    val profile = if (isOwner) currentUserProfile else visitorProfile
    val friends = if (isOwner) myFriends else visitorFriends
    
    val feedViewModel: com.example.subtrackai.viewmodel.FeedViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val allPosts by feedViewModel.posts.collectAsState()
    val profilePosts = allPosts.filter { 
        it.userId == (visitorUserId ?: currentUserProfile?.uid)
    }

    val sheetState = rememberModalBottomSheetState()
    var showCommentSheet by remember { mutableStateOf(false) }
    var selectedPostId by remember { mutableStateOf<String?>(null) }
    
    var showShareSheet by remember { mutableStateOf(false) }
    var selectedSharePost by remember { mutableStateOf<com.example.subtrackai.model.Post?>(null) }
    
    var showEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(visitorUserId) {
        if (visitorUserId != null) {
            socialViewModel.loadVisitorProfile(visitorUserId) {
                visitorProfile = it
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text(if (isOwner) "My Profile" else profile?.username ?: "Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(100.dp),
                        shape = CircleShape,
                        color = DeepPurple.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(ProfileIcons.getIcon(profile?.profileIcon), contentDescription = null, modifier = Modifier.size(60.dp), tint = DeepPurple)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(profile?.fullName ?: "User", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    Text("@${profile?.username ?: "user"}", color = Color.Gray, fontSize = 16.sp)
                    Text(profile?.email ?: "", color = Color.Gray, fontSize = 14.sp)

                    Spacer(modifier = Modifier.height(4.dp))

                    if (profile?.uid?.isNotEmpty() == true) {
                        Surface(
                            color = if (profile.userStatus == "Online") Color(0xFF4CAF50).copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (profile.userStatus == "Online") Color(0xFF4CAF50) else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = profile.userStatus,
                                    fontSize = 12.sp,
                                    color = if (profile.userStatus == "Online") Color(0xFF4CAF50) else Color.Gray
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(profile?.bio ?: "No bio yet.", modifier = Modifier.padding(horizontal = 32.dp))
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileStat("Friends", "${friends.size}")
                        ProfileStat("Posts", "${profilePosts.size}")
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    
                    if (isOwner) {
                        Button(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepPurple)
                        ) {
                            Text("Edit Profile")
                        }
                    } else {
                        val friendRequests by socialViewModel.friendRequests.collectAsState()
                        // Use deterministic ID logic for local check
                        val currentUid = currentUserProfile?.uid ?: ""
                        val targetUid = profile?.uid ?: ""
                        val requestId = if (currentUid < targetUid) "${currentUid}_${targetUid}" else "${targetUid}_${currentUid}"
                        
                        val request = friendRequests.find { it.id == requestId || (it.fromId == currentUid && it.toId == targetUid) }
                        val isFriend = myFriends.any { it.uid == targetUid }

                        if (isFriend) {
                            Button(
                                onClick = { /* Message flow maybe? */ },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                            ) {
                                Text("Friends")
                            }
                        } else if (request != null && request.status == "pending") {
                            Button(
                                onClick = { socialViewModel.cancelFriendRequest(profile?.uid ?: "") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                            ) {
                                Text("Cancel Request")
                            }
                        } else {
                            Button(
                                onClick = { 
                                    profile?.let { 
                                        socialViewModel.sendFriendRequest(it)
                                    } 
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DeepPurple)
                            ) {
                                Text("Add Friend")
                            }
                        }
                    }
                }

                if (isOwner) {
                    HorizontalDivider()
                    Text(
                        "Post to Profile", 
                        modifier = Modifier.padding(16.dp), 
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable { onNavigateToCreatePost() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = DeepPurple)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Write something on your profile...", color = Color.Gray)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Text(
                        "My Posts", 
                        modifier = Modifier.padding(16.dp), 
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                } else {
                    HorizontalDivider()
                    Text(
                        "Posts", 
                        modifier = Modifier.padding(16.dp), 
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            items(profilePosts) { post ->
                val currentUserId = currentUserProfile?.uid ?: ""
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    PostItem(
                        post = post,
                        onLikeToggle = { feedViewModel.toggleLike(post.id) },
                        onProfileClick = { },
                        onOriginalProfileClick = { authorId -> 
                            if (authorId != (visitorUserId ?: currentUserProfile?.uid)) {
                                onNavigateToProfile(authorId)
                            }
                        },
                        isOwner = post.userId == currentUserId,
                        onDelete = { feedViewModel.deletePost(post.id) },
                        onEdit = { newContent -> feedViewModel.editPost(post.id, newContent) },
                        onCommentClick = { 
                            selectedPostId = post.id
                            showCommentSheet = true
                        },
                        onShare = { 
                            selectedSharePost = post
                            showShareSheet = true
                        },
                        onToggleComments = { feedViewModel.editPost(post.id, if (post.commentsEnabled) "OFF" else "ON") },
                        showFeedTag = true
                    )
                }
            }
        }

        if (showCommentSheet && selectedPostId != null) {
            ModalBottomSheet(
                onDismissRequest = { 
                    showCommentSheet = false
                    selectedPostId = null
                },
                sheetState = sheetState,
                dragHandle = { BottomSheetDefaults.DragHandle() },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                CommentSheetContent(
                    postId = selectedPostId!!,
                    currentUserId = currentUserProfile?.uid ?: "",
                    viewModel = feedViewModel,
                    socialViewModel = socialViewModel,
                    onDismiss = { 
                        showCommentSheet = false
                        selectedPostId = null
                    }
                )
            }
        }

        if (showShareSheet && selectedSharePost != null) {
            ModalBottomSheet(
                onDismissRequest = { 
                    showShareSheet = false
                    selectedSharePost = null
                },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                ShareSheetContent(
                    post = selectedSharePost!!,
                    socialViewModel = socialViewModel,
                    onDismiss = { 
                        showShareSheet = false
                        selectedSharePost = null
                    }
                )
            }
        }

        if (showEditDialog && currentUserProfile != null) {
            EditProfileDialog(
                userProfile = currentUserProfile!!,
                onDismiss = { showEditDialog = false },
                onConfirm = { name, uname, bio, icon ->
                    socialViewModel.updateProfile(name, uname, bio, icon)
                    showEditDialog = false
                }
            )
        }
    }
}

@Composable
fun ProfileStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(label, fontSize = 14.sp, color = Color.Gray)
    }
}
