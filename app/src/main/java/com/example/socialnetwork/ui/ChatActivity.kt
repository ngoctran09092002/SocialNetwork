package com.example.socialnetwork.ui

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialnetwork.R
import com.example.socialnetwork.viewmodel.ChatViewModel

class ChatActivity : AppCompatActivity() {

    // 1. Khai báo ViewModel và Adapter
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter
    private val myId = "user_A"
    private val receiverId = "user_B"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat) // Kết nối với file XML bạn đã vẽ

        // 2. Ánh xạ View từ XML
        val rvChat = findViewById<RecyclerView>(R.id.rvChat)
        val edtMessage = findViewById<EditText>(R.id.edtMessage)
        val btnSend = findViewById<ImageButton>(R.id.btnSend)
        val btnAttach = findViewById<ImageButton>(R.id.btnAttach)

        // 3. Thiết lập RecyclerView
        chatAdapter = ChatAdapter(myId)
        rvChat.layoutManager = LinearLayoutManager(this)
        rvChat.adapter = chatAdapter

        // 4. Lắng nghe dữ liệu từ ViewModel (Logic Real-time)
        viewModel.messages.observe(this) { list ->
            chatAdapter.messages = list.toMutableList()
            chatAdapter.notifyDataSetChanged()
            rvChat.scrollToPosition(list.size - 1)
        }

        // Bắt đầu quan sát tin nhắn trên Firebase
        viewModel.startObservingMessages(myId, receiverId)

        // 5. Logic nút Gửi
        btnSend.setOnClickListener {
            val content = edtMessage.text.toString()
            if (content.isNotEmpty()) {
                viewModel.sendMessage(content, myId, receiverId)
                edtMessage.text.clear()
            }
        }

        // 6. Logic nút Tệp
        btnAttach.setOnClickListener {
        }
    }
}