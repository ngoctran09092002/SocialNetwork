package com.example.socialnetwork.ui

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialnetwork.R
import com.example.socialnetwork.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth

class ChatActivity : AppCompatActivity() {

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter
    private val myId by lazy { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    private val receiverId by lazy { intent.getStringExtra("receiverId") ?: "" }
    private val receiverName by lazy { intent.getStringExtra("receiverName") ?: "Chat" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = receiverName
        toolbar.setNavigationOnClickListener { finish() }

        val rvChat = findViewById<RecyclerView>(R.id.rvChat)
        val edtMessage = findViewById<EditText>(R.id.edtMessage)
        val btnSend = findViewById<ImageButton>(R.id.btnSend)

        chatAdapter = ChatAdapter(myId)
        val layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvChat.layoutManager = layoutManager
        rvChat.adapter = chatAdapter

        viewModel.messages.observe(this) { list ->
            chatAdapter.messages = list.toMutableList()
            chatAdapter.notifyDataSetChanged()
            if (list.isNotEmpty()) rvChat.scrollToPosition(list.size - 1)
        }

        if (myId.isNotEmpty() && receiverId.isNotEmpty()) {
            viewModel.startObservingMessages(myId, receiverId)
        }

        btnSend.setOnClickListener {
            val content = edtMessage.text.toString().trim()
            if (content.isNotEmpty()) {
                viewModel.sendMessage(content, myId, receiverId)
                edtMessage.text.clear()
            }
        }
    }
}
