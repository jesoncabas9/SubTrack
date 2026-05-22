package com.example.subtrackai.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.subtrackai.model.Post
import com.example.subtrackai.ui.theme.DeepPurple
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

    var commentPostId by remember { mutableStateOf<String?>(null) }
    var sharePost by remember { mutableStateOf<Post?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize()) {
            // Post Creation Trigger (Like FB Lite entry)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp)
                    .clickable { onNavigateToCreatePost() },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = DeepPurple.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = DeepPurple)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "What's on your mind?",
                        color = Color.Gray,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF4CAF50))
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        onCommentClick = { commentPostId = post.id },
                        onShare = { sharePost = post },
                        onToggleComments = { viewModel.editPost(post.id, if (post.commentsEnabled) "OFF" else "ON") } // Tweak this logic
                    )
                }
            }
        }
        
        if (commentPostId != null) {
            CommentSheet(
                postId = commentPostId!!,
                currentUserId = currentUserId ?: "",
                viewModel = viewModel,
                socialViewModel = socialViewModel,
                onDismiss = { commentPostId = null }
            )
        }

        if (sharePost != null) {
            ShareDialog(
                post = sharePost!!,
                socialViewModel = socialViewModel,
                onDismiss = { sharePost = null }
            )
        }
    }
}

@Composable
fun ShareDialog(
    post: Post,
    socialViewModel: com.example.subtrackai.viewmodel.SocialViewModel,
    onDismiss: () -> Unit
) {
    val friends by socialViewModel.friends.collectAsState()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share Post") },
        text = {
            Column {
                TextButton(onClick = {
                    socialViewModel.sharePostToProfile(post)
                    onDismiss()
                }) {
                    Icon(Icons.Default.Person, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share to my Profile")
                }
                
                HorizontalDivider()
                
                Text("Share to Friends", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 16.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(friends) { friend ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    socialViewModel.sharePostInChat(post, friend.uid)
                                    onDismiss()
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(com.example.subtrackai.util.ProfileIcons.getIcon(friend.profileIcon), contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(friend.username)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CommentSheet(
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Comments") },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
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
                            Box(modifier = Modifier.padding(start = 32.dp)) {
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
                
                if (replyingTo != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Replying to ${replyingTo!!.authorName}", fontSize = 12.sp, color = DeepPurple)
                        IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(16.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(if (replyingTo != null) "Write a reply..." else "Add a comment...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
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
            }) { Text("Post") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun CommentItem(
    comment: com.example.subtrackai.model.Comment, 
    isOwner: Boolean, 
    onDelete: () -> Unit,
    onReply: () -> Unit
) {
    Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = DeepPurple.copy(alpha = 0.1f)) {
            Box(contentAlignment = Alignment.Center) {
                Text(comment.authorName.take(1).uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(comment.authorName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(comment.text, fontSize = 13.sp)
            
            TextButton(onClick = onReply, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(24.dp)) {
                Text("Reply", fontSize = 11.sp, color = DeepPurple)
            }
        }
        if (isOwner) {
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Delete", modifier = Modifier.size(16.dp))
            }
        }
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (post.shared) {
                    Surface(
                        color = DeepPurple.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(12.dp), tint = DeepPurple)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Shared a post", fontSize = 10.sp, color = DeepPurple, fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (showFeedTag && !post.profilePost) {
                    Surface(
                        color = Color.Gray.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.RssFeed, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Posted on Feed", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    }
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
                        color = DeepPurple.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(post.authorName.take(1).uppercase(), fontWeight = FontWeight.Bold, color = DeepPurple)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(post.authorName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        val timeString = post.timestamp?.let { 
                            SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(it)
                        } ?: "Just now"
                        Text(timeString, fontSize = 11.sp, color = Color.Gray)
                    }
                }
                
                if (isOwner) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = { 
                                    isEditing = true
                                    showMenu = false 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (post.commentsEnabled) "Turn off Comments" else "Turn on Comments") },
                                onClick = { 
                                    onToggleComments()
                                    showMenu = false 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = Color.Red) },
                                onClick = { 
                                    onDelete()
                                    showMenu = false 
                                }
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
                    trailingIcon = {
                        IconButton(onClick = { 
                            onEdit(editedContent)
                            isEditing = false
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                    }
                )
            } else {
                if (post.shared) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .clickable { 
                                post.originalAuthorId?.let { onOriginalProfileClick(it) }
                            },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(28.dp), 
                                    shape = CircleShape, 
                                    color = DeepPurple.copy(alpha = 0.1f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            post.originalAuthorName?.take(1)?.uppercase() ?: "?", 
                                            fontSize = 12.sp, 
                                            fontWeight = FontWeight.Bold,
                                            color = DeepPurple
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    post.originalAuthorName ?: "Unknown", 
                                    fontWeight = FontWeight.Bold, 
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                post.content, 
                                fontSize = 14.sp,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Text(post.content, fontSize = 15.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onLikeToggle) {
                        Icon(
                            imageVector = if (post.likes > 0) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (post.likes > 0) Color.Red else Color.Gray
                        )
                    }
                    Text("${post.likes} Likes", fontSize = 13.sp, color = Color.Gray)
                    
                    if (post.commentsEnabled) {
                        IconButton(onClick = onCommentClick) {
                            Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "Comment", tint = Color.Gray)
                        }
                    }
                }
                
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.Gray)
                }
            }
        }
    }
}
