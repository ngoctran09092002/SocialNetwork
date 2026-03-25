package com.example.socialnetwork.mock
import com.example.socialn.core.interfaces.IUserRepository
import com.example.socialnetwork.core.models.User

object  MockUserRepository : IUserRepository {
    private var currentUser = User(
        id = "me",
        name = "Lê Hoàng Hiếu Nghĩa Đệ Nhất Thương Tâm Nhân",
        avatarUrl = "profile",
        bio = "Nếu nói lầm lẫn lần này thì nói lại. Nói lầm lẫn lần nữa thì lại nói lại. Nói cho đến lúc luôn luôn lưu loát hết lầm lẫn mới thôi."
    )

    private val users = mutableListOf(
        User("1", "Messi", "messi", "GOAT"),
        User("2", "Ronaldo", "ronal", "SIUUU"),
        User("3", "Faker", "faker", "Mọi người có thể không nhiều cúp như anh nhưng anh vẫn tôn trọng. Đối với những kẻ không cúp - anh không chấp")
    )

    override suspend fun getUserProfile(userId: String): User? {
        return when (userId) {
            currentUser.id -> currentUser
            else -> users.find { it.id == userId }
        }
    }
    override suspend fun searchUsers(query: String): List<User> {
        return users.filter {
            it.name.contains(query, ignoreCase = true)
        }
    }
    suspend fun updateUser(user: User) {
        if (user.id == currentUser.id) {
            currentUser = user
        } else {
            val index = users.indexOfFirst { it.id == user.id }
            if (index != -1) {
                users[index] = user
            }
        }
    }
}
