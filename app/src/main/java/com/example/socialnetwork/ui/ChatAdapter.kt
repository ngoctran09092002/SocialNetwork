package com.example.socialnetwork.ui

import android.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.socialnetwork.R
import com.example.socialnetwork.core.models.Message
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private val myId: String,
    private val onDeleteLocally: (Message) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
            holder.tvMsg.text = message.content
            holder.tvTime.text = time
            holder.itemView.setOnLongClickListener {
                try {
                    showDeleteDialog(holder.itemView, message)
                } catch (e: Exception) {
                    Log.e("CHAT_ADAPTER", "Error showing delete dialog", e)
                }
                true
            }
        } else if (holder is ReceivedViewHolder) {
            holder.tvMsg.text = message.content
            holder.tvTime.text = time
            holder.itemView.setOnLongClickListener {
                showDeleteDialog(holder.itemView, message)
                true
            }
        }
    }

    override fun getItemCount(): Int = messages.size
    class SentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMsg: TextView = view.findViewById(R.id.tvMessageSent)
        val tvTime: TextView = view.findViewById(R.id.tvTimeSent)
    }

    class ReceivedViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMsg: TextView = view.findViewById(R.id.tvMessageReceived)
        val tvTime: TextView = view.findViewById(R.id.tvTimeReceived)
    }

    fun updateMessages(newMessages: List<Message>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
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
    fun addOlderMessages(olderMessages: List<Message>) {
        messages.addAll(0, olderMessages) // thêm vào đầu danh sách
        notifyItemRangeInserted(0, olderMessages.size)
    }

    fun getOldestMessageTimestamp(): Long? {
        return messages.firstOrNull()?.timestamp
    }
}