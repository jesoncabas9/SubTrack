package com.example.subtrackai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subtrackai.model.*
import com.example.subtrackai.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

class SocialViewModel : ViewModel() {

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _friends = MutableStateFlow<List<UserProfile>>(emptyList())
    val friends: StateFlow<List<UserProfile>> = _friends.asStateFlow()

    private val _friendRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val friendRequests: StateFlow<List<FriendRequest>> = _friendRequests.asStateFlow()

    private val _visitorFriends = MutableStateFlow<List<UserProfile>>(emptyList())
    val visitorFriends = _visitorFriends.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

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

    fun refreshAll(uid: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                loadCurrentUserProfile(uid)
                observeFriendRequests(uid)
                observeFriends(uid)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private    fun observeFriends(uid: String) {
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
                
                val friendIds = requests.map { if (it.senderId == uid) it.receiverId else it.senderId }
                if (friendIds.isEmpty()) {
                    _friends.value = emptyList()
                    return@launch
                }
                
                val friendProfiles = supabase.postgrest["profiles"]
                    .select {
                        filter {
                            filter("id", FilterOperator.IN, "(${friendIds.joinToString(",")})")
                        }
                    }
                    .decodeList<UserProfile>()
                _friends.value = friendProfiles
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error observing friends: ${e.message}")
            }
        }
    }

    fun loadCurrentUserProfile(uid: String) {
        viewModelScope.launch {
            try {
                val profile = supabase.postgrest["profiles"]
                    .select {
                        filter {
                            eq("id", uid)
                        }
                    }
                    .decodeSingleOrNull<UserProfile>()
                _userProfile.value = profile
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error loading profile: ${e.message}")
            }
        }
    }

    fun toggleOnlineStatus(isOnline: Boolean) {
        val uid = _userProfile.value?.uid ?: return
        viewModelScope.launch {
            try {
                val data = ProfileUpdate(isOnline = isOnline)
                supabase.postgrest["profiles"].update(Json.encodeToJsonElement(data)) {
                    filter { eq("id", uid) }
                }
                loadCurrentUserProfile(uid)
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error toggling online status: ${e.message}")
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
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error loading visitor profile")
                onResult(null)
            }
        }
    }

    fun observeVisitorFriends(uid: String) {
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
                
                val friendIds = requests.map { if (it.senderId == uid) it.receiverId else it.senderId }
                if (friendIds.isEmpty()) {
                    _visitorFriends.value = emptyList()
                    return@launch
                }
                
                val friendProfiles = supabase.postgrest["profiles"]
                    .select {
                        filter {
                            filter("id", FilterOperator.IN, "(${friendIds.joinToString(",")})")
                        }
                    }
                    .decodeList<UserProfile>()
                _visitorFriends.value = friendProfiles
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error observing visitor friends: ${e.message}")
            }
        }
    }

    fun observeFriendRequests(uid: String) {
        viewModelScope.launch {
            try {
                val requests = supabase.postgrest["friend_requests"]
                    .select {
                        filter {
                            eq("receiver_id", uid)
                        }
                    }
                    .decodeList<FriendRequest>()
                _friendRequests.value = requests
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error observing friend requests: ${e.message}")
            }
        }
    }

    fun searchUsers(query: String, onResult: (List<UserProfile>) -> Unit) {
        viewModelScope.launch {
            try {
                val results = supabase.postgrest["profiles"]
                    .select {
                        filter {
                            ilike("username", "%$query%")
                        }
                    }
                    .decodeList<UserProfile>()
                onResult(results)
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error searching users")
                onResult(emptyList())
            }
        }
    }

    suspend fun sendFriendRequest(targetUser: UserProfile) {
        val currentUserId = _userProfile.value?.uid ?: return
        val currentUserName = _userProfile.value?.username ?: "Someone"
        try {
            val data = FriendRequestInsert(
                    senderId = currentUserId,
                    receiverId = targetUser.uid,
                    senderName = currentUserName
                )
            supabase.postgrest["friend_requests"].insert(Json.encodeToJsonElement(data))
        } catch (e: Exception) {
            Log.e("SocialViewModel", "Error sending friend request: ${e.message}")
        }
    }

    suspend fun cancelFriendRequest(receiverId: String) {
        val senderId = _userProfile.value?.uid ?: return
        try {
            supabase.postgrest["friend_requests"].delete {
                filter {
                    eq("sender_id", senderId)
                    eq("receiver_id", receiverId)
                }
            }
        } catch (e: Exception) {
            Log.e("SocialViewModel", "Error canceling friend request: ${e.message}")
        }
    }

    suspend fun unfriendUser(friendId: String) {
        val uid = _userProfile.value?.uid ?: return
        try {
            supabase.postgrest["friend_requests"].delete {
                filter {
                    or {
                        and {
                            eq("sender_id", uid)
                            eq("receiver_id", friendId)
                        }
                        and {
                            eq("sender_id", friendId)
                            eq("receiver_id", uid)
                        }
                    }
                }
            }
            observeFriends(uid)
        } catch (e: Exception) {
            Log.e("SocialViewModel", "Error unfriending user: ${e.message}")
        }
    }

    suspend fun acceptFriendRequest(request: FriendRequest) {
        try {
            val data = FriendRequestUpdate(status = "accepted")
            supabase.postgrest["friend_requests"].update(Json.encodeToJsonElement(data)) {
                filter {
                    eq("id", request.id ?: "")
                }
            }
            val uid = _userProfile.value?.uid ?: return
            observeFriends(uid)
            observeFriendRequests(uid)
        } catch (e: Exception) {
            Log.e("SocialViewModel", "Error accepting friend request: ${e.message}")
        }
    }

    fun updateCurrency(currency: String) {
        val uid = _userProfile.value?.uid ?: return
        viewModelScope.launch {
            try {
                val data = ProfileUpdate(currency = currency)
                supabase.postgrest["profiles"].update(Json.encodeToJsonElement(data)) {
                    filter { eq("id", uid) }
                }
                loadCurrentUserProfile(uid)
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error updating currency: ${e.message}")
            }
        }
    }

    fun updateUserStatus(status: String) {
        val uid = _userProfile.value?.uid ?: return
        viewModelScope.launch {
            try {
                val data = ProfileUpdate(userStatus = status)
                supabase.postgrest["profiles"].update(Json.encodeToJsonElement(data)) {
                    filter { eq("id", uid) }
                }
                loadCurrentUserProfile(uid)
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error updating status: ${e.message}")
            }
        }
    }

    suspend fun addComment(postId: String, content: String, profileIcon: String, avatarUrl: String? = null, parentCommentId: String? = null) {
        Log.d("SV_DEBUG", "addComment started")
        val user = supabase.auth.currentUserOrNull() ?: return
        try {
            val data = CommentInsert(
                postId = postId,
                userId = user.id,
                authorName = _userProfile.value?.username ?: "User",
                profileIcon = profileIcon,
                avatarUrl = avatarUrl,
                text = content,
                parentCommentId = parentCommentId
            )
            Log.d("SV_DEBUG", "Inserting: $data")
            supabase.postgrest["comments"].insert(Json.encodeToJsonElement(data))
            Log.d("SV_DEBUG", "Insert successful")
        } catch (e: Exception) {
            Log.e("SocialViewModel", "Error adding comment: ${e.message}")
        }
    }

    suspend fun editComment(postId: String, commentId: String, newText: String) {
        Log.d("SV_DEBUG", "editComment: id=$commentId, newText=$newText")
        try {
            val data = CommentUpdate(text = newText)
            val result = supabase.postgrest["comments"].update(Json.encodeToJsonElement(data)) {
                filter { eq("id", commentId) }
            }
            Log.d("SV_DEBUG", "Edit successful: $result")
        } catch (e: Exception) {
            Log.e("SocialViewModel", "Error editing comment: ${e.message}", e)
        }
    }

    suspend fun deleteComment(postId: String, commentId: String) {
        Log.d("SV_DEBUG", "deleteComment: id=$commentId")
        try {
            val result = supabase.postgrest["comments"].delete {
                filter { eq("id", commentId) }
            }
            Log.d("SV_DEBUG", "Delete successful: $result")
        } catch (e: Exception) {
            Log.e("SocialViewModel", "Error deleting comment: ${e.message}", e)
        }
    }

    fun toggleCommentsEnabled(postId: String, enabled: Boolean) {
        viewModelScope.launch {
            try {
                val data = PostUpdate(commentsEnabled = enabled)
                supabase.postgrest["posts"].update(Json.encodeToJsonElement(data)) {
                    filter { eq("id", postId) }
                }
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error toggling comments: ${e.message}")
            }
        }
    }

    suspend fun sharePostToProfile(post: Post) {
        val user = supabase.auth.currentUserOrNull() ?: return
        try {
            val data = PostInsert(
                userId = user.id,
                authorName = _userProfile.value?.username ?: "User",
                content = post.content,
                profileIcon = _userProfile.value?.profileIcon,
                avatarUrl = _userProfile.value?.avatarUrl,
                shared = true,
                originalPostId = post.id,
                originalAuthorName = post.authorName,
                originalAuthorId = post.userId,
                profilePost = true
            )
            supabase.postgrest["posts"].insert(Json.encodeToJsonElement(data))
        } catch (e: Exception) {
            Log.e("SocialViewModel", "Error sharing to profile")
        }
    }

    suspend fun sharePostInChat(post: Post, friendId: String) {
        val user = supabase.auth.currentUserOrNull() ?: return
        try {
            val data = MessageInsert(
                senderId = user.id,
                receiverId = friendId,
                text = "Shared a post: ${post.content}"
            )
            supabase.postgrest["messages"].insert(Json.encodeToJsonElement(data))
        } catch (e: Exception) {
            Log.e("SocialViewModel", "Error sharing in chat")
        }
    }

    fun updateProfile(fullName: String, bio: String, icon: String, avatarUrl: String) {
        val uid = _userProfile.value?.uid ?: return
        viewModelScope.launch {
            try {
                val data = ProfileUpdate(
                        fullName = fullName,
                        bio = bio,
                        profileIcon = icon,
                        avatarUrl = avatarUrl
                    )
                supabase.postgrest["profiles"].update(Json.encodeToJsonElement(data)) {
                    filter { eq("id", uid) }
                }
                loadCurrentUserProfile(uid)
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error updating profile: ${e.message}")
            }
        }
    }
}
