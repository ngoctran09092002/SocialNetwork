package com.example.socialnetwork.chat

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.socialnetwork.core.models.Message
import com.example.socialnetwork.repository.ChatRepositoryImpl
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChatRepositoryImpl()
    private val context = application.applicationContext

    private val _messages = MutableLiveData<List<Message>>(emptyList())
    val messages: LiveData<List<Message>> = _messages

    private val messageMap = TreeMap<Long, Message>() // key = timestamp

    private val prefs = context.getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)
    private val firestore = FirebaseFirestore.getInstance()

    fun startObservingMessages(senderId: String, receiverId: String) {

        repository.removeListener()
        messageMap.clear()
        _messages.postValue(emptyList())

        val chatRoomId = if (senderId < receiverId)
            "${senderId}_${receiverId}"
        else
            "${receiverId}_${senderId}"

        Log.d("CHAT", "OBSERVE chatId: $chatRoomId")

        repository.observeMessages(chatRoomId) { newMessage ->
            val deletedIds = getDeletedMessageIds(chatRoomId)
            if (newMessage.id in deletedIds) return@observeMessages
            messageMap[newMessage.timestamp] = newMessage
            _messages.postValue(messageMap.values.toList())
        }
    }
    fun sendMessage(content: String, senderId: String, receiverId: String) {
        Log.d("CHAT_DEBUG", "senderId: $senderId")
        if (content.isBlank()) return

        val timestamp = System.currentTimeMillis()
        val message = Message(
            id = "",
            senderId = senderId,
            receiverId = receiverId,
            content = content,
            timestamp = timestamp
        )

        repository.sendMessage(message)
    }
    private fun getDeletedMessageIds(chatRoomId: String): MutableSet<String> {
        return prefs.getStringSet(chatRoomId, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
    }
    private fun saveDeletedMessageId(chatRoomId: String, messageId: String) {
        val set = getDeletedMessageIds(chatRoomId)
        set.add(messageId)
        prefs.edit().putStringSet(chatRoomId, set).apply()
    }
    fun deleteAllMessagesForMe(senderId: String, receiverId: String) {
        val chatRoomId = if (senderId < receiverId)
            "${senderId}_${receiverId}"
        else
            "${receiverId}_${senderId}"
        messageMap.values.forEach { saveDeletedMessageId(chatRoomId, it.id) }
        messageMap.clear()
        _messages.postValue(emptyList())
    }
    fun deleteMessageForMeLocally(message: Message, senderId: String) {
        val chatRoomId = if (senderId < message.receiverId)
            "${senderId}_${message.receiverId}"
        else
            "${message.receiverId}_${senderId}"
        messageMap.entries.removeIf { it.value.id == message.id }
        _messages.postValue(messageMap.values.toList())
        saveDeletedMessageId(chatRoomId, message.id)
    }
    fun getOlderMessages(
        uid: String,
        receiverId: String,
        oldestTimestamp: Long,
        callback: (List<Message>) -> Unit
    ) {
        val chatRoomId = if (uid < receiverId)
            "${uid}_${receiverId}"
        else
            "${receiverId}_${uid}"
        val messagesRef = firestore.collection("messages")
            .whereEqualTo("chatRoomId", chatRoomId)
            .whereLessThan("timestamp", oldestTimestamp)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(10)

        messagesRef.get().addOnSuccessListener { snapshot ->
            val olderMessages = snapshot.toObjects(Message::class.java).sortedBy { it.timestamp }
            Log.d("CHAT_DEBUG", "Older messages count: ${olderMessages.size}")
            callback(olderMessages)
        }
    }
    override fun onCleared() {
        super.onCleared()
        repository.removeListener()
    }
}