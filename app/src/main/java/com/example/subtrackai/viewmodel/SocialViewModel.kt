package com.example.subtrackai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subtrackai.model.*
import com.example.subtrackai.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SocialViewModel : ViewModel() {

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()

    private val _friends = MutableStateFlow<List<UserProfile>>(emptyList())
    val friends = _friends.asStateFlow()

    private val _friendRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val friendRequests = _friendRequests.asStateFlow()

    private val _visitorFriends = MutableStateFlow<List<UserProfile>>(emptyList())
    val visitorFriends = _visitorFriends.asStateFlow()

    init {
        viewModelScope.launch {
            supabase.auth.sessionStatus.collect { status ->
                if (status is SessionStatus.Authenticated) {
                    val uid = status.session.user?.id ?: return@collect
                    loadCurrentUserProfile(uid)
                    observeFriendRequests(uid)
                    observeFriends(uid)
                } else {
                    _userProfile.value = null
                    _friends.value = emptyList()
                    _friendRequests.value = emptyList()
                }
            }
        }
    }

    private fun observeFriends(uid: String) {
        viewModelScope.launch {
            try {
                val requests = supabase.postgrest["friend_requests"]
                    .select {
                        filter {
                            eq("status", "accepted")
                            or {
                                eq("sender_id", uid)
                                eq("receiver_id", uid)
                            }
                        }
                    }
                    .decodeList<FriendRequest>()
                
                val friendIds = requests.map { if (it.senderId == uid) it.receiverId else it.senderId }.filter { it != uid }
                
                if (friendIds.isNotEmpty()) {
                    val users = supabase.postgrest["profiles"]
                        .select {
                            filter {
                                isIn("id", friendIds)
                            }
                        }
                        .decodeList<UserProfile>()
                    _friends.value = users
                } else {
                    _friends.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error observing friends: ${e.message}", e)
            }
        }
    }

    private fun loadCurrentUserProfile(uid: String) {
        viewModelScope.launch {
            try {
                val profile = supabase.postgrest["profiles"]
                    .select {
                        filter {
                            eq("id", uid)
                        }
                    }
                    .decodeSingleOrNull<UserProfile>()
                
                if (profile != null) {
                    _userProfile.value = profile
                } else {
                    Log.w("SocialViewModel", "Profile not found for UID: $uid")
                }
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error loading profile: ${e.message}", e)
            }
        }
    }

    fun toggleOnlineStatus(isOnline: Boolean) {
        val uid = supabase.auth.currentUserOrNull()?.id ?: return
        viewModelScope.launch {
            try {
                supabase.postgrest["profiles"].update(
                    buildJsonObject { put("is_online", isOnline) }
                ) {
                    filter { eq("id", uid) }
                }
                loadCurrentUserProfile(uid)
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error toggling status: ${e.message}", e)
            }
        }
    }

    fun loadVisitorProfile(uid: String, onResult: (UserProfile?) -> Unit) {
        viewModelScope.launch {
            try {
                val profile = supabase.postgrest["profiles"]
                    .select {
                        filter {
                            eq("id", uid)
                        }
                    }
                    .decodeSingleOrNull<UserProfile>()
                onResult(profile)
                observeVisitorFriends(uid)
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error loading visitor profile: ${e.message}", e)
                onResult(null)
            }
        }
    }

    private fun observeVisitorFriends(uid: String) {
        viewModelScope.launch {
            try {
                val requests = supabase.postgrest["friend_requests"]
                    .select {
                        filter {
                            eq("status", "accepted")
                            or {
                                eq("sender_id", uid)
                                eq("receiver_id", uid)
                            }
                        }
                    }
                    .decodeList<FriendRequest>()
                
                val friendIds = requests.map { if (it.senderId == uid) it.receiverId else it.senderId }.filter { it != uid }
                
                if (friendIds.isNotEmpty()) {
                    val users = supabase.postgrest["profiles"]
                        .select {
                            filter {
                                isIn("id", friendIds)
                            }
                        }
                        .decodeList<UserProfile>()
                    _visitorFriends.value = users
                } else {
                    _visitorFriends.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error observing visitor friends", e)
            }
        }
    }

    private fun observeFriendRequests(uid: String) {
        viewModelScope.launch {
            try {
                val requests = supabase.postgrest["friend_requests"]
                    .select {
                        filter {
                            eq("status", "pending")
                            or {
                                eq("sender_id", uid)
                                eq("receiver_id", uid)
                            }
                        }
                    }
                    .decodeList<FriendRequest>()
                _friendRequests.value = requests
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error observing requests", e)
            }
        }
    }

    fun searchUsers(query: String, onResult: (List<UserProfile>) -> Unit) {
        viewModelScope.launch {
            try {
                val currentUid = supabase.auth.currentUserOrNull()?.id
                val users = supabase.postgrest["profiles"]
                    .select {
                        filter {
                            ilike("username", "%$query%")
                        }
                    }
                    .decodeList<UserProfile>()
                    .filter { it.uid != currentUid }
                onResult(users)
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error searching users", e)
                onResult(emptyList())
            }
        }
    }

    fun sendFriendRequest(toUser: UserProfile) {
        val user = supabase.auth.currentUserOrNull() ?: return
        if (toUser.uid == user.id) return 
        
        val request = FriendRequest(
            senderId = user.id,
            senderName = _userProfile.value?.username ?: "Unknown",
            receiverId = toUser.uid,
            status = "pending"
        )
        viewModelScope.launch {
            try {
                supabase.postgrest["friend_requests"].insert(request)
                observeFriendRequests(user.id)
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error sending request: ${e.message}", e)
            }
        }
    }

    fun cancelFriendRequest(toUserId: String) {
        val user = supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch {
            try {
                supabase.postgrest["friend_requests"].delete {
                    filter {
                        eq("sender_id", user.id)
                        eq("receiver_id", toUserId)
                    }
                }
                observeFriendRequests(user.id)
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error canceling request", e)
            }
        }
    }

    fun unfriendUser(toUserId: String) {
        val user = supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch {
            try {
                supabase.postgrest["friend_requests"].delete {
                    filter {
                        or {
                            and {
                                eq("sender_id", user.id)
                                eq("receiver_id", toUserId)
                            }
                            and {
                                eq("sender_id", toUserId)
                                eq("receiver_id", user.id)
                            }
                        }
                    }
                }
                observeFriends(user.id)
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error unfriending", e)
            }
        }
    }

    fun acceptFriendRequest(request: FriendRequest) {
        val requestId = request.id ?: return
        val user = supabase.auth.currentUserOrNull() ?: return
        viewModelScope.launch {
            try {
                supabase.postgrest["friend_requests"].update(
                    buildJsonObject { put("status", "accepted") }
                ) {
                    filter {
                        eq("id", requestId)
                    }
                }
                observeFriendRequests(user.id)
                observeFriends(user.id)
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error accepting request", e)
            }
        }
    }

    fun updateCurrency(currency: String) {
        val uid = supabase.auth.currentUserOrNull()?.id ?: return
        viewModelScope.launch {
            try {
                supabase.postgrest["profiles"].update(
                    buildJsonObject { put("currency", currency) }
                ) {
                    filter {
                        eq("id", uid)
                    }
                }
                loadCurrentUserProfile(uid)
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error updating currency", e)
            }
        }
    }

    fun updateUserStatus(status: String) {
        val uid = supabase.auth.currentUserOrNull()?.id ?: return
        viewModelScope.launch {
            try {
                supabase.postgrest["profiles"].update(
                    buildJsonObject { 
                        put("user_status", status)
                        put("is_online", status == "Online")
                    }
                ) {
                    filter {
                        eq("id", uid)
                    }
                }
                loadCurrentUserProfile(uid)
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error updating status", e)
            }
        }
    }

    fun addComment(postId: String, content: String, profileIcon: String, parentCommentId: String? = null) {
        val user = supabase.auth.currentUserOrNull() ?: return
        val comment = Comment(
            postId = postId,
            userId = user.id,
            authorName = _userProfile.value?.username ?: "User",
            profileIcon = profileIcon,
            content = content,
            parentCommentId = parentCommentId
        )
        viewModelScope.launch {
            try {
                supabase.postgrest["comments"].insert(comment)
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error adding comment: ${e.message}", e)
            }
        }
    }

    fun editComment(postId: String, commentId: String, newText: String) {
        viewModelScope.launch {
            try {
                supabase.postgrest["comments"].update(
                    buildJsonObject { put("content", newText) }
                ) {
                    filter {
                        eq("id", commentId)
                    }
                }
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error editing comment", e)
            }
        }
    }

    fun deleteComment(postId: String, commentId: String) {
        viewModelScope.launch {
            try {
                supabase.postgrest["comments"].delete {
                    filter {
                        eq("id", commentId)
                    }
                }
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error deleting comment", e)
            }
        }
    }

    fun toggleCommentsEnabled(postId: String, enabled: Boolean) {
        viewModelScope.launch {
            try {
                supabase.postgrest["posts"].update(
                    buildJsonObject { put("comments_enabled", enabled) }
                ) {
                    filter {
                        eq("id", postId)
                    }
                }
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error toggling comments", e)
            }
        }
    }

    fun sharePostToProfile(post: Post) {
        val user = supabase.auth.currentUserOrNull() ?: return
        val sharedPost = Post(
            userId = user.id,
            authorName = _userProfile.value?.username ?: "User",
            content = post.content,
            shared = true,
            originalPostId = if (post.shared) post.originalPostId else post.id,
            originalAuthorName = if (post.shared) post.originalAuthorName else post.authorName,
            originalAuthorId = if (post.shared) post.originalAuthorId else post.userId,
            likes = 0,
            likedBy = emptyList(),
            commentsEnabled = true,
            profilePost = true
        )
        viewModelScope.launch {
            try {
                supabase.postgrest["posts"].insert(sharedPost)
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error sharing post", e)
            }
        }
    }

    fun sharePostInChat(post: Post, receiverId: String) {
        val user = supabase.auth.currentUserOrNull() ?: return
        val message = Message(
            senderId = user.id,
            receiverId = receiverId,
            text = "Shared a post: ${post.content}\nby ${post.authorName}"
        )
        viewModelScope.launch {
            try {
                supabase.postgrest["messages"].insert(message)
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error sharing post in chat", e)
            }
        }
    }

    fun updateProfile(fullName: String, username: String, bio: String, icon: String) {
        val uid = supabase.auth.currentUserOrNull()?.id ?: return
        viewModelScope.launch {
            try {
                supabase.postgrest["profiles"].update(
                    buildJsonObject { 
                        put("full_name", fullName)
                        put("username", username)
                        put("bio", bio)
                        put("profile_icon", icon)
                    }
                ) {
                    filter {
                        eq("id", uid)
                    }
                }
                loadCurrentUserProfile(uid)
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error updating profile", e)
            }
        }
    }
}

