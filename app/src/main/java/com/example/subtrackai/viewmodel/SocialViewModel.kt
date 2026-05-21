package com.example.subtrackai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subtrackai.model.*
import com.example.subtrackai.model.Comment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SocialViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()

    private val _friends = MutableStateFlow<List<UserProfile>>(emptyList())
    val friends = _friends.asStateFlow()

    private val _friendRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val friendRequests = _friendRequests.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (uid != null) {
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

    private fun observeFriends(uid: String) {
        // Query accepted friend requests where user is either sender or receiver
        firestore.collection("friendRequests")
            .whereIn("status", listOf("accepted"))
            .addSnapshotListener { snapshot, _ ->
                val requests = snapshot?.toObjects(FriendRequest::class.java) ?: emptyList()
                val friendIds = requests.map { if (it.fromId == uid) it.toId else it.fromId }.filter { it != uid }
                
                if (friendIds.isNotEmpty()) {
                    firestore.collection("users")
                        .whereIn("uid", friendIds)
                        .addSnapshotListener { userSnapshot, _ ->
                            _friends.value = userSnapshot?.toObjects(UserProfile::class.java) ?: emptyList()
                        }
                } else {
                    _friends.value = emptyList()
                }
            }
    }

    private fun loadCurrentUserProfile(uid: String) {
        firestore.collection("users").document(uid).addSnapshotListener { snapshot, _ ->
            _userProfile.value = snapshot?.toObject(UserProfile::class.java)
        }
    }

    fun toggleOnlineStatus(isOnline: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).update("showSubscriptions", isOnline)
    }

    fun loadVisitorProfile(uid: String, onResult: (UserProfile?) -> Unit) {
        firestore.collection("users").document(uid).get().addOnSuccessListener {
            onResult(it.toObject(UserProfile::class.java))
        }
    }

    private fun observeFriendRequests(uid: String) {
        firestore.collection("friendRequests")
            .whereEqualTo("toId", uid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, _ ->
                _friendRequests.value = snapshot?.toObjects(FriendRequest::class.java) ?: emptyList()
            }
    }

    fun searchUsers(query: String, onResult: (List<UserProfile>) -> Unit) {
        firestore.collection("users")
            .whereGreaterThanOrEqualTo("username", query)
            .whereLessThanOrEqualTo("username", query + "\uf8ff")
            .get()
            .addOnSuccessListener {
                val currentUid = auth.currentUser?.uid
                val users = it.toObjects(UserProfile::class.java).filter { it.uid != currentUid }
                onResult(users)
            }
    }

    fun sendFriendRequest(toUser: UserProfile) {
        val currentUser = auth.currentUser ?: return
        if (toUser.uid == currentUser.uid) return // Cannot friend self
        
        val request = mapOf(
            "fromId" to currentUser.uid,
            "fromName" to (_userProfile.value?.username ?: "Unknown"),
            "toId" to toUser.uid,
            "status" to "pending",
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        firestore.collection("friendRequests").add(request)
    }

    fun acceptFriendRequest(request: FriendRequest) {
        firestore.collection("friendRequests").document(request.id).update("status", "accepted")
        // In a real app, we'd add to a friends sub-collection here as well
    }

    fun updateCurrency(currency: String) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).update("currency", currency)
    }

    fun updateUserStatus(status: String) {
        val uid = auth.currentUser?.uid ?: return
        val updates = mapOf(
            "userStatus" to status,
            "isOnline" to (status == "Online")
        )
        firestore.collection("users").document(uid).update(updates)
    }

    fun addComment(postId: String, text: String, profileIcon: String, parentId: String? = null) {
        val user = auth.currentUser ?: return
        val comment = mapOf(
            "postId" to postId,
            "userId" to user.uid,
            "authorName" to (_userProfile.value?.username ?: "User"),
            "profileIcon" to profileIcon,
            "text" to text,
            "parentCommentId" to parentId,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        firestore.collection("posts").document(postId).collection("comments").add(comment)
    }

    fun editComment(postId: String, commentId: String, newText: String) {
        firestore.collection("posts").document(postId).collection("comments").document(commentId).update("text", newText)
    }

    fun deleteComment(postId: String, commentId: String) {
        firestore.collection("posts").document(postId).collection("comments").document(commentId).delete()
    }

    fun toggleCommentsEnabled(postId: String, enabled: Boolean) {
        firestore.collection("posts").document(postId).update("commentsEnabled", enabled)
    }

    fun sharePostToProfile(post: Post) {
        val user = auth.currentUser ?: return
        val sharedPost = Post(
            userId = user.uid,
            authorName = _userProfile.value?.username ?: "User",
            content = post.content,
            shared = true,
            originalPostId = post.id,
            originalAuthorName = post.authorName,
            originalAuthorId = post.userId,
            likes = 0,
            likedBy = emptyList(),
            commentsEnabled = true,
            profilePost = true
        )
        firestore.collection("posts").add(sharedPost)
    }

    fun sharePostInChat(post: Post, receiverId: String) {
        val senderId = auth.currentUser?.uid ?: return
        val message = Message(
            senderId = senderId,
            receiverId = receiverId,
            text = "Shared a post: ${post.content}\nby ${post.authorName}"
        )
        firestore.collection("messages").add(message)
    }

    fun updateProfile(fullName: String, username: String, bio: String, icon: String) {
        val uid = auth.currentUser?.uid ?: return
        val updates = mapOf(
            "fullName" to fullName,
            "username" to username,
            "bio" to bio,
            "profileIcon" to icon
        )
        firestore.collection("users").document(uid).update(updates)
    }
}
