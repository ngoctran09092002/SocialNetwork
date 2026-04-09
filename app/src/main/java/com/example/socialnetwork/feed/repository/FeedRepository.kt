package com.example.socialnetwork.features.feed.repository

import android.util.Log
import com.example.socialnetwork.core.interfaces.IFeedRepository
import com.example.socialnetwork.core.models.Comment
import com.example.socialnetwork.core.models.Post
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FeedRepository : IFeedRepository {

    private val db = FirebaseFirestore.getInstance()
    private val TAG = "FeedRepository"

    override suspend fun getTimelinePosts(): List<Post> {
        return try {
            Log.d(TAG, "Loading posts from Firestore...")
            val snapshot = db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            Log.d(TAG, "Got ${snapshot.size()} documents from Firestore")

            val posts = snapshot.toObjects(Post::class.java)
            posts.forEach { post ->
                Log.d(TAG, "Post: id=${post.id}, caption=${post.caption}, likes=${post.likesCount}")
            }

            posts
        } catch (e: Exception) {
            Log.e(TAG, "Error loading posts: ${e.message}", e)
            emptyList()
        }
    }

    //  Node 5 thêm vào
    suspend fun getPostsByUser(userId: String): List<Post> {
        return try {
            val snapshot = db.collection("posts")
                .whereEqualTo("authorId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.toObjects(Post::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun createPost(post: Post): Boolean {
        return try {
            Log.d(TAG, "Creating post: ${post.id}")
            db.collection("posts")
                .document(post.id)
                .set(post)
                .await()
            Log.d(TAG, "Post created successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating post: ${e.message}", e)
            false
        }
    }

    override suspend fun likePost(postId: String, userId: String) {
        try {
            Log.d(TAG, "Liking post: $postId by user: $userId")
            val postRef = db.collection("posts").document(postId)
            val likeRef = postRef.collection("likes").document(userId)

            val likeDoc = likeRef.get().await()

            if (likeDoc.exists()) {
                Log.d(TAG, "Unlike post: $postId")
                likeRef.delete().await()
                postRef.update("likesCount", FieldValue.increment(-1)).await()
            } else {
                Log.d(TAG, "Like post: $postId")
                likeRef.set(mapOf("liked" to true, "timestamp" to System.currentTimeMillis())).await()
                postRef.update("likesCount", FieldValue.increment(1)).await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error liking post: ${e.message}", e)
            throw Exception("Failed to like/unlike post: ${e.message}")
        }
    }

    override suspend fun addComment(postId: String, comment: Comment) {
        try {
            Log.d(TAG, "Adding comment to post: $postId")
            val postRef = db.collection("posts").document(postId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(postRef)
                val currentCount = snapshot.getLong("commentCount") ?: 0
                transaction.update(postRef, "commentCount", currentCount + 1)
                val commentRef = postRef.collection("comments").document()
                transaction.set(commentRef, comment)
            }.await()
            Log.d(TAG, "Comment added successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding comment: ${e.message}", e)
            throw Exception("Failed to add comment: ${e.message}")
        }
    }

    override suspend fun getCommentCount(postId: String): Int {
        return try {
            val snapshot = db.collection("posts")
                .document(postId)
                .collection("comments")
                .get()
                .await()
            snapshot.size()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting comment count: ${e.message}", e)
            0
        }
    }
    suspend fun deletePost(postId: String): Boolean {
        return try {
            Log.d(TAG, "Deleting post: $postId")

            val postRef = db.collection("posts").document(postId)

            val likesSnapshot = postRef.collection("likes").get().await()
            for (likeDoc in likesSnapshot.documents) {
                likeDoc.reference.delete().await()
            }

            val commentsSnapshot = postRef.collection("comments").get().await()
            for (commentDoc in commentsSnapshot.documents) {
                commentDoc.reference.delete().await()
            }

            postRef.delete().await()

            Log.d(TAG, "Post deleted successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting post: ${e.message}", e)
            false
        }
    }
}