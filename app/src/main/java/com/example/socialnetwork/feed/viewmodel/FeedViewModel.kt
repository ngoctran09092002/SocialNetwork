package com.example.socialnetwork.feed.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialnetwork.core.models.Comment
import com.example.socialnetwork.core.models.Post
import com.example.socialnetwork.features.feed.repository.FeedRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FeedViewModel(
    private val feedRepository: FeedRepository,
    private val currentUserId: String
) : ViewModel() {
    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val TAG = "FeedViewModel"

    private val _likeStateChanged = MutableLiveData<Pair<String, Boolean>>()
    val likeStateChanged: LiveData<Pair<String, Boolean>> = _likeStateChanged

    private val _likeCountChanged = MutableLiveData<Pair<String, Int>>()
    val likeCountChanged: LiveData<Pair<String, Int>> = _likeCountChanged

    fun loadPosts() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val postsList = feedRepository.getTimelinePosts()
                _posts.value = postsList
                _error.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Error loading posts: ${e.message}", e)
                _error.value = "Failed to load posts"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun likePost(postId: String) {
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val likeDoc = db.collection("posts")
                    .document(postId)
                    .collection("likes")
                    .document(currentUserId)
                    .get()
                    .await()
                val isCurrentlyLiked = likeDoc.exists()

                // Gọi repository để like/unlike
                feedRepository.likePost(postId, currentUserId)

                // Cập nhật local list
                _posts.value?.let { currentPosts ->
                    val updatedPosts = currentPosts.map { post ->
                        if (post.id == postId) {
                            val newLikeCount = if (isCurrentlyLiked) {
                                post.likesCount - 1
                            } else {
                                post.likesCount + 1
                            }
                            post.copy(likesCount = newLikeCount)
                        } else {
                            post
                        }
                    }
                    _posts.value = updatedPosts
                }

                // Thông báo trạng thái like thay đổi
                _likeStateChanged.value = Pair(postId, !isCurrentlyLiked)
                // Thông báo số lượng like thay đổi
                val newLikeCount = if (isCurrentlyLiked) {
                    _posts.value?.find { it.id == postId }?.likesCount ?: 0
                } else {
                    _posts.value?.find { it.id == postId }?.likesCount ?: 0
                }
                _likeCountChanged.value = Pair(postId, newLikeCount)

            } catch (e: Exception) {
                Log.e(TAG, "Error liking post: ${e.message}", e)
                _error.value = "Failed to update like"
            }
        }
    }

    fun addComment(postId: String, commentText: String) {
        if (commentText.isBlank()) return

        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val userDoc = db.collection("users").document(currentUserId).get().await()
                val username = userDoc.getString("name") ?: "User"
                val avatarUrl = userDoc.getString("avatarUrl") ?: ""

                val comment = Comment(
                    postId = postId,
                    userId = currentUserId,
                    username = username,
                    avatarUrl = avatarUrl,
                    content = commentText,
                    timestamp = System.currentTimeMillis()
                )

                feedRepository.addComment(postId, comment)

                // Update local post list
                _posts.value?.let { currentPosts ->
                    val updatedPosts = currentPosts.map { post ->
                        if (post.id == postId) {
                            post.copy(commentCount = post.commentCount + 1)
                        } else {
                            post
                        }
                    }
                    _posts.value = updatedPosts
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding comment: ${e.message}", e)
                _error.value = "Failed to add comment"
            }
        }
    }
}