package com.example.socialnetwork.repository

import android.util.Log
import com.example.socialnetwork.core.models.Message
import com.example.socialnetwork.core.interfaces.IChatRepository
import com.google.firebase.database.*

class ChatRepositoryImpl : IChatRepository {

    // Lấy config Firebase mặc định, giữ an toàn root node "chats"
    private val dbRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("chats")
    private var activeListener: ChildEventListener? = null
    private var activeRef: Query? = null

    override fun observeMessages(chatRoomId: String, onNewMessage: (Message) -> Unit) {
        removeListener()

        val ref = dbRef.child(chatRoomId)
            .child("messages")
            .orderByChild("timestamp")

        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                snapshot.getValue(Message::class.java)?.let { onNewMessage(it) }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(error: DatabaseError) {
                // Bắt buộc phải giữ log error để kiểm soát lỗi kết nối/quyền truy cập
                Log.e("ChatRepo", "Lỗi lắng nghe DB: ${error.code} - ${error.message}")
            }
        }

        activeRef = ref
        activeListener = listener
        ref.addChildEventListener(listener)
    }

    override fun sendMessage(message: Message) {
        val chatId = if (message.senderId < message.receiverId)
            "${message.senderId}_${message.receiverId}"
        else
            "${message.receiverId}_${message.senderId}"

        dbRef.child(chatId).child("messages").push().setValue(message)
            .addOnFailureListener { Log.e("ChatRepo", "Gửi tin nhắn thất bại: ${it.message}") }
    }

    override fun removeListener() {
        activeListener?.let { activeRef?.removeEventListener(it) }
        activeListener = null
        activeRef = null
    }
}