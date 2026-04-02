package com.example.socialnetwork.feed.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
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

class PostAdapter(
    private var posts: List<Post> = emptyList(),
    private val onLikeClick: (Post) -> Unit,
    private val onCommentClick: (Post) -> Unit,
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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        val isLiked = likedPosts.contains(post.id)

        holder.txtCaption.text = post.caption
        holder.txtLikeCount.text = formatCount(post.likesCount, "like")
        holder.txtCommentCount.text = formatCount(post.commentCount, "comment")

        updateLikeButton(holder.btnLike, isLiked)

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

        loadUserInfo(holder, post.authorId)

        holder.btnLike.setOnClickListener {
            onLikeClick(post)
        }

        holder.btnComment.setOnClickListener {
            onCommentClick(post)
        }
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
                .placeholder(R.drawable.profile)
                .error(R.drawable.profile)
                .circleCrop()
                .into(holder.imgAvatar)
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val userDoc = db.collection("users").document(userId).get().await()
                val username = userDoc.getString("name") ?: "User ${userId.take(6)}"
                val avatarUrl = userDoc.getString("avatarUrl") ?: ""

                userCache[userId] = Pair(username, avatarUrl)

                holder.txtUserName.text = username
                Glide.with(holder.itemView.context)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .circleCrop()
                    .into(holder.imgAvatar)
            } catch (e: Exception) {
                holder.txtUserName.text = "User ${userId.take(6)}"
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
            count == 0 -> "No $word yet"
            count == 1 -> "1 $word"
            else -> "$count ${word}s"
        }
    }

    fun updatePosts(newPosts: List<Post>) {
        posts = newPosts
        loadLikedStates()
        notifyDataSetChanged()
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

    fun getPosts(): List<Post> = posts

    override fun getItemCount(): Int = posts.size
}