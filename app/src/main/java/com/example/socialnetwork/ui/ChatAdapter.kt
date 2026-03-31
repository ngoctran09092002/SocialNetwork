package com.example.socialnetwork.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.socialnetwork.R
import com.example.socialnetwork.core.models.Message
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(private val myId: String) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_SENT = 1
    private val TYPE_RECEIVED = 2
    var messages = mutableListOf<Message>()

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == myId) TYPE_SENT else TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_SENT) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_sent, parent, false)
            SentViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_received, parent, false)
            ReceivedViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))

        if (holder is SentViewHolder) {
            holder.tvMsg.text = message.content
            holder.tvTime.text = time
        } else if (holder is ReceivedViewHolder) {
            holder.tvMsg.text = message.content
            holder.tvTime.text = time
        }
    }

    override fun getItemCount() = messages.size

    class SentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMsg: TextView = view.findViewById(R.id.tvMessageSent)
        val tvTime: TextView = view.findViewById(R.id.tvTimeSent)
    }

    class ReceivedViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMsg: TextView = view.findViewById(R.id.tvMessageReceived)
        val tvTime: TextView = view.findViewById(R.id.tvTimeReceived)
    }
}