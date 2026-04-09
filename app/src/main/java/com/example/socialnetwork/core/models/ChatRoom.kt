package com.example.socialnetwork.core.models

data class ChatRoom(
    val chatRoomId: String = "",
    val user1Id: String = "",
    val user2Id: String = "",
    val initiatorId: String = "",
    val status: String = STATUS_PENDING,
    val lastMessage: String = "",
    val lastMessageType: String = "TEXT",
    val lastMessageTime: Long = 0L,
    // Cache tên + avatar để hiển thị nhanh
    val user1Name: String = "",
    val user2Name: String = "",
    val user1Avatar: String = "",
    val user2Avatar: String = "",
    val deletedBy: List<String> = emptyList(),
    val blockedBy: List<String> = emptyList()
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_ACCEPTED = "ACCEPTED"
        const val STATUS_REJECTED = "REJECTED"
    }

    /** Lấy ID của người đối diện */
    fun getOtherUserId(myId: String): String =
        if (myId == user1Id) user2Id else user1Id

    fun getOtherUserName(myId: String): String =
        if (myId == user1Id) user2Name else user1Name

    fun getOtherUserAvatar(myId: String): String =
        if (myId == user1Id) user2Avatar else user1Avatar

    /** Mình có phải người nhận lời mời không */
    fun isPendingForMe(myId: String): Boolean =
        status == STATUS_PENDING && initiatorId != myId
}
