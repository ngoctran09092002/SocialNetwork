package com.example.socialnetwork.repository

import com.example.socialnetwork.core.models.Message
import com.example.socialnetwork.core.interfaces.IChatRepository
import com.google.firebase.database.*

class ChatRepositoryImpl : IChatRepository {

    // Reference tới node "chats" trong Firebase Realtime Database
    private val dbRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("chats")

    /**
     * Lắng nghe tin nhắn mới theo chatRoomId (ví dụ chatId = senderId_receiverId)
     */
    override fun observeMessages(chatRoomId: String, onNewMessage: (Message) -> Unit) {
        dbRef.child(chatRoomId).child("messages")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val msg = snapshot.getValue(Message::class.java)
                    msg?.let { onNewMessage(it) }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    /**
     * Gửi tin nhắn Message lên Firebase
     */
    override fun sendMessage(message: Message) {
        // Tạo chatId duy nhất dựa trên senderId và receiverId
        val chatId = if (message.senderId < message.receiverId)
            "${message.senderId}_${message.receiverId}"
        else
            "${message.receiverId}_${message.senderId}"

        dbRef.child(chatId).child("messages").push().setValue(message)
    }
}