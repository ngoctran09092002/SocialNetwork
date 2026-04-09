package com.example.socialnetwork.core.interfaces

import com.example.socialnetwork.core.models.Comment
import com.example.socialnetwork.core.models.Post
import com.example.socialnetwork.core.models.Message
import com.example.socialnetwork.core.models.User

// Node 1 (Auth) phải implement cái này. Các Node khác gọi nó để lấy ID user đang đăng nhập.
interface IAuthService {
    fun getCurrentUserId(): String?
}

// Node 3 (Media) phải implement. Node 2 (Feed) và Node 4 (Chat) gọi nó để up ảnh.
interface IMediaService {
    // Nhận vào đường dẫn file ảnh, trả về Link URL trên Firebase Storage
    suspend fun uploadImage(fileUri: String): String
}

// Node 2 (Feed) implement. Xử lý logic tải bài viết.
interface IFeedRepository {
    suspend fun getTimelinePosts(currentUserId: String = ""): List<Post>
    suspend fun createPost(post: Post): Boolean
    suspend fun likePost(postId: String, userId: String)
    suspend fun addComment(postId: String, comment: Comment)
    suspend fun getCommentCount(postId: String): Int
}

// Node 4 (Chat) implement. Lắng nghe tin nhắn realtime.
interface IChatRepository {
    fun observeMessages(chatRoomId: String, onNewMessage: (Message) -> Unit)
    fun sendMessage(message: Message)
    fun removeListener()
}

// Node 5 (Profile) implement.
interface IUserRepository {
    suspend fun getUserProfile(userId: String): User?
    suspend fun searchUsers(query: String): List<User>
    suspend fun getUserPostCount(userId: String): Int
    suspend fun getFriendCount(userId: String): Int
    suspend fun updateUser(user: User): Boolean
}