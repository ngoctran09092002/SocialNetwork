package com.example.socialnetwork.mock

import com.example.socialnetwork.core.interfaces.IAuthService

class MockAuthService : IAuthService {
    override fun getCurrentUserId(): String? {
        return "me"
    }
}