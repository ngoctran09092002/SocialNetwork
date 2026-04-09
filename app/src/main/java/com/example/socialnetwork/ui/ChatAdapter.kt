package com.example.socialnetwork.ui

import android.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialnetwork.R
import com.example.socialnetwork.core.models.Message
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private val myId: String,
    private var otherAvatarUrl: String = "",
    private val onDeleteLocally: (Message) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    fun setOtherAvatar(url: String) {
        otherAvatarUrl = url
    }

    companion object {
        private const val TYPE_SENT = 1
        private const val TYPE_RECEIVED = 2
    }

    private val messages = mutableListOf<Message>()

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == myId) TYPE_SENT else TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_SENT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_sent, parent, false)
            SentViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_received, parent, false)
            ReceivedViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))

        if (holder is SentViewHolder) {
            holder.tvTime.text = time
            holder.itemView.setOnLongClickListener {
                try { showDeleteDialog(holder.itemView, message) } catch (e: Exception) {
                    Log.e("ChatAdapter", "Delete dialog error", e)
                }
                true
            }
            if (message.type == "IMAGE") {
                holder.tvMsg.visibility = View.GONE
                holder.imgChat.visibility = View.VISIBLE
                Glide.with(holder.itemView.context).load(message.content)
                    .placeholder(R.color.bg_screen).into(holder.imgChat)
            } else {
                holder.tvMsg.visibility = View.VISIBLE
                holder.imgChat.visibility = View.GONE
                holder.tvMsg.text = message.content
            }
        } else if (holder is ReceivedViewHolder) {
            holder.tvTime.text = time
            // Load avatar người gửi
            if (otherAvatarUrl.isNotEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(otherAvatarUrl)
                    .placeholder(R.drawable.profile)
                    .error(R.drawable.profile)
                    .circleCrop()
                    .into(holder.imgAvatar)
            }
            holder.itemView.setOnLongClickListener {
                showDeleteDialog(holder.itemView, message)
                true
            }
            if (message.type == "IMAGE") {
                holder.tvMsg.visibility = View.GONE
                holder.imgChat.visibility = View.VISIBLE
                Glide.with(holder.itemView.context).load(message.content)
                    .placeholder(R.color.bg_screen).into(holder.imgChat)
            } else {
                holder.tvMsg.visibility = View.VISIBLE
                holder.imgChat.visibility = View.GONE
                holder.tvMsg.text = message.content
            }
        }
    }

    override fun getItemCount(): Int = messages.size

    class SentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMsg: TextView = view.findViewById(R.id.tvMessageSent)
        val tvTime: TextView = view.findViewById(R.id.tvTimeSent)
        val imgChat: ImageView = view.findViewById(R.id.imgChatSent)
    }

    class ReceivedViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMsg: TextView = view.findViewById(R.id.tvMessageReceived)
        val tvTime: TextView = view.findViewById(R.id.tvTimeReceived)
        val imgChat: ImageView = view.findViewById(R.id.imgChatReceived)
        val imgAvatar: ImageView = view.findViewById(R.id.imgSenderAvatar)
    }

    fun updateMessages(newMessages: List<Message>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    fun addOlderMessages(olderMessages: List<Message>) {
        messages.addAll(0, olderMessages)
        notifyItemRangeInserted(0, olderMessages.size)
    }

    fun getOldestMessageTimestamp(): Long? {
        return messages.firstOrNull()?.timestamp
    }

    private fun showDeleteDialog(view: View, message: Message) {
        AlertDialog.Builder(view.context)
            .setTitle("Xóa tin nhắn")
            .setMessage("Bạn có chắc muốn xóa tin nhắn này không?")
            .setPositiveButton("Xóa") { dialog, _ ->
                onDeleteLocally(message)
                dialog.dismiss()
            }
            .setNegativeButton("Hủy") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
