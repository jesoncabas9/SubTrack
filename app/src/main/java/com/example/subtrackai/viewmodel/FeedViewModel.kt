package com.example.subtrackai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subtrackai.model.Comment
import com.example.subtrackai.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FeedViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    init {
        observePosts()
    }

    private fun observePosts() {
        firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val postList = snapshot?.toObjects(Post::class.java) ?: emptyList()
                _posts.value = postList
            }
    }

    fun getComments(postId: String, onResult: (List<Comment>) -> Unit) {
        firestore.collection("posts").document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                onResult(snapshot?.toObjects(Comment::class.java) ?: emptyList())
            }
    }

    fun createPost(content: String, authorName: String, profilePost: Boolean = false) {
        val user = auth.currentUser ?: return
        val newPost = Post(
            userId = user.uid,
            authorName = authorName,
            content = content,
            profilePost = profilePost
        )
        viewModelScope.launch {
            firestore.collection("posts").add(newPost)
        }
    }

    fun toggleLike(postId: String) {
        val userId = auth.currentUser?.uid ?: return
        val postRef = firestore.collection("posts").document(postId)
        
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val post = snapshot.toObject(Post::class.java) ?: return@runTransaction
            
            val newLikedBy = post.likedBy.toMutableList()
            var newLikes = post.likes
            
            if (newLikedBy.contains(userId)) {
                newLikedBy.remove(userId)
                newLikes--
            } else {
                newLikedBy.add(userId)
                newLikes++
            }
            
            transaction.update(postRef, "likedBy", newLikedBy)
            transaction.update(postRef, "likes", newLikes)
        }
    }

    fun deletePost(postId: String) {
        firestore.collection("posts").document(postId).delete()
    }

    fun editPost(postId: String, newContent: String) {
        if (newContent == "ON" || newContent == "OFF") {
            firestore.collection("posts").document(postId).update("commentsEnabled", newContent == "ON")
        } else {
            firestore.collection("posts").document(postId).update("content", newContent)
        }
    }
}
