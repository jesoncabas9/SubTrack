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
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.subtrackai.supabase
import com.example.subtrackai.ui.theme.DeepPurple
import com.example.subtrackai.viewmodel.AuthViewModel
import com.example.subtrackai.viewmodel.SocialViewModel
import com.example.subtrackai.viewmodel.FeedViewModel
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    socialViewModel: SocialViewModel,
    feedViewModel: FeedViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    targetUid: String? = null
) {
    val currentUserId = supabase.auth.currentUserOrNull()?.id
    val isOwner = targetUid == null || targetUid == currentUserId
    
    val profile by if (isOwner) socialViewModel.userProfile.collectAsState() else {
        val p = remember { mutableStateOf<com.example.subtrackai.model.UserProfile?>(null) }
        LaunchedEffect(targetUid) {
            targetUid?.let { socialViewModel.loadVisitorProfile(it) { profile -> p.value = profile } }
        }
        p
    }

    val friends by if (isOwner) socialViewModel.friends.collectAsState() else socialViewModel.visitorFriends.collectAsState()
    
    LaunchedEffect(targetUid) {
        if (!isOwner && targetUid != null) {
            // Future: make public if direct refresh is needed, currently synced in refresAll
        }
    }

    val allPosts by feedViewModel.posts.collectAsState()
    val profilePosts = remember(allPosts, profile) {
        allPosts.filter { it.userId == profile?.uid }
    }

    val isRefreshing by socialViewModel.isRefreshing.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    var showCommentSheet by remember { mutableStateOf(false) }
    var selectedPostId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isOwner) "My Profile" else profile?.username ?: "Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { (targetUid ?: currentUserId)?.let { socialViewModel.refreshAll(it) } },
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        com.example.subtrackai.ui.components.ProfileAvatar(
                    iconName = profile?.profileIcon,
                    avatarUrl = profile?.avatarUrl,
                    modifier = Modifier.size(100.dp),
                    tint = DeepPurple
                )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(profile?.fullName ?: "User", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text("@${profile?.username ?: "user"}", color = Color.Gray, fontSize = 16.sp)
                        Text(profile?.email ?: "", color = Color.Gray, fontSize = 14.sp)

                        Spacer(modifier = Modifier.height(4.dp))

                        if (!isOwner && profile != null) {
                            val isFriend = friends.any { it.uid == currentUserId }
                            val hasSentRequest = socialViewModel.friendRequests.collectAsState().value.any { it.receiverId == profile?.uid && it.status == "pending" }
                            
                            Row(modifier = Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (isFriend) {
                                    Button(onClick = { onNavigateToChat(profile!!.uid) }, colors = ButtonDefaults.buttonColors(containerColor = DeepPurple)) {
                                        Icon(Icons.Default.Message, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Message")
                                    }
                                    OutlinedButton(onClick = { 
                                        scope.launch { socialViewModel.unfriendUser(profile!!.uid) }
                                    }) {
                                        Text("Unfriend")
                                    }
                                } else if (hasSentRequest) {
                                    OutlinedButton(onClick = { 
                                        scope.launch { socialViewModel.cancelFriendRequest(profile!!.uid) }
                                    }) {
                                        Text("Request Sent")
                                    }
                                } else {
                                    Button(onClick = { 
                                        profile?.let { scope.launch { socialViewModel.sendFriendRequest(it) } }
                                    }, colors = ButtonDefaults.buttonColors(containerColor = DeepPurple)) {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Add Friend")
                                    }
                                }
                            }
                        } else if (isOwner) {
                            Button(
                                onClick = onNavigateToSettings,
                                modifier = Modifier.padding(top = 16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DeepPurple)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Edit Profile")
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ProfileStat("Friends", "${friends.size}")
                            ProfileStat("Posts", "${profilePosts.size}")
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            "Recent Posts", 
                            modifier = Modifier.fillMaxWidth(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                if (profilePosts.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text("No posts yet", color = Color.Gray)
                        }
                    }
                } else {
                    items(profilePosts) { post ->
                        com.example.subtrackai.ui.screens.PostItem(
                            post = post,
                            onLikeToggle = { feedViewModel.toggleLike(post.id ?: "") },
                            onProfileClick = { },
                            isOwner = isOwner,
                            onDelete = { feedViewModel.deletePost(post.id ?: "") },
                            onEdit = { feedViewModel.editPost(post.id ?: "", it) },
                            onCommentClick = { 
                                selectedPostId = post.id
                                showCommentSheet = true
                            },
                            onShare = { },
                            onToggleComments = { },
                            showFeedTag = false
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
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
                    currentUserId = currentUserId ?: "",
                    viewModel = feedViewModel,
                    socialViewModel = socialViewModel,
                    onDismiss = { 
                        showCommentSheet = false
                        selectedPostId = null
                    }
                )
            }
        }
    }
}

@Composable
fun ProfileStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = DeepPurple)
        Text(label, color = Color.Gray, fontSize = 14.sp)
    }
}
