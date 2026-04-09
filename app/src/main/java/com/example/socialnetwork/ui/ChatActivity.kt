package com.example.socialnetwork.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialnetwork.R
import com.example.socialnetwork.auth.LoginActivity
import com.example.socialnetwork.chat.ChatViewModel
import com.example.socialnetwork.core.models.Message
import com.google.firebase.auth.FirebaseAuth

class ChatActivity : AppCompatActivity() {

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter

    private var uid: String = ""
    private val receiverId by lazy { intent.getStringExtra("receiverId") ?: "" }
    private val receiverName by lazy { intent.getStringExtra("receiverName") ?: "Chat" }
    private val receiverAvatar by lazy { intent.getStringExtra("receiverAvatar") ?: "" }

    private lateinit var edtMessage: EditText
    private lateinit var rvChat: RecyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        //  Setup UI Toolbar
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tvReceiverName).text = receiverName
        val imgAvatar = findViewById<ImageView>(R.id.imgReceiverAvatar)
        if (receiverAvatar.isNotEmpty()) {
            Glide.with(this).load(receiverAvatar).circleCrop().into(imgAvatar)
        }
        //  Firebase Auth
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        } else {
            uid = auth.currentUser?.uid ?: ""
            initChat()
        }
    }

    private fun initChat() {

        if (uid.isEmpty() || receiverId.isEmpty()) {
            Log.e("CHAT", "UID hoặc receiverId chưa sẵn sàng")
            return
        }
        rvChat = findViewById(R.id.rvChat)
        edtMessage = findViewById(R.id.edtMessage)
        val btnSend = findViewById<ImageButton>(R.id.btnSend)
        val btnDelete = findViewById<ImageButton>(R.id.btnDelete)

        chatAdapter = ChatAdapter(uid) { message ->
            viewModel.deleteMessageForMeLocally(message, uid)
        }

        val layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvChat.layoutManager = layoutManager
        rvChat.adapter = chatAdapter
        rvChat.isNestedScrollingEnabled = true
        rvChat.overScrollMode = RecyclerView.OVER_SCROLL_ALWAYS
        edtMessage.post {
            edtMessage.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(edtMessage, InputMethodManager.SHOW_IMPLICIT)
        }
        viewModel.startObservingMessages(uid, receiverId)
        viewModel.messages.observe(this) { list ->
            val lastVisible = layoutManager.findLastVisibleItemPosition()
            val isAtBottom = lastVisible >= chatAdapter.itemCount - 2

            chatAdapter.updateMessages(list)

            if (isAtBottom && chatAdapter.itemCount > 0) {
                rvChat.scrollToPosition(chatAdapter.itemCount - 1)
            }
        }
        btnSend.setOnClickListener {
            val content = edtMessage.text.toString().trim()
            if (content.isNotEmpty()) {
                viewModel.sendMessage(content, uid, receiverId)
                edtMessage.text.clear()
            }
        }

        btnDelete.setOnClickListener {
            viewModel.deleteAllMessagesForMe(uid, receiverId)
        }
        rvChat.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val firstVisible = layoutManager.findFirstVisibleItemPosition()
                if (firstVisible >= 0 && firstVisible <= 2) {
                    loadOlderMessages()
                }
            }
        })
    }
    private fun loadOlderMessages() {
        // Lấy timestamp của tin nhắn cũ nhất hiện có
        val oldestTimestamp = chatAdapter.getOldestMessageTimestamp() ?: return
        viewModel.getOlderMessages(uid, receiverId, oldestTimestamp) { olderMessages ->
            if (olderMessages.isNotEmpty()) {
                val firstVisible = (rvChat.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                val offsetView = rvChat.getChildAt(0)
                val offset = offsetView?.top ?: 0

                chatAdapter.addOlderMessages(olderMessages)
                (rvChat.layoutManager as LinearLayoutManager)
                    .scrollToPositionWithOffset(firstVisible + olderMessages.size, offset)
            }
        }
    }
}