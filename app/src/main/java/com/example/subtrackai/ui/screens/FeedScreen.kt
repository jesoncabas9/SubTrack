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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.subtrackai.model.Post
import com.example.subtrackai.ui.theme.DeepPurple
import com.example.subtrackai.ui.theme.LightPurple
import com.example.subtrackai.util.ProfileIcons
import com.example.subtrackai.viewmodel.FeedViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    socialViewModel: com.example.subtrackai.viewmodel.SocialViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateToCreatePost: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToMyProfile: () -> Unit,
    isDarkMode: Boolean
) {
    val posts by viewModel.feedPosts.collectAsState()
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid
    
    val userProfile by socialViewModel.userProfile.collectAsState()

    val sheetState = rememberModalBottomSheetState()
    var showCommentSheet by remember { mutableStateOf(false) }
    var selectedPostId by remember { mutableStateOf<String?>(null) }
    
    var showShareSheet by remember { mutableStateOf(false) }
    var selectedSharePost by remember { mutableStateOf<Post?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Post Creation Trigger
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { onNavigateToCreatePost() },
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                ProfileIcons.getIcon(userProfile?.profileIcon), 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Text(
                            "What's on your mind?",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        Icons.Default.Image, 
                        contentDescription = null, 
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (posts.isEmpty()) {
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(posts) { post ->
                        PostItem(
                            post = post, 
                            onLikeToggle = { viewModel.toggleLike(post.id) },
                            onProfileClick = { onNavigateToProfile(post.userId) },
                            onOriginalProfileClick = { authorId -> onNavigateToProfile(authorId) },
                            isOwner = post.userId == currentUserId,
                            onDelete = { viewModel.deletePost(post.id) },
                            onEdit = { newContent -> viewModel.editPost(post.id, newContent) },
                            onCommentClick = { 
                                selectedPostId = post.id
                                showCommentSheet = true 
                            },
                            onShare = { 
                                selectedSharePost = post
                                showShareSheet = true
                            },
                            onToggleComments = { viewModel.editPost(post.id, if (post.commentsEnabled) "OFF" else "ON") }
                        )
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
fun ShareSheetContent(
    post: Post,
    socialViewModel: com.example.subtrackai.viewmodel.SocialViewModel,
    onDismiss: () -> Unit
) {
    val friends by socialViewModel.friends.collectAsState()
    
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
        Text("Share Post", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        
        Surface(
            modifier = Modifier.fillMaxWidth().clickable {
                socialViewModel.sharePostToProfile(post)
                onDismiss()
            },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Text("Share to my Profile", fontWeight = FontWeight.Medium)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Send to Friends", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
            items(friends) { friend ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            socialViewModel.sharePostInChat(post, friend.uid)
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(ProfileIcons.getIcon(friend.profileIcon), contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(friend.username, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun CommentSheetContent(
    postId: String,
    currentUserId: String,
    viewModel: FeedViewModel,
    socialViewModel: com.example.subtrackai.viewmodel.SocialViewModel,
    onDismiss: () -> Unit
) {
    var comments by remember { mutableStateOf<List<com.example.subtrackai.model.Comment>>(emptyList()) }
    var text by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<com.example.subtrackai.model.Comment?>(null) }
    val userProfile by socialViewModel.userProfile.collectAsState()

    LaunchedEffect(postId) {
        viewModel.getComments(postId) { comments = it }
    }

    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp).padding(horizontal = 16.dp)) {
        Text("Comments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            val topLevelComments = comments.filter { it.parentCommentId == null }
            items(topLevelComments) { comment ->
                CommentItem(
                    comment = comment,
                    isOwner = comment.userId == currentUserId,
                    onDelete = { socialViewModel.deleteComment(postId, comment.id) },
                    onReply = { replyingTo = comment }
                )
                
                // Render Sub-comments
                val replies = comments.filter { it.parentCommentId == comment.id }
                replies.forEach { reply ->
                    Box(modifier = Modifier.padding(start = 40.dp)) {
                        CommentItem(
                            comment = reply,
                            isOwner = reply.userId == currentUserId,
                            onDelete = { socialViewModel.deleteComment(postId, reply.id) },
                            onReply = { replyingTo = reply }
                        )
                    }
                }
            }
        }
        
        Surface(
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                if (replyingTo != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Replying to ${replyingTo!!.authorName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text(if (replyingTo != null) "Write a reply..." else "Add a comment...", fontSize = 14.sp) },
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (text.isNotBlank()) {
                                socialViewModel.addComment(
                                    postId = postId, 
                                    text = text, 
                                    profileIcon = userProfile?.profileIcon ?: "Person",
                                    parentId = replyingTo?.id
                                )
                                text = ""
                                replyingTo = null
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Post", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: com.example.subtrackai.model.Comment, 
    isOwner: Boolean, 
    onDelete: () -> Unit,
    onReply: () -> Unit
) {
    Row(modifier = Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.Top) {
        Surface(
            modifier = Modifier.size(36.dp), 
            shape = CircleShape, 
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    comment.authorName.take(1).uppercase(), 
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(comment.authorName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(comment.text, fontSize = 14.sp)
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
                TextButton(onClick = onReply, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(24.dp)) {
                    Text("Reply", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
                if (isOwner) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "Delete", 
                        fontSize = 12.sp, 
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.clickable { onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
fun ShimmerPostItem() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).graphicsLayer(alpha = alpha)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).background(Color.Gray.copy(alpha = 0.2f), CircleShape))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Box(modifier = Modifier.width(100.dp).height(12.dp).background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.width(60.dp).height(10.dp).background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp)))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(16.dp).background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp)))
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(0.7f).height(16.dp).background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp)))
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Box(modifier = Modifier.width(60.dp).height(20.dp).background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp)))
                Box(modifier = Modifier.width(60.dp).height(20.dp).background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp)))
                Box(modifier = Modifier.width(60.dp).height(20.dp).background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp)))
            }
        }
        HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
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
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {} // Just for the scale effect container
            ),
        shape = RoundedCornerShape(0.dp), // Use 0.dp for a full-width "feed" look or keeping it at 16.dp for card look
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (post.shared) {
                Row(
                    modifier = Modifier.padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${post.authorName} shared a post", 
                        fontSize = 12.sp, 
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
                    modifier = Modifier.weight(1f).clickable { onProfileClick() }
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                post.authorName.take(1).uppercase(), 
                                fontWeight = FontWeight.Bold, 
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(post.authorName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        val timeString = post.timestamp?.let { 
                            SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(it)
                        } ?: "Just now"
                        Text(timeString, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
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
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (isEditing) {
                OutlinedTextField(
                    value = editedContent,
                    onValueChange = { editedContent = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
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
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(32.dp), 
                                    shape = CircleShape, 
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            post.originalAuthorName?.take(1)?.uppercase() ?: "?", 
                                            fontSize = 12.sp, 
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    post.originalAuthorName ?: "Unknown", 
                                    fontWeight = FontWeight.Bold, 
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                post.content, 
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    Text(post.content, fontSize = 16.sp, lineHeight = 22.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                val isLiked = post.likedBy.contains(currentUserId)
                
                Row(
                    modifier = Modifier.weight(1f).clickable { onLikeToggle() }.padding(vertical = 8.dp),
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
                        modifier = Modifier.weight(1f).clickable { onCommentClick() }.padding(vertical = 8.dp),
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
                            "Comment", 
                            fontSize = 14.sp, 
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.weight(1f).clickable { onShare() }.padding(vertical = 8.dp),
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
        HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    }
}
