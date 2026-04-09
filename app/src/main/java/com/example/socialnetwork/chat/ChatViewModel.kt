package com.example.socialnetwork.chat

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.socialnetwork.core.models.Message
import java.util.*

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChatRepositoryImpl()
    private val context = application.applicationContext

    private val _messages = MutableLiveData<List<Message>>(emptyList())
    val messages: LiveData<List<Message>> = _messages

    private val messageMap = TreeMap<Long, Message>() // key = timestamp
    private val prefs = context.getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)

    private var currentChatRoomId = ""

    fun startObservingMessages(senderId: String, receiverId: String) {
        repository.removeListener()
        messageMap.clear()
        _messages.postValue(emptyList())

        currentChatRoomId = buildChatRoomId(senderId, receiverId)
        Log.d("CHAT", "OBSERVE chatId: $currentChatRoomId")

        // Lắng nghe tin nhắn bị xóa (realtime cả hai bên)
        repository.onMessageRemoved = { removedId ->
            messageMap.entries.removeIf { it.value.id == removedId }
            _messages.postValue(messageMap.values.toList())
        }

        repository.observeMessages(currentChatRoomId) { newMessage ->
            val deletedIds = getDeletedMessageIds(currentChatRoomId)
            if (newMessage.id in deletedIds) return@observeMessages
            messageMap[newMessage.timestamp] = newMessage
            _messages.postValue(messageMap.values.toList())
        }
    }

    fun sendMessage(content: String, senderId: String, receiverId: String, type: String = "TEXT") {
        if (content.isBlank()) return
        val message = Message(
            senderId = senderId,
            receiverId = receiverId,
            content = content,
            type = type,
            timestamp = System.currentTimeMillis()
        )
        repository.sendMessage(message)
    }

    fun getOlderMessages(
        uid: String,
        receiverId: String,
        oldestTimestamp: Long,
        callback: (List<Message>) -> Unit
    ) {
        val chatRoomId = buildChatRoomId(uid, receiverId)
        val deletedIds = getDeletedMessageIds(chatRoomId)

        repository.getOlderMessages(chatRoomId, oldestTimestamp) { olderMessages ->
            val filtered = olderMessages.filter { it.id !in deletedIds }
            // Thêm vào messageMap để tránh trùng lặp
            filtered.forEach { messageMap[it.timestamp] = it }
            Log.d("CHAT", "Loaded ${filtered.size} older messages")
            callback(filtered)
        }
    }

    fun deleteAllMessagesForMe(senderId: String, receiverId: String) {
        val chatRoomId = buildChatRoomId(senderId, receiverId)
        messageMap.values.forEach { saveDeletedMessageId(chatRoomId, it.id) }
        messageMap.clear()
        _messages.postValue(emptyList())
    }

    fun deleteMessageForEveryone(message: Message, myId: String) {
        val otherId = if (message.senderId == myId) message.receiverId else message.senderId
        val chatRoomId = buildChatRoomId(myId, otherId)
        // Xóa khỏi Firebase → cả hai bên mất
        repository.deleteMessage(chatRoomId, message.id)
        // Xóa khỏi local ngay lập tức
        messageMap.entries.removeIf { it.value.id == message.id }
        _messages.postValue(messageMap.values.toList())
    }

    private fun buildChatRoomId(a: String, b: String): String {
        return if (a < b) "${a}_${b}" else "${b}_${a}"
    }

    private fun getDeletedMessageIds(chatRoomId: String): MutableSet<String> {
        return prefs.getStringSet(chatRoomId, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
    }

    private fun saveDeletedMessageId(chatRoomId: String, messageId: String) {
        val set = getDeletedMessageIds(chatRoomId)
        set.add(messageId)
        prefs.edit().putStringSet(chatRoomId, set).apply()
    }

    override fun onCleared() {
        super.onCleared()
        repository.removeListener()
    }
}
