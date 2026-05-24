package com.example.subtrackai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subtrackai.model.Comment
import com.example.subtrackai.model.Post
import com.example.subtrackai.model.PostInsert
import com.example.subtrackai.model.PostUpdate
import com.example.subtrackai.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

class FeedViewModel : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()
    
    val feedPosts: StateFlow<List<Post>> = combine(_posts, _searchQuery) { posts, query ->
        if (query.isBlank()) posts
        else posts.filter { it.content.contains(query, ignoreCase = true) || it.authorName.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

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

    fun refreshPosts() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val postList = supabase.postgrest["posts"]
                    .select {
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<Post>()
                _posts.value = postList
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Error refreshing posts: ${e.message}", e)
            } finally {
                _isRefreshing.value = false
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
                Log.d("FV_DEBUG", "getComments: fetched ${comments.size} comments for post $postId")
                onResult(comments)
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Error fetching comments", e)
                onResult(emptyList())
            }
        }
    }

    fun createPost(content: String, authorName: String, profilePost: Boolean = false, profileIcon: String? = "Person", avatarUrl: String? = null) {
        val user = supabase.auth.currentUserOrNull() ?: return
        val data = PostInsert(
            userId = user.id,
            authorName = authorName,
            content = content,
            profilePost = profilePost,
            profileIcon = profileIcon,
            avatarUrl = avatarUrl
        )
        viewModelScope.launch {
            try {
                supabase.postgrest["posts"].insert(Json.encodeToJsonElement(data))
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
                    // USER ALREADY LIKED - REMOVE IT
                    newLikedBy.remove(userId)
                    newLikes--
                } else {
                    // NEW LIKE
                    newLikedBy.add(userId)
                    newLikes++
                }
                
                // FORCE LIKES TO NEVER BE NEGATIVE
                if (newLikes < 0) newLikes = 0

                supabase.postgrest["posts"].update(
                    buildJsonObject {
                        put("likes", newLikes)
                        put("liked_by", Json.encodeToJsonElement(ListSerializer(String.serializer()), newLikedBy))
                    }
                ) {
                    filter {
                        eq("id", postId)
                    }
                }
                observePosts()
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Error toggling like: ${e.message}")
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
                Log.e("FeedViewModel", "Error deleting post: ${e.message}")
            }
        }
    }

    fun editPost(postId: String, newContent: String) {
        viewModelScope.launch {
            try {
                if (newContent == "ON" || newContent == "OFF") {
                    val data = PostUpdate(commentsEnabled = newContent == "ON")
                    supabase.postgrest["posts"].update(Json.encodeToJsonElement(data)) {
                        filter {
                            eq("id", postId)
                        }
                    }
                } else {
                    val data = PostUpdate(content = newContent)
                    supabase.postgrest["posts"].update(Json.encodeToJsonElement(data)) {
                        filter {
                            eq("id", postId)
                        }
                    }
                }
                observePosts()
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Error editing post: ${e.message}")
            }
        }
    }
}
