
package com.example.socialnetwork.ui
import android.content.Context
import android.content.Intent
import android.view.inputmethod.InputMethodManager
import android.os.Bundle
import android.util.Log
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
import com.example.socialnetwork.core.models.Message
import com.example.socialnetwork.chat.ChatViewModel
import com.google.firebase.auth.FirebaseAuth

class ChatActivity : AppCompatActivity() {

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter

    // Global UID
    private var uid: String = ""

    // Lấy receiver info từ Intent (sẽ pass từ danh sách chat)
    //private val receiverId by lazy { intent.getStringExtra("receiverId") ?: "" }
    private val receiverId by lazy { intent.getStringExtra("receiverId") ?: "" }
    private val receiverName by lazy { intent.getStringExtra("receiverName") ?: "Chat" }
    private val receiverAvatar by lazy { intent.getStringExtra("receiverAvatar") ?: "" }
    private lateinit var edtMessage: EditText
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
            // Chưa login → chuyển sang màn login
            startActivity(Intent(this, LoginActivity::class.java))
            Log.d("AUTH", "User already logged in: $uid")
            finish()
            return
        } else {
            uid = auth.currentUser?.uid ?: ""
            initChat()
        }
    }

    private fun initChat() {
        // Check an toàn
        if (uid.isEmpty()) {
            Log.e("CHAT", "UID chưa sẵn sàng")
            return
        }
        if (receiverId.isEmpty()) {
            Log.e("CHAT", "receiverId chưa được truyền từ Intent")
            // Có thể show Toast hoặc finish() activity
            return
        }

        // Bắt đầu nghe tin nhắn
        viewModel.startObservingMessages(uid, receiverId)

        val rvChat = findViewById<RecyclerView>(R.id.rvChat)
         edtMessage = findViewById(R.id.edtMessage)
        edtMessage.post {
            edtMessage.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(edtMessage, InputMethodManager.SHOW_IMPLICIT)
        }
        val btnSend = findViewById<ImageButton>(R.id.btnSend)

        chatAdapter = ChatAdapter(uid)
        val layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rvChat.layoutManager = layoutManager
        rvChat.adapter = chatAdapter

        //  Observe messages từ ViewModel
        viewModel.messages.observe(this) { list ->
            chatAdapter.messages = list.toMutableList()
            chatAdapter.notifyDataSetChanged()
            if (list.isNotEmpty()) {
                rvChat.scrollToPosition(list.size - 1)
            }
        }

        //  Nút gửi
        btnSend.setOnClickListener {
            if (uid.isEmpty()) {
                Log.e("SEND", "UID chưa sẵn sàng")
                return@setOnClickListener
            }

            val content = edtMessage.text.toString().trim()
            if (content.isNotEmpty()) {
                viewModel.sendMessage(content, uid, receiverId)
                edtMessage.text.clear()
            }
        }
    }
}