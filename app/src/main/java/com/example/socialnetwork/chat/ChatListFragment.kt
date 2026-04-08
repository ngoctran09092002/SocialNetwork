package com.example.socialnetwork.chat

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialnetwork.R
import com.example.socialnetwork.adapter.UserAdapter
import com.example.socialnetwork.core.models.User
import com.example.socialnetwork.ui.ChatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ChatListFragment : Fragment(R.layout.fragment_chat_list) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerUsers)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val adapter = UserAdapter { user ->
            val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                putExtra("receiverId", user.id)
                putExtra("receiverName", user.name)
                putExtra("receiverAvatar", user.avatarUrl)
            }
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = FirebaseFirestore.getInstance()
                    .collection("users")
                    .get()
                    .await()
                val users = snapshot.documents.mapNotNull { doc ->
                    val id = doc.id
                    if (id == currentUserId) return@mapNotNull null
                    User(
                        id = id,
                        name = doc.getString("name") ?: "Người dùng",
                        avatarUrl = doc.getString("avatarUrl") ?: "",
                        bio = doc.getString("bio") ?: ""
                    )
                }
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    adapter.updateList(users)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Lấy danh sách người dùng thất bại", Toast.LENGTH_SHORT).show()
                }
                }
        }
    }
}
