package com.example.socialnetwork.firebase

import android.util.Log
import com.example.socialn.core.interfaces.IUserRepository
import com.example.socialnetwork.core.models.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseUserRepository : IUserRepository {

    private val db = FirebaseFirestore.getInstance()
    private val usersRef = db.collection("users")

    override suspend fun getUserProfile(userId: String): User? {
        return try {
            val doc = usersRef.document(userId).get().await()

            Log.d("FIREBASE", "doc exists: ${doc.exists()}")
            Log.d("FIREBASE", "data: ${doc.data}")

            val userWithoutId = doc.toObject(User::class.java)
            if (userWithoutId != null) {
                userWithoutId.copy(id = doc.id)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("FIREBASE", "error", e)
            null
        }
    }

    override suspend fun searchUsers(query: String): List<User> {
        if (query.isBlank()) return emptyList()
        return try {
            val snapshot = usersRef.get().await()
            snapshot.documents.mapNotNull { it.toObject(User::class.java)?.copy(id = it.id) }
                .filter { it.name.contains(query, ignoreCase = true) }
        } catch (e: Exception) {
            Log.e("FIREBASE", "search error", e)
            emptyList()
        }
    }

    suspend fun getAllUsers(): List<User> {
        return try {
            val snapshot = usersRef.get().await()
            snapshot.documents.mapNotNull { it.toObject(User::class.java)?.copy(id = it.id) }
        } catch (e: Exception) {
            Log.e("FIREBASE", "getAllUsers error", e)
            emptyList()
        }
    }

    suspend fun updateUser(user: User) {
        try {
            usersRef.document(user.id).set(user).await()
        } catch (e: Exception) {
            Log.e("FIREBASE", "updateUser error", e)
        }
    }
}