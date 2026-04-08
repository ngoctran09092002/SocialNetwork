package com.example.socialnetwork.feed.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialnetwork.R
import com.example.socialnetwork.core.models.Post
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostAdapter(
    private var posts: List<Post> = emptyList(),
    private val onLikeClick: (Post) -> Unit,
    private val onCommentClick: (Post) -> Unit,
    private val onDeletePost: (Post, Int) -> Unit,  // Thêm callback xóa post
    private val currentUserId: String
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private val userCache = mutableMapOf<String, Pair<String, String>>()
    private val likedPosts = mutableSetOf<String>()

    inner class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgPost: ImageView = view.findViewById(R.id.imgPost)
        val txtCaption: TextView = view.findViewById(R.id.txtCaption)
        val txtLikeCount: TextView = view.findViewById(R.id.txtLikeCount)
        val txtCommentCount: TextView = view.findViewById(R.id.txtCommentCount)
        val btnLike: ImageButton = view.findViewById(R.id.btnLike)
        val btnComment: ImageButton = view.findViewById(R.id.btnComment)
        val txtUserName: TextView = view.findViewById(R.id.txtUserName)
        val imgAvatar: ImageView = view.findViewById(R.id.imgAvatar)
        val txtPostTime: TextView = view.findViewById(R.id.txtPostTime)
        val btnDeletePost: ImageButton = view.findViewById(R.id.btnDeletePost)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        val isLiked = likedPosts.contains(post.id)

        holder.txtPostTime.text = formatTimestamp(post.timestamp)
        holder.txtCaption.text = post.caption

        // Format số lượng like và comment bằng tiếng Việt
        holder.txtLikeCount.text = formatCount(post.likesCount, "thích")
        holder.txtCommentCount.text = formatCount(post.commentCount, "bình luận")

        updateLikeButton(holder.btnLike, isLiked)

<<<<<<< Updated upstream
        Glide.with(holder.itemView.context)
            .load(post.imageUrl)
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_background)
            .into(holder.imgPost)
=======
        // Hiển thị ảnh post
        if (post.imageUrl.isNullOrEmpty()) {
            holder.imgPost.visibility = View.GONE
        } else {
            holder.imgPost.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(post.imageUrl)
                .placeholder(R.color.bg_screen)
                .error(R.color.bg_screen)
                .into(holder.imgPost)
        }
>>>>>>> Stashed changes

        loadUserInfo(holder, post.authorId)

        // Hiển thị nút xóa nếu là chủ post
        holder.btnDeletePost.visibility = if (post.authorId == currentUserId) View.VISIBLE else View.GONE

        holder.btnDeletePost.setOnClickListener {
            showDeleteConfirmation(holder.itemView, post, position)
        }

        holder.btnLike.setOnClickListener {
            onLikeClick(post)
        }

        holder.btnComment.setOnClickListener {
            onCommentClick(post)
        }
    }

    private fun showDeleteConfirmation(view: View, post: Post, position: Int) {
        AlertDialog.Builder(view.context)
            .setTitle("Xóa bài viết")
            .setMessage("Bạn có chắc chắn muốn xóa bài viết này không?")
            .setPositiveButton("Xóa") { _, _ ->
                onDeletePost(post, position)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun updateLikeButton(button: ImageButton, isLiked: Boolean) {
        if (isLiked) {
            button.setColorFilter(ContextCompat.getColor(button.context, R.color.red))
            button.setImageResource(R.drawable.ic_like_filled)
        } else {
            button.clearColorFilter()
            button.setImageResource(R.drawable.ic_like)
        }
    }

    private fun loadUserInfo(holder: PostViewHolder, userId: String) {
        userCache[userId]?.let { (name, avatarUrl) ->
            holder.txtUserName.text = name
            Glide.with(holder.itemView.context)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .circleCrop()
                .into(holder.imgAvatar)
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val userDoc = db.collection("users").document(userId).get().await()
                val username = userDoc.getString("name") ?: "Người dùng ${userId.take(6)}"
                val avatarUrl = userDoc.getString("avatarUrl") ?: ""

                userCache[userId] = Pair(username, avatarUrl)

                holder.txtUserName.text = username
                Glide.with(holder.itemView.context)
                    .load(avatarUrl)
                    .placeholder(R.drawable.profile)
                    .error(R.drawable.profile)
                    .circleCrop()
                    .into(holder.imgAvatar)
            } catch (e: Exception) {
                holder.txtUserName.text = "Người dùng ${userId.take(6)}"
            }
        }
    }

    fun updateLikeState(postId: String, isLiked: Boolean) {
        val position = posts.indexOfFirst { it.id == postId }
        if (position != -1) {
            if (isLiked) {
                likedPosts.add(postId)
            } else {
                likedPosts.remove(postId)
            }
            notifyItemChanged(position)
        }
    }

    fun loadLikedStates() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val db = FirebaseFirestore.getInstance()
                likedPosts.clear()
                for (post in posts) {
                    val likeDoc = db.collection("posts")
                        .document(post.id)
                        .collection("likes")
                        .document(currentUserId)
                        .get()
                        .await()
                    if (likeDoc.exists()) {
                        likedPosts.add(post.id)
                    }
                }
                notifyDataSetChanged()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun formatCount(count: Int, word: String): String {
        return when {
            count == 0 -> "0 $word"
            count == 1 -> "1 $word"
            else -> "$count $word"
        }
    }

    fun updatePosts(newPosts: List<Post>) {
        posts = newPosts
        loadLikedStates()
        notifyDataSetChanged()
    }

    fun removePost(position: Int) {
        if (position in posts.indices) {
            posts = posts.toMutableList().apply { removeAt(position) }
            notifyItemRemoved(position)
        }
    }

    fun updatePostCommentCount(postId: String, newCommentCount: Int) {
        val position = posts.indexOfFirst { it.id == postId }
        if (position != -1) {
            val updatedPost = posts[position].copy(commentCount = newCommentCount)
            posts = posts.toMutableList().apply {
                this[position] = updatedPost
            }
            notifyItemChanged(position)
        }
    }

    fun updatePostLikeCount(postId: String, newLikeCount: Int) {
        val position = posts.indexOfFirst { it.id == postId }
        if (position != -1) {
            val updatedPost = posts[position].copy(likesCount = newLikeCount)
            posts = posts.toMutableList().apply {
                this[position] = updatedPost
            }
            notifyItemChanged(position)
        }
    }

<<<<<<< Updated upstream
=======
    fun clearUserCache() {
        userCache.clear()
        notifyDataSetChanged()
    }

>>>>>>> Stashed changes
    private fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val now = Date()
        val diff = now.time - timestamp

        return when {
            diff < 60000 -> "Vừa xong"
            diff < 3600000 -> "${diff / 60000} phút trước"
            diff < 86400000 -> "${diff / 3600000} giờ trước"
            else -> SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(date)
        }
    }

    fun getPosts(): List<Post> = posts

    override fun getItemCount(): Int = posts.size
}