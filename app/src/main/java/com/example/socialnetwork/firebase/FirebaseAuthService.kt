package com.example.socialnetwork.firebase

import com.example.socialn.core.interfaces.IAuthService
import com.google.firebase.auth.FirebaseAuth

class FirebaseAuthService : IAuthService {

    private val auth = FirebaseAuth.getInstance()

    override fun getCurrentUserId(): String? {
        return "testUser"
//        return auth.currentUser?.uid
    }
}