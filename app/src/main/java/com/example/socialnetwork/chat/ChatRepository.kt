package com.example.socialnetwork.chat

import android.util.Log
import com.example.socialnetwork.core.models.Message
import com.example.socialnetwork.core.interfaces.IChatRepository
import com.google.firebase.database.*

class ChatRepositoryImpl : IChatRepository {

    private val dbRef: DatabaseReference = FirebaseDatabase
        .getInstance()
        .getReference("chats")
    private var activeListener: ChildEventListener? = null
    private var activeRef: Query? = null

    override fun observeMessages(chatRoomId: String, onNewMessage: (Message) -> Unit) {
        removeListener()
        Log.d("ChatRepo", "=== observeMessages path: chats/$chatRoomId/messages ===")

        // Debug: đọc 1 lần để kiểm tra data có tồn tại không
        dbRef.child(chatRoomId).child("messages")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("ChatRepo", "DEBUG: Tổng tin nhắn trong DB = ${snapshot.childrenCount}")
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChatRepo", "DEBUG: Đọc DB thất bại: ${error.code} - ${error.message}")
                }
            })

        val ref = dbRef.child(chatRoomId)
            .child("messages")
            .orderByChild("timestamp")

        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                Log.d("ChatRepo", "onChildAdded: key=${snapshot.key}, value=${snapshot.value}")
                snapshot.getValue(Message::class.java)?.let { oldMessage ->
                    val message = oldMessage.copy(id = snapshot.key ?: "")
                    onNewMessage(message)
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
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

        val ref = dbRef.child(chatId).child("messages").push()
        val messageWithId = message.copy(id = ref.key ?: "")

        ref.setValue(messageWithId)
            .addOnFailureListener { Log.e("ChatRepo", "Gửi tin nhắn thất bại: ${it.message}") }
            .addOnSuccessListener { Log.d("ChatRepo", "Tin nhắn đã gửi") }
    }

    /**
     * Lấy tin nhắn cũ hơn timestamp cho trước (phân trang).
     * Query Realtime Database — cùng DB với observeMessages.
     */
    fun getOlderMessages(
        chatRoomId: String,
        beforeTimestamp: Long,
        limit: Int = 20,
        callback: (List<Message>) -> Unit
    ) {
        dbRef.child(chatRoomId).child("messages")
            .orderByChild("timestamp")
            .endBefore(beforeTimestamp.toDouble())
            .limitToLast(limit)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messages = snapshot.children.mapNotNull { child ->
                        child.getValue(Message::class.java)?.copy(id = child.key ?: "")
                    }.sortedBy { it.timestamp }
                    Log.d("ChatRepo", "Loaded ${messages.size} older messages")
                    callback(messages)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChatRepo", "Lỗi load lịch sử: ${error.message}")
                    callback(emptyList())
                }
            })
    }

    override fun removeListener() {
        activeListener?.let { activeRef?.removeEventListener(it) }
        activeListener = null
        activeRef = null
    }
}
