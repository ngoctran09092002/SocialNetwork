package com.example.socialnetwork.chat

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialnetwork.R
import com.example.socialnetwork.adapter.UserAdapter
import com.example.socialnetwork.core.models.ChatRoom
import com.example.socialnetwork.core.models.User
import com.example.socialnetwork.ui.ChatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ChatListFragment : Fragment(R.layout.fragment_chat_list) {

    private val db = FirebaseFirestore.getInstance()
    private var currentUserId = ""
    private var searchJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val edtSearch = view.findViewById<EditText>(R.id.edtSearch)
        val searchResultsContainer = view.findViewById<View>(R.id.searchResultsContainer)
        val chatListContainer = view.findViewById<View>(R.id.chatListContainer)
        val recyclerSearchResults = view.findViewById<RecyclerView>(R.id.recyclerSearchResults)
        val recyclerPending = view.findViewById<RecyclerView>(R.id.recyclerPending)
        val recyclerChats = view.findViewById<RecyclerView>(R.id.recyclerChats)
        val txtPendingHeader = view.findViewById<TextView>(R.id.txtPendingHeader)
        val txtChatsHeader = view.findViewById<TextView>(R.id.txtChatsHeader)
        val emptyState = view.findViewById<View>(R.id.emptyState)
        val txtEmpty = view.findViewById<TextView>(R.id.txtEmpty)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        // --- Search results adapter ---
        val searchAdapter = UserAdapter { user ->
            openChat(user.id, user.name, user.avatarUrl)
            edtSearch.text.clear()
        }
        recyclerSearchResults.layoutManager = LinearLayoutManager(requireContext())
        recyclerSearchResults.adapter = searchAdapter

        // --- Pending requests adapter ---
        val pendingAdapter = ChatRoomAdapter(currentUserId,
            onClick = { room ->
                openChat(room.getOtherUserId(currentUserId), room.getOtherUserName(currentUserId), room.getOtherUserAvatar(currentUserId))
            },
            onLongClick = { room -> showDeleteDialog(room) }
        )
        recyclerPending.layoutManager = LinearLayoutManager(requireContext())
        recyclerPending.adapter = pendingAdapter

        // --- Accepted chats adapter ---
        val chatsAdapter = ChatRoomAdapter(currentUserId,
            onClick = { room ->
                openChat(room.getOtherUserId(currentUserId), room.getOtherUserName(currentUserId), room.getOtherUserAvatar(currentUserId))
            },
            onLongClick = { room -> showDeleteDialog(room) }
        )
        recyclerChats.layoutManager = LinearLayoutManager(requireContext())
        recyclerChats.adapter = chatsAdapter

        // --- Search logic ---
        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.length >= 2) {
                    searchResultsContainer.visibility = View.VISIBLE
                    chatListContainer.visibility = View.GONE
                    searchUsers(query, searchAdapter)
                } else {
                    searchResultsContainer.visibility = View.GONE
                    chatListContainer.visibility = View.VISIBLE
                }
            }
        })

        // --- Load chat rooms ---
        loadChatRooms(pendingAdapter, chatsAdapter, txtPendingHeader, txtChatsHeader, emptyState, progressBar)
    }

    override fun onResume() {
        super.onResume()
        // Refresh khi quay lại từ ChatActivity
        view?.let {
            val pendingAdapter = it.findViewById<RecyclerView>(R.id.recyclerPending)?.adapter as? ChatRoomAdapter
            val chatsAdapter = it.findViewById<RecyclerView>(R.id.recyclerChats)?.adapter as? ChatRoomAdapter
            val txtPendingHeader = it.findViewById<TextView>(R.id.txtPendingHeader)
            val txtChatsHeader = it.findViewById<TextView>(R.id.txtChatsHeader)
            val emptyState = it.findViewById<View>(R.id.emptyState)
            val progressBar = it.findViewById<ProgressBar>(R.id.progressBar)
            if (pendingAdapter != null && chatsAdapter != null) {
                loadChatRooms(pendingAdapter, chatsAdapter, txtPendingHeader!!, txtChatsHeader!!, emptyState!!, progressBar!!)
            }
        }
    }

    private fun searchUsers(query: String, adapter: UserAdapter) {
        searchJob?.cancel()
        searchJob = CoroutineScope(Dispatchers.IO).launch {
            delay(300) // debounce
            try {
                val queryLower = query.lowercase()
                val snapshot = db.collection("users").get().await()

                val users = snapshot.documents.mapNotNull { doc ->
                    if (doc.id == currentUserId) return@mapNotNull null
                    val name = doc.getString("name") ?: ""
                    val email = doc.getString("email") ?: ""
                    // Tìm không phân biệt hoa/thường, chứa ở bất kỳ đâu
                    if (name.lowercase().contains(queryLower) || email.lowercase().contains(queryLower)) {
                        User(
                            id = doc.id,
                            name = name.ifEmpty { "Người dùng" },
                            email = email,
                            avatarUrl = doc.getString("avatarUrl") ?: "",
                            bio = doc.getString("bio") ?: ""
                        )
                    } else null
                }
                withContext(Dispatchers.Main) { adapter.updateList(users) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Tìm kiếm lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadChatRooms(
        pendingAdapter: ChatRoomAdapter,
        chatsAdapter: ChatRoomAdapter,
        txtPendingHeader: TextView,
        txtChatsHeader: TextView,
        emptyState: View,
        progressBar: ProgressBar
    ) {
        progressBar.visibility = View.VISIBLE
        emptyState.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Query chatRooms where current user is participant
                val snap1 = db.collection("chatRooms")
                    .whereEqualTo("user1Id", currentUserId)
                    .get().await()
                val snap2 = db.collection("chatRooms")
                    .whereEqualTo("user2Id", currentUserId)
                    .get().await()

                val allRooms = (snap1.toObjects(ChatRoom::class.java) + snap2.toObjects(ChatRoom::class.java))
                    .filter { it.status != ChatRoom.STATUS_REJECTED }
                    .filter { currentUserId !in it.deletedBy }
                    .sortedByDescending { it.lastMessageTime }

                val pending = allRooms.filter { it.isPendingForMe(currentUserId) }
                // Hiện cả ACCEPTED + PENDING do mình gửi (chờ đối phương chấp nhận)
                val chats = allRooms.filter {
                    it.status == ChatRoom.STATUS_ACCEPTED ||
                    (it.status == ChatRoom.STATUS_PENDING && it.initiatorId == currentUserId)
                }

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    pendingAdapter.updateList(pending)
                    txtPendingHeader.visibility = if (pending.isNotEmpty()) View.VISIBLE else View.GONE
                    (txtPendingHeader.parent as? View)?.findViewById<RecyclerView>(R.id.recyclerPending)?.visibility =
                        if (pending.isNotEmpty()) View.VISIBLE else View.GONE

                    chatsAdapter.updateList(chats)
                    txtChatsHeader.visibility = if (chats.isNotEmpty()) View.VISIBLE else View.GONE

                    emptyState.visibility = if (pending.isEmpty() && chats.isEmpty()) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    emptyState.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun showDeleteDialog(room: ChatRoom) {
        val otherName = room.getOtherUserName(currentUserId)
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Xóa đoạn chat")
            .setMessage("Xóa cuộc trò chuyện với $otherName?")
            .setPositiveButton("Xóa") { _, _ -> deleteChatRoom(room) }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteChatRoom(room: ChatRoom) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Thêm uid vào deletedBy — chỉ ẩn phía mình, đối phương vẫn thấy
                val updated = room.deletedBy.toMutableList().apply { add(currentUserId) }
                db.collection("chatRooms").document(room.chatRoomId)
                    .update("deletedBy", updated).await()

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Đã xóa đoạn chat", Toast.LENGTH_SHORT).show()
                    // Refresh list
                    onResume()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openChat(receiverId: String, receiverName: String, receiverAvatar: String) {
        val intent = Intent(requireContext(), ChatActivity::class.java).apply {
            putExtra("receiverId", receiverId)
            putExtra("receiverName", receiverName)
            putExtra("receiverAvatar", receiverAvatar)
        }
        startActivity(intent)
    }
}
