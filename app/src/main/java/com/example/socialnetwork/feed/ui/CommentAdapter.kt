package com.example.socialnetwork.feed.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialnetwork.R
import com.example.socialnetwork.core.models.Comment
import java.text.SimpleDateFormat
import java.util.*

class CommentAdapter(
    private val comments: List<Comment>,
    private val currentUserId: String,
    private val postAuthorId: String,
    private val onDeleteClick: (Comment) -> Unit
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    inner class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgAvatar: ImageView = view.findViewById(R.id.imgAvatar)
        val txtUsername: TextView = view.findViewById(R.id.txtUsername)
        val txtComment: TextView = view.findViewById(R.id.txtComment)
        val txtTime: TextView = view.findViewById(R.id.txtTime)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]

        holder.txtUsername.text = comment.username
        holder.txtComment.text = comment.content
        holder.txtTime.text = formatTimestamp(comment.timestamp)

        Glide.with(holder.itemView.context)
            .load(comment.avatarUrl)
            .placeholder(R.drawable.profile)
            .error(R.drawable.profile)
            .circleCrop()
            .into(holder.imgAvatar)

        // CHỈ hiển thị nút xóa nếu là chủ comment HOẶC chủ post
        val canDelete = comment.userId == currentUserId || postAuthorId == currentUserId
        holder.btnDelete.visibility = if (canDelete) View.VISIBLE else View.GONE

        holder.btnDelete.setOnClickListener {
            onDeleteClick(comment)
        }
    }

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

    override fun getItemCount(): Int = comments.size
}