package com.example.socialnetwork.core.models

data class User(
    val id: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val bio: String = ""
)

data class Post(
    val id: String = "",
    val authorId: String = "",
    val imageUrl: String = "",
    val caption: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val commentCount: Int = 0
)

data class Comment(
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val avatarUrl: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class Message(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val type: String = "TEXT", // hoặc "IMAGE"
    val timestamp: Long = System.currentTimeMillis()
)


