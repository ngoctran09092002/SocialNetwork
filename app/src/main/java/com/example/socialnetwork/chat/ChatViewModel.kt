package com.example.socialnetwork.chat

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.socialnetwork.core.models.Message

class ChatViewModel : ViewModel() {

    private val repository = ChatRepositoryImpl()
    private val _messages = MutableLiveData<List<Message>>(emptyList())
    val messages: LiveData<List<Message>> = _messages

    // Tránh đăng ký listener nhiều lần khi Activity rotate
    private var isObserving = false

    fun startObservingMessages(senderId: String, receiverId: String) {
        if (isObserving) return
        isObserving = true

        val chatRoomId = if (senderId < receiverId) "${senderId}_${receiverId}"
        else "${receiverId}_${senderId}"
        Log.d("CHAT_DEBUG", "OBSERVE chatId: $chatRoomId")
        repository.observeMessages(chatRoomId) { newMessage ->
            val updated = _messages.value.orEmpty().toMutableList()
            updated.add(newMessage)
            _messages.postValue(updated)
        }
    }

    fun sendMessage(content: String, senderId: String, receiverId: String) {
        Log.d("CHAT_DEBUG", "senderId: $senderId")
        if (content.isBlank()) return
        val message = Message(
            senderId = senderId,
            receiverId = receiverId,
            content = content,
            timestamp = System.currentTimeMillis()
        )
        repository.sendMessage(message)
    }


    override fun onCleared() {
        super.onCleared()
        repository.removeListener()
    }
}