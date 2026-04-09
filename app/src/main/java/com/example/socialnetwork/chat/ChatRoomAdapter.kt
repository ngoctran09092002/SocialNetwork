package com.example.socialnetwork.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialnetwork.R
import com.example.socialnetwork.core.models.ChatRoom
import java.text.SimpleDateFormat
import java.util.*

class ChatRoomAdapter(
    private val myId: String,
    private val onClick: (ChatRoom) -> Unit
) : RecyclerView.Adapter<ChatRoomAdapter.ViewHolder>() {

    private val items = mutableListOf<ChatRoom>()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgAvatar: ImageView = view.findViewById(R.id.imgAvatar)
        val txtName: TextView = view.findViewById(R.id.txtName)
        val txtLastMessage: TextView = view.findViewById(R.id.txtLastMessage)
        val txtTime: TextView = view.findViewById(R.id.txtTime)
        val txtPendingBadge: TextView = view.findViewById(R.id.txtPendingBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_room, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val room = items[position]

        holder.txtName.text = room.getOtherUserName(myId)
        holder.txtLastMessage.text = if (room.lastMessageType == "IMAGE") "Đã gửi ảnh" else room.lastMessage

        if (room.lastMessageTime > 0) {
            holder.txtTime.text = formatTime(room.lastMessageTime)
        } else {
            holder.txtTime.text = ""
        }

        // Pending badge
        when {
            room.isPendingForMe(myId) -> {
                holder.txtPendingBadge.visibility = View.VISIBLE
                holder.txtPendingBadge.text = "Chờ chấp nhận"
            }
            room.status == ChatRoom.STATUS_PENDING && room.initiatorId == myId -> {
                holder.txtPendingBadge.visibility = View.VISIBLE
                holder.txtPendingBadge.text = "Đang chờ"
                holder.txtPendingBadge.setTextColor(
                    holder.itemView.context.getColor(R.color.text_secondary)
                )
            }
            else -> holder.txtPendingBadge.visibility = View.GONE
        }

        val avatarUrl = room.getOtherUserAvatar(myId)
        Glide.with(holder.itemView.context)
            .load(avatarUrl)
            .placeholder(R.drawable.profile)
            .error(R.drawable.profile)
            .circleCrop()
            .into(holder.imgAvatar)

        holder.itemView.setOnClickListener { onClick(room) }
    }

    override fun getItemCount() = items.size

    fun updateList(newItems: List<ChatRoom>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    private fun formatTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < 60_000 -> "Vừa xong"
            diff < 3600_000 -> "${diff / 60_000} phút"
            diff < 86400_000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
            else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
