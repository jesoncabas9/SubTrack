package com.example.subtrackai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subtrackai.model.Comment
import com.example.subtrackai.model.Post
import com.example.subtrackai.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FeedViewModel : ViewModel() {

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()
    
    // Show ALL posts in feed, but maybe we can distinguish them in the UI
    val feedPosts: StateFlow<List<Post>> = _posts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            supabase.auth.sessionStatus.collect { status ->
                if (status is SessionStatus.Authenticated) {
                    observePosts()
                } else {
                    _posts.value = emptyList()
                }
            }
        }
    }

    fun observePosts() {
        viewModelScope.launch {
            try {
                val postList = supabase.postgrest["posts"]
                    .select {
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<Post>()
                _posts.value = postList
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Error observing posts: ${e.message}", e)
            }
        }
    }

    fun getComments(postId: String, onResult: (List<Comment>) -> Unit) {
        viewModelScope.launch {
            try {
                val comments = supabase.postgrest["comments"]
                    .select {
                        filter {
                            eq("post_id", postId)
                        }
                        order("created_at", Order.ASCENDING)
                    }
                    .decodeList<Comment>()
                onResult(comments)
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Error fetching comments", e)
                onResult(emptyList())
            }
        }
    }

    fun createPost(content: String, authorName: String, profilePost: Boolean = false) {
        val user = supabase.auth.currentUserOrNull() ?: return
        val newPost = Post(
            userId = user.id,
            authorName = authorName,
            content = content,
            profilePost = profilePost
        )
        viewModelScope.launch {
            try {
                // encodeDefaults=false in SupabaseModule ensures id and created_at are NOT sent if null
                supabase.postgrest["posts"].insert(newPost)
                observePosts()
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Error creating post: ${e.message}", e)
            }
        }
    }

    fun toggleLike(postId: String) {
        val user = supabase.auth.currentUserOrNull() ?: return
        val userId = user.id
        
        viewModelScope.launch {
            try {
                val post = supabase.postgrest["posts"]
                    .select {
                        filter {
                            eq("id", postId)
                        }
                    }
                    .decodeSingle<Post>()
                
                val newLikedBy = post.likedBy.toMutableList()
                var newLikes = post.likes
                
                if (newLikedBy.contains(userId)) {
                    newLikedBy.remove(userId)
                    newLikes--
                } else {
                    newLikedBy.add(userId)
                    newLikes++
                }
                
                supabase.postgrest["posts"].update(
                    mapOf(
                        "liked_by" to newLikedBy,
                        "likes" to newLikes
                    )
                ) {
                    filter {
                        eq("id", postId)
                    }
                }
                observePosts()
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Error toggling like", e)
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                supabase.postgrest["posts"].delete {
                    filter {
                        eq("id", postId)
                    }
                }
                observePosts()
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Error deleting post", e)
            }
        }
    }

    fun editPost(postId: String, newContent: String) {
        viewModelScope.launch {
            try {
                if (newContent == "ON" || newContent == "OFF") {
                    supabase.postgrest["posts"].update(
                        mapOf("comments_enabled" to (newContent == "ON"))
                    ) {
                        filter {
                            eq("id", postId)
                        }
                    }
                } else {
                    supabase.postgrest["posts"].update(
                        mapOf("content" to newContent)
                    ) {
                        filter {
                            eq("id", postId)
                        }
                    }
                }
                observePosts()
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Error editing post", e)
            }
        }
    }
}
