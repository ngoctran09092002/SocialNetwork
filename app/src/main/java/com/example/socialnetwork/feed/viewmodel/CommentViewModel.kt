package com.example.socialnetwork.feed.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialnetwork.core.models.Comment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CommentViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _comments = MutableLiveData<List<Comment>>()
    val comments: LiveData<List<Comment>> = _comments

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadComments(postId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val snapshot = db.collection("posts")
                    .document(postId)
                    .collection("comments")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val commentsList = snapshot.toObjects(Comment::class.java)
                _comments.value = commentsList
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load comments"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addComment(postId: String, text: String, userId: String, username: String, avatarUrl: String) {
        viewModelScope.launch {
            try {
                val commentId = FirebaseFirestore.getInstance().collection("posts")
                    .document(postId)
                    .collection("comments")
                    .document().id

                val comment = Comment(
                    id = commentId,
                    postId = postId,
                    userId = userId,
                    username = username,
                    avatarUrl = avatarUrl,
                    content = text,
                    timestamp = System.currentTimeMillis()
                )

                val postRef = db.collection("posts").document(postId)

                db.runTransaction { transaction ->
                    val snapshot = transaction.get(postRef)
                    val currentCount = snapshot.getLong("commentCount") ?: 0
                    transaction.update(postRef, "commentCount", currentCount + 1)

                    val commentRef = postRef.collection("comments").document(commentId)
                    transaction.set(commentRef, comment)
                }.await()

                loadComments(postId)

            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to add comment"
            }
        }
    }

    fun deleteComment(postId: String, commentId: String, currentUserId: String, commentAuthorId: String, postAuthorId: String) {
        if (currentUserId != commentAuthorId && currentUserId != postAuthorId) {
            _error.value = "You don't have permission to delete this comment"
            return
        }

        viewModelScope.launch {
            try {
                val postRef = db.collection("posts").document(postId)
                val commentRef = postRef.collection("comments").document(commentId)

                db.runTransaction { transaction ->
                    val snapshot = transaction.get(postRef)
                    val currentCount = snapshot.getLong("commentCount") ?: 0
                    transaction.update(postRef, "commentCount", currentCount - 1)
                    transaction.delete(commentRef)
                }.await()

                loadComments(postId)

            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete comment"
            }
        }
    }

}