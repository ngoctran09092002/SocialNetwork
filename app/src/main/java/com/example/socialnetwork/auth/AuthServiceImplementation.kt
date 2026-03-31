package com.example.socialnetwork.auth

import android.content.Intent
import com.example.socialnetwork.core.interfaces.IAuthService
import com.google.firebase.auth.FirebaseAuth

class AuthServiceImplementation : IAuthService {
    override fun getCurrentUserId(): String? {
        val user = FirebaseAuth.getInstance().currentUser

        return user?.uid ?: "test_user_id_seminar"
    }

    fun performLogout(activity: android.app.Activity) {
        // 1. Đăng xuất khỏi Firebase
        FirebaseAuth.getInstance().signOut()

        // 2. Chuyển về màn hình Login và xóa toàn bộ lịch sử các màn hình trước đó
        val intent = Intent(activity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        activity.startActivity(intent)
        activity.finish()
    }
}