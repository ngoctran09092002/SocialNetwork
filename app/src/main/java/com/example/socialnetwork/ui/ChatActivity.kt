package com.example.socialnetwork.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialnetwork.R
import com.example.socialnetwork.auth.LoginActivity
import com.example.socialnetwork.chat.ChatViewModel
import com.example.socialnetwork.core.models.ChatRoom
import com.example.socialnetwork.util.CloudinaryUploader
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ChatActivity : AppCompatActivity() {

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter
    private val db = FirebaseFirestore.getInstance()

    private var uid: String = ""
    private val receiverId by lazy { intent.getStringExtra("receiverId") ?: "" }
    private val receiverName by lazy { intent.getStringExtra("receiverName") ?: "Chat" }
    private val receiverAvatar by lazy { intent.getStringExtra("receiverAvatar") ?: "" }

    private lateinit var edtMessage: EditText
    private lateinit var rvChat: RecyclerView
    private lateinit var inputBar: View
    private lateinit var requestBar: View
    private var chatRoomId = ""
    private var chatRoomStatus = ""
    private lateinit var blockedBar: View
    private var isBlockedByMe = false
    private var isBlockedByOther = false

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        Toast.makeText(this, "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val imageUrl = CloudinaryUploader.upload(this@ChatActivity, uri)
                withContext(Dispatchers.Main) {
                    sendMessageWithRoomCheck(imageUrl, "IMAGE")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChatActivity, "Gửi ảnh thất bại", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Toolbar
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tvReceiverName).text = receiverName
        val imgAvatar = findViewById<ImageView>(R.id.imgReceiverAvatar)
        if (receiverAvatar.isNotEmpty()) {
            Glide.with(this).load(receiverAvatar).circleCrop().into(imgAvatar)
        }

        // Auth check
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        uid = auth.currentUser?.uid ?: ""
        chatRoomId = buildChatRoomId(uid, receiverId)

        initChat()
        checkChatRoomStatus()
    }

    private fun initChat() {
        if (uid.isEmpty() || receiverId.isEmpty()) return

        rvChat = findViewById(R.id.rvChat)
        edtMessage = findViewById(R.id.edtMessage)
        inputBar = findViewById(R.id.inputBar)
        requestBar = findViewById(R.id.requestBar)
        blockedBar = findViewById(R.id.blockedBar)
        val btnSend = findViewById<ImageButton>(R.id.btnSend)
        val btnPickImage = findViewById<ImageButton>(R.id.btnPickImage)
        val btnChatSettings = findViewById<ImageButton>(R.id.btnChatSettings)

        btnChatSettings.setOnClickListener { showChatSettingsDialog() }

        chatAdapter = ChatAdapter(uid) { message ->
            viewModel.deleteMessageForEveryone(message, uid)
        }

        val layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rvChat.layoutManager = layoutManager
        rvChat.adapter = chatAdapter

        viewModel.startObservingMessages(uid, receiverId)

        viewModel.messages.observe(this) { list ->
            val lastVisible = layoutManager.findLastVisibleItemPosition()
            val isAtBottom = lastVisible >= chatAdapter.itemCount - 2
            chatAdapter.updateMessages(list)
            if (isAtBottom && chatAdapter.itemCount > 0) {
                rvChat.scrollToPosition(chatAdapter.itemCount - 1)
            }
        }

        val sendAction = {
            val content = edtMessage.text.toString().trim()
            if (content.isNotEmpty()) {
                sendMessageWithRoomCheck(content, "TEXT")
                edtMessage.text.clear()
            }
        }

        btnSend.setOnClickListener { sendAction() }

        edtMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendAction()
                true
            } else false
        }

        btnPickImage.setOnClickListener { pickImage.launch("image/*") }

        // Accept / Reject buttons
        requestBar.findViewById<View>(R.id.btnAccept)?.setOnClickListener {
            acceptChatRequest()
        }
        requestBar.findViewById<View>(R.id.btnReject)?.setOnClickListener {
            rejectChatRequest()
        }

        rvChat.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val firstVisible = layoutManager.findFirstVisibleItemPosition()
                if (firstVisible in 0..2) loadOlderMessages()
            }
        })
    }

    /**
     * Kiểm tra trạng thái chatRoom: PENDING / ACCEPTED / chưa tồn tại
     */
    private fun checkChatRoomStatus() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val doc = db.collection("chatRooms").document(chatRoomId).get().await()
                val room = doc.toObject(ChatRoom::class.java)
                withContext(Dispatchers.Main) {
                    // Kiểm tra block
                    if (room != null) {
                        isBlockedByMe = uid in room.blockedBy
                        isBlockedByOther = receiverId in room.blockedBy
                        if (isBlockedByMe) {
                            inputBar.visibility = View.GONE
                            requestBar.visibility = View.GONE
                            blockedBar.visibility = View.VISIBLE
                            blockedBar.findViewById<TextView>(R.id.txtBlockedMessage)?.text = "Bạn đã chặn người này"
                            return@withContext
                        }
                        if (isBlockedByOther) {
                            inputBar.visibility = View.GONE
                            requestBar.visibility = View.GONE
                            blockedBar.visibility = View.VISIBLE
                            blockedBar.findViewById<TextView>(R.id.txtBlockedMessage)?.text = "Bạn đã bị chặn"
                            return@withContext
                        }
                        blockedBar.visibility = View.GONE
                    }

                    if (room == null) {
                        // Chưa có chatRoom → user mới, sẽ tạo khi gửi tin nhắn đầu tiên
                        chatRoomStatus = ""
                        inputBar.visibility = View.VISIBLE
                        requestBar.visibility = View.GONE
                    } else {
                        chatRoomStatus = room.status
                        when {
                            room.status == ChatRoom.STATUS_REJECTED -> {
                                // Đã bị từ chối trước đó → cho phép gửi lại (sẽ reset thành PENDING)
                                chatRoomStatus = ""
                                inputBar.visibility = View.VISIBLE
                                requestBar.visibility = View.GONE
                                Toast.makeText(this@ChatActivity, "Cuộc trò chuyện trước đã bị từ chối. Gửi tin nhắn để gửi lời mời lại.", Toast.LENGTH_LONG).show()
                            }
                            room.isPendingForMe(uid) -> {
                                // Mình là người nhận → hiện accept/reject, ẩn input
                                requestBar.visibility = View.VISIBLE
                                inputBar.visibility = View.GONE
                            }
                            room.status == ChatRoom.STATUS_PENDING && room.initiatorId == uid -> {
                                // Mình là người gửi, đang chờ đối phương chấp nhận
                                inputBar.visibility = View.VISIBLE
                                requestBar.visibility = View.GONE
                                Toast.makeText(this@ChatActivity, "Đang chờ đối phương chấp nhận", Toast.LENGTH_SHORT).show()
                            }
                            room.status == ChatRoom.STATUS_ACCEPTED -> {
                                inputBar.visibility = View.VISIBLE
                                requestBar.visibility = View.GONE
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatActivity", "checkChatRoomStatus error: ${e.message}")
            }
        }
    }

    /**
     * Gửi tin nhắn + tạo/cập nhật chatRoom metadata
     */
    private fun sendMessageWithRoomCheck(content: String, type: String) {
        viewModel.sendMessage(content, uid, receiverId, type)

        // Cập nhật chatRoom metadata
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val myName = FirebaseAuth.getInstance().currentUser?.displayName ?: ""
                val docRef = db.collection("chatRooms").document(chatRoomId)
                val doc = docRef.get().await()

                val existingRoom = doc.toObject(ChatRoom::class.java)
                val needsNewRoom = !doc.exists() || existingRoom?.status == ChatRoom.STATUS_REJECTED

                if (needsNewRoom) {
                    // Tạo chatRoom mới hoặc reset từ REJECTED → PENDING
                    val myProfile = db.collection("users").document(uid).get().await()
                    val receiverProfile = db.collection("users").document(receiverId).get().await()

                    val u1Id = if (uid < receiverId) uid else receiverId
                    val u2Id = if (uid < receiverId) receiverId else uid
                    val u1Name = if (uid < receiverId) (myProfile.getString("name") ?: "") else (receiverProfile.getString("name") ?: "")
                    val u2Name = if (uid < receiverId) (receiverProfile.getString("name") ?: "") else (myProfile.getString("name") ?: "")
                    val u1Avatar = if (uid < receiverId) (myProfile.getString("avatarUrl") ?: "") else (receiverProfile.getString("avatarUrl") ?: "")
                    val u2Avatar = if (uid < receiverId) (receiverProfile.getString("avatarUrl") ?: "") else (myProfile.getString("avatarUrl") ?: "")

                    val room = ChatRoom(
                        chatRoomId = chatRoomId,
                        user1Id = u1Id,
                        user2Id = u2Id,
                        initiatorId = uid,
                        status = ChatRoom.STATUS_PENDING,
                        lastMessage = content,
                        lastMessageType = type,
                        lastMessageTime = System.currentTimeMillis(),
                        user1Name = u1Name,
                        user2Name = u2Name,
                        user1Avatar = u1Avatar,
                        user2Avatar = u2Avatar
                    )
                    docRef.set(room).await()
                    withContext(Dispatchers.Main) {
                        chatRoomStatus = ChatRoom.STATUS_PENDING
                    }
                } else {
                    // Cập nhật last message + xóa UID khỏi deletedBy (nếu có) để chat hiện lại
                    val updates = mutableMapOf<String, Any>(
                        "lastMessage" to content,
                        "lastMessageType" to type,
                        "lastMessageTime" to System.currentTimeMillis()
                    )
                    // Xóa mình khỏi deletedBy nếu đã xóa chat trước đó
                    val currentDeleted = existingRoom?.deletedBy?.toMutableList() ?: mutableListOf()
                    if (uid in currentDeleted) {
                        currentDeleted.remove(uid)
                        updates["deletedBy"] = currentDeleted
                    }
                    docRef.update(updates).await()
                }
            } catch (e: Exception) {
                Log.e("ChatActivity", "updateChatRoom error: ${e.message}")
            }
        }
    }

    private fun acceptChatRequest() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.collection("chatRooms").document(chatRoomId)
                    .update("status", ChatRoom.STATUS_ACCEPTED).await()
                withContext(Dispatchers.Main) {
                    chatRoomStatus = ChatRoom.STATUS_ACCEPTED
                    requestBar.visibility = View.GONE
                    inputBar.visibility = View.VISIBLE
                    Toast.makeText(this@ChatActivity, "Đã chấp nhận", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChatActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun rejectChatRequest() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.collection("chatRooms").document(chatRoomId)
                    .update("status", ChatRoom.STATUS_REJECTED).await()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChatActivity, "Đã từ chối", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChatActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadOlderMessages() {
        val oldestTimestamp = chatAdapter.getOldestMessageTimestamp() ?: return
        viewModel.getOlderMessages(uid, receiverId, oldestTimestamp) { olderMessages ->
            if (olderMessages.isNotEmpty()) {
                val lm = rvChat.layoutManager as LinearLayoutManager
                val firstVisible = lm.findFirstVisibleItemPosition()
                val offsetView = rvChat.getChildAt(0)
                val offset = offsetView?.top ?: 0
                chatAdapter.addOlderMessages(olderMessages)
                lm.scrollToPositionWithOffset(firstVisible + olderMessages.size, offset)
            }
        }
    }

    private fun showChatSettingsDialog() {
        val options = if (isBlockedByMe) {
            arrayOf("Bỏ chặn người này")
        } else {
            arrayOf("Chặn người này")
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(receiverName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> if (isBlockedByMe) unblockUser() else blockUser()
                }
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    private fun blockUser() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Chặn $receiverName?")
            .setMessage("Người này sẽ không thể gửi tin nhắn cho bạn.")
            .setPositiveButton("Chặn") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val docRef = db.collection("chatRooms").document(chatRoomId)
                        val doc = docRef.get().await()
                        if (doc.exists()) {
                            val current = doc.toObject(ChatRoom::class.java)
                            val updated = current?.blockedBy?.toMutableList() ?: mutableListOf()
                            if (uid !in updated) updated.add(uid)
                            docRef.update("blockedBy", updated).await()
                        } else {
                            // Tạo chatRoom mới nếu chưa có
                            docRef.set(mapOf("blockedBy" to listOf(uid), "chatRoomId" to chatRoomId)).await()
                        }
                        withContext(Dispatchers.Main) {
                            isBlockedByMe = true
                            inputBar.visibility = View.GONE
                            requestBar.visibility = View.GONE
                            blockedBar.visibility = View.VISIBLE
                            blockedBar.findViewById<TextView>(R.id.txtBlockedMessage)?.text = "Bạn đã chặn người này"
                            Toast.makeText(this@ChatActivity, "Đã chặn $receiverName", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ChatActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun unblockUser() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val docRef = db.collection("chatRooms").document(chatRoomId)
                val doc = docRef.get().await()
                val current = doc.toObject(ChatRoom::class.java)
                val updated = current?.blockedBy?.toMutableList() ?: mutableListOf()
                updated.remove(uid)
                docRef.update("blockedBy", updated).await()
                withContext(Dispatchers.Main) {
                    isBlockedByMe = false
                    blockedBar.visibility = View.GONE
                    inputBar.visibility = View.VISIBLE
                    Toast.makeText(this@ChatActivity, "Đã bỏ chặn $receiverName", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChatActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun buildChatRoomId(a: String, b: String): String {
        return if (a < b) "${a}_${b}" else "${b}_${a}"
    }
}
