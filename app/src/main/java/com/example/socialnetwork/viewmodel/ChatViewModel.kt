package com.example.socialnetwork.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.socialnetwork.core.models.Message
import com.example.socialnetwork.repository.ChatRepositoryImpl

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

        repository.observeMessages(chatRoomId) { newMessage ->
            val updated = _messages.value.orEmpty().toMutableList()
            updated.add(newMessage)
            _messages.postValue(updated)
        }
    }

    fun sendMessage(content: String, senderId: String, receiverId: String) {
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
