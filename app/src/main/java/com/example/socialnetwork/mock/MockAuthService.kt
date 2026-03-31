package com.example.socialnetwork.mock

import com.example.socialn.core.interfaces.IAuthService

class MockAuthService : IAuthService {
    override fun getCurrentUserId(): String? {
        return "me"
    }
}