package com.example.socialnetwork.chat

import android.util.Log
import com.example.socialnetwork.core.models.Message
import com.example.socialnetwork.core.interfaces.IChatRepository
import com.google.firebase.database.*

class ChatRepositoryImpl : IChatRepository {

    private val dbRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("chats")
    private var activeListener: ChildEventListener? = null
    private var activeRef: Query? = null
    override fun observeMessages(chatRoomId: String, onNewMessage: (Message) -> Unit) {
        removeListener()

        val ref = dbRef.child(chatRoomId)
            .child("messages")
            .orderByChild("timestamp") // đảm bảo thứ tự theo thời gian

        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
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
            .addOnFailureListener { Log.e("CHAT", "Gửi tin nhắn thất bại: ${it.message}") }
            .addOnSuccessListener { Log.d("CHAT", "Tin nhắn đã gửi lên Firebase") }
    }
    override fun removeListener() {
        activeListener?.let { activeRef?.removeEventListener(it) }
        activeListener = null
        activeRef = null
    }
}