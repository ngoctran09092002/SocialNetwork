package com.example.socialnetwork.repository

import android.util.Log
import com.example.socialnetwork.core.models.Message
import com.example.socialnetwork.core.interfaces.IChatRepository
import com.google.firebase.database.*

class ChatRepositoryImpl : IChatRepository {

    private val dbRef: DatabaseReference = FirebaseDatabase
        .getInstance("https://androi-2-410ab-default-rtdb.firebaseio.com")
        .getReference("chats")
    private var activeListener: ChildEventListener? = null
    private var activeRef: Query? = null

    override fun observeMessages(chatRoomId: String, onNewMessage: (Message) -> Unit) {
        removeListener()
        Log.d("ChatRepo", "Bắt đầu lắng nghe chatRoomId=$chatRoomId")

        val ref = dbRef.child(chatRoomId).child("messages")
            .orderByChild("timestamp")

        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                Log.d("ChatRepo", "Nhận message: ${snapshot.value}")
                snapshot.getValue(Message::class.java)?.let { onNewMessage(it) }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatRepo", "Lỗi lắng nghe: ${error.code} - ${error.message}")
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

        Log.d("ChatRepo", "Gửi tin nhắn vào chatId=$chatId")
        dbRef.child(chatId).child("messages").push().setValue(message)
            .addOnSuccessListener { Log.d("ChatRepo", "Gửi thành công") }
            .addOnFailureListener { Log.e("ChatRepo", "Gửi thất bại: ${it.message}") }
    }

    override fun removeListener() {
        activeListener?.let { activeRef?.removeEventListener(it) }
        activeListener = null
        activeRef = null
    }
}
