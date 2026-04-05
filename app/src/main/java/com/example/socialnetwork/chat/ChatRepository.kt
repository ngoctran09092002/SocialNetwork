package com.example.socialnetwork.chat

import com.example.socialnetwork.core.models.Message
import com.example.socialnetwork.core.interfaces.IChatRepository
import com.google.firebase.database.*

class ChatRepositoryImpl : IChatRepository {

    private val dbRef: DatabaseReference = FirebaseDatabase.getInstance().getReference()
    private var activeListener: ChildEventListener? = null
    private var activeRef: Query? = null

    override fun observeMessages(
        chatRoomId: String,
        onNewMessage: (Message) -> Unit
    ) {
        removeListener()

        val ref = dbRef.child(chatRoomId)
            .child("messages")
            .orderByChild("timestamp")

        val listener = object : ChildEventListener {

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {

                snapshot.getValue(Message::class.java)?.let {
                    onNewMessage(it) //
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onChildRemoved(snapshot: DataSnapshot) {}

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(error: DatabaseError) {}
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
    }

    override fun removeListener() {
        activeListener?.let { activeRef?.removeEventListener(it) }
        activeListener = null
        activeRef = null
    }
}
