package com.example.subtrackai.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.subtrackai.model.Comment
import com.example.subtrackai.model.Post
import com.example.subtrackai.supabase
import com.example.subtrackai.viewmodel.FeedViewModel
import com.example.subtrackai.viewmodel.SocialViewModel
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    socialViewModel: SocialViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateToCreatePost: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToMyProfile: () -> Unit,
    isDarkMode: Boolean
) {
    val posts by viewModel.feedPosts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val currentUserId = supabase.auth.currentUserOrNull()?.id
    
    val userProfile by socialViewModel.userProfile.collectAsState()

    val sheetState = rememberModalBottomSheetState()
    var showCommentSheet by remember { mutableStateOf(false) }
    var selectedPostId by remember { mutableStateOf<String?>(null) }
    
    var showShareSheet by remember { mutableStateOf(false) }
    var selectedSharePost by remember { mutableStateOf<Post?>(null) }

    val pullToRefreshState = rememberPullToRefreshState()

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshPosts() },
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = { Text("Search posts or people...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Search")
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                )

                // Post Creation Trigger
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { onNavigateToCreatePost() },
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        com.example.subtrackai.ui.components.ProfileAvatar(
                            iconName = userProfile?.profileIcon,
                            avatarUrl = userProfile?.avatarUrl,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                "Share your thoughts...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(onClick = { /* Photo trigger */ }) {
                            Icon(
                                Icons.Default.Image, 
                                contentDescription = null, 
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                if (posts.isEmpty() && !isRefreshing) {
                    // Professional Shimmer Loading
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(5) {
                            ShimmerPostItem()
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(posts) { post ->
                            PostItem(
                                post = post, 
                                onLikeToggle = { post.id?.let { viewModel.toggleLike(it) } },
                                onProfileClick = { onNavigateToProfile(post.userId) },
                                onOriginalProfileClick = { authorId -> onNavigateToProfile(authorId) },
                                isOwner = post.userId == currentUserId,
                                onDelete = { post.id?.let { viewModel.deletePost(it) } },
                                onEdit = { newContent -> post.id?.let { viewModel.editPost(it, newContent) } },
                                onCommentClick = { 
                                    selectedPostId = post.id
                                    showCommentSheet = true 
                                },
                                onShare = { 
                                    selectedSharePost = post
                                    showShareSheet = true
                                },
                                onToggleComments = { post.id?.let { viewModel.editPost(it, if (post.commentsEnabled) "OFF" else "ON") } },
                                showFeedTag = true
                            )
                        }
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
                    viewModel = viewModel,
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
    }
}

@Composable
fun ShareSheetContent(post: Post, socialViewModel: SocialViewModel, onDismiss: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var friends by remember { mutableStateOf<List<com.example.subtrackai.model.UserProfile>>(emptyList()) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        socialViewModel.friends.collect { friends = it }
    }

    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp).padding(16.dp)) {
        Text("Share Post", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            ShareOption(Icons.Default.History, "My Profile") {
                scope.launch { socialViewModel.sharePostToProfile(post) }
                onDismiss()
            }
            ShareOption(Icons.Default.ContentCopy, "Copy Link") {
                // Clipboard logic
                onDismiss()
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Send to Friends", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(friends) { friend ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { 
                        scope.launch { socialViewModel.sharePostInChat(post, friend.uid) }
                        onDismiss()
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.example.subtrackai.ui.components.ProfileAvatar(
                        iconName = friend.profileIcon,
                        avatarUrl = friend.avatarUrl,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(friend.username, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun ShareOption(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(50.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 12.sp)
    }
}

@Composable
fun CommentSheetContent(postId: String, currentUserId: String, viewModel: FeedViewModel, socialViewModel: SocialViewModel, onDismiss: () -> Unit) {
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var text by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<Comment?>(null) }
    val userProfile by socialViewModel.userProfile.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(postId) {
        viewModel.getComments(postId) { comments = it }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .padding(horizontal = 16.dp)
            .imePadding()
    ) {
        Text(
            "Comments", 
            style = MaterialTheme.typography.titleLarge, 
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        
        // Input at the top for guaranteed visibility
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Column {
                if (replyingTo != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Replying to ${replyingTo!!.authorName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(16.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp))
                        }
                    }
                }
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.example.subtrackai.ui.components.ProfileAvatar(
                        iconName = userProfile?.profileIcon,
                        avatarUrl = userProfile?.avatarUrl,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    TextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text("Add a comment...", fontSize = 14.sp) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        maxLines = 3
                    )
                    IconButton(
                        onClick = {
                            if (text.isNotBlank()) {
                                scope.launch {
                                    socialViewModel.addComment(
                                        postId = postId, 
                                        content = text, 
                                        profileIcon = userProfile?.profileIcon ?: "Person",
                                        avatarUrl = userProfile?.avatarUrl,
                                        parentCommentId = replyingTo?.id
                                    )
                                    text = ""
                                    replyingTo = null
                                    viewModel.getComments(postId) { comments = it }
                                }
                            }
                        },
                        enabled = text.isNotBlank()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send, 
                            contentDescription = "Post", 
                            tint = if (text.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val topLevelComments = comments.filter { it.parentCommentId == null }
            if (topLevelComments.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        Text("No comments yet", color = Color.Gray)
                    }
                }
            } else {
                items(topLevelComments) { comment ->
                    CommentItem(
                        comment = comment,
                        isOwner = comment.userId == currentUserId,
                        onDelete = { 
                            scope.launch {
                                socialViewModel.deleteComment(postId, comment.id!!)
                                viewModel.getComments(postId) { comments = it }
                            }
                        },
                        onEdit = { newText ->
                            scope.launch {
                                socialViewModel.editComment(postId, comment.id!!, newText)
                                viewModel.getComments(postId) { comments = it }
                            }
                        },
                        onReply = { replyingTo = comment }
                    )
                    
                    val replies = comments.filter { it.parentCommentId == comment.id && it.parentCommentId != null }
                    replies.forEach { reply ->
                        Box(modifier = Modifier.padding(start = 48.dp)) {
                            CommentItem(
                                comment = reply,
                                isOwner = reply.userId == currentUserId,
                                onDelete = { 
                                    scope.launch {
                                        socialViewModel.deleteComment(postId, reply.id!!)
                                        viewModel.getComments(postId) { comments = it }
                                    }
                                },
                                onEdit = { newText ->
                                    scope.launch {
                                        socialViewModel.editComment(postId, reply.id!!, newText)
                                        viewModel.getComments(postId) { comments = it }
                                    }
                                },
                                onReply = { replyingTo = reply }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment, 
    isOwner: Boolean, 
    onDelete: () -> Unit, 
    onEdit: (String) -> Unit,
    onReply: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedText by remember { mutableStateOf(comment.text) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        com.example.subtrackai.ui.components.ProfileAvatar(
            iconName = comment.profileIcon,
            avatarUrl = comment.avatarUrl, 
            modifier = Modifier.size(36.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = comment.authorName, 
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    if (isEditing) {
                        OutlinedTextField(
                            value = editedText,
                            onValueChange = { editedText = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            trailingIcon = {
                                IconButton(onClick = { 
                                    onEdit(editedText)
                                    isEditing = false
                                }) {
                                    Icon(Icons.Default.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                    } else {
                        Text(
                            text = comment.text, 
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            ) {
                val timeString = comment.createdAt?.let { 
                    try {
                        val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                        val date = isoFormat.parse(it)
                        date?.let { d ->
                            java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(d)
                        } ?: ""
                    } catch (e: Exception) { "" }
                } ?: ""
                
                if (timeString.isNotEmpty()) {
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }
                
                Text(
                    text = "Reply",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onReply() }
                )
                
                if (isOwner) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = if (isEditing) "Cancel" else "Edit", 
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.clickable { 
                            if (isEditing) editedText = comment.text
                            isEditing = !isEditing 
                        }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Delete", 
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.clickable { onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
fun ShimmerPostItem() {
    // Basic shimmer skeleton
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = Color.LightGray.copy(alpha = 0.3f)) {}
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Surface(modifier = Modifier.width(100.dp).height(14.dp), color = Color.LightGray.copy(alpha = 0.3f)) {}
                Spacer(modifier = Modifier.height(4.dp))
                Surface(modifier = Modifier.width(60.dp).height(10.dp), color = Color.LightGray.copy(alpha = 0.2f)) {}
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Surface(modifier = Modifier.fillMaxWidth().height(16.dp), color = Color.LightGray.copy(alpha = 0.2f)) {}
        Spacer(modifier = Modifier.height(8.dp))
        Surface(modifier = Modifier.width(200.dp).height(16.dp), color = Color.LightGray.copy(alpha = 0.2f)) {}
    }
}

@Composable
fun PostItem(
    post: Post, 
    onLikeToggle: () -> Unit, 
    onProfileClick: () -> Unit,
    onOriginalProfileClick: (String) -> Unit = {},
    isOwner: Boolean,
    onDelete: () -> Unit,
    onEdit: (String) -> Unit,
    onCommentClick: () -> Unit,
    onShare: () -> Unit,
    onToggleComments: () -> Unit,
    showFeedTag: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editedContent by remember { mutableStateOf(post.content) }
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.99f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (post.shared) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${post.authorName} shared this", 
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary, 
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onProfileClick
                    )
                ) {
                    com.example.subtrackai.ui.components.ProfileAvatar(
                        iconName = post.profileIcon, 
                        avatarUrl = post.avatarUrl,
                        modifier = Modifier.size(42.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = post.authorName, 
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            if (showFeedTag) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = if (post.profilePost) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (post.profilePost) "Profile" else "Feed",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = if (post.profilePost) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        val timeString = post.createdAt?.let { 
                            try {
                                val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                                val date = isoFormat.parse(it)
                                date?.let { d ->
                                    java.text.SimpleDateFormat("MMM dd • HH:mm", java.util.Locale.getDefault()).format(d)
                                } ?: "Just now"
                            } catch (e: Exception) {
                                "Just now"
                            }
                        } ?: "Just now"
                        Text(
                            text = timeString, 
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
                
                if (isOwner) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreHoriz, contentDescription = "Options")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = { 
                                    isEditing = true
                                    showMenu = false 
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (post.commentsEnabled) "Turn off Comments" else "Turn on Comments") },
                                onClick = { 
                                    onToggleComments()
                                    showMenu = false 
                                },
                                leadingIcon = { Icon(if (post.commentsEnabled) Icons.Default.CommentsDisabled else Icons.Default.Comment, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = { 
                                    onDelete()
                                    showMenu = false 
                                },
                                leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            if (isEditing) {
                OutlinedTextField(
                    value = editedContent,
                    onValueChange = { editedContent = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    trailingIcon = {
                        IconButton(onClick = { 
                            onEdit(editedContent)
                            isEditing = false
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            } else {
                if (post.shared) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                post.originalAuthorId?.let { onOriginalProfileClick(it) }
                            },
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                com.example.subtrackai.ui.components.ProfileAvatar(
                                    iconName = "Person", // This is within a shared post, ideally would be the original author's icon
                                    avatarUrl = null,
                                    modifier = Modifier.size(30.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    post.originalAuthorName ?: "Unknown", 
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                post.content, 
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    Text(
                        text = post.content, 
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentUserId = supabase.auth.currentUserOrNull()?.id
                val isLiked = post.likedBy.contains(currentUserId ?: "")
                
                Row(
                    modifier = Modifier.weight(1f).clickable { onLikeToggle() }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    AnimatedContent(
                        targetState = isLiked,
                        transitionSpec = {
                            if (targetState) {
                                (scaleIn(animationSpec = spring(Spring.DampingRatioMediumBouncy)) + fadeIn())
                                    .togetherWith(scaleOut() + fadeOut())
                            } else {
                                fadeIn() togetherWith fadeOut()
                            }
                        },
                        label = "likeAnimation"
                    ) { liked ->
                        Icon(
                            imageVector = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (liked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (post.likes > 0) "${post.likes}" else "Like",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (post.commentsEnabled) {
                    Row(
                        modifier = Modifier.weight(1f).clickable { onCommentClick() }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Comment, 
                            contentDescription = "Comment", 
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (post.commentCount > 0) "${post.commentCount}" else "Comment",
                            fontSize = 14.sp, 
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.weight(1f).clickable { onShare() }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Share, 
                        contentDescription = "Share", 
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Share", 
                        fontSize = 14.sp, 
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun Modifier.size(size: Int): Modifier = size(size.dp)
