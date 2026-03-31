package com.example.socialnetwork.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.socialnetwork.core.models.Message
import com.example.socialnetwork.repository.ChatRepositoryImpl

class ChatViewModel : ViewModel() {

    // Khởi tạo Repository thực thi (Impl)
    private val repository = ChatRepositoryImpl()

    // Danh sách tin nhắn để Activity quan sát
    private val _messages = MutableLiveData<MutableList<Message>>(mutableListOf())
    val messages: LiveData<MutableList<Message>> = _messages

    /**
     * Bắt đầu lắng nghe tin nhắn giữa 2 người
     */
    fun startObservingMessages(senderId: String, receiverId: String) {
        // Tạo ChatRoomId giống logic trong Repository
        val chatRoomId = if (senderId < receiverId) "${senderId}_${receiverId}"
        else "${receiverId}_${senderId}"

        repository.observeMessages(chatRoomId) { newMessage ->
            // Khi có tin nhắn mới từ Firebase, thêm vào list hiện tại
            val currentList = _messages.value ?: mutableListOf()
            currentList.add(newMessage)
            _messages.postValue(currentList)
        }
    }

    /**
     * Hàm gọi gửi tin nhắn
     */
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
}