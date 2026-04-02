package com.example.socialnetwork.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialnetwork.R
import com.example.socialnetwork.adapter.UserAdapter
import com.example.socialnetwork.core.interfaces.IAuthService
import com.example.socialnetwork.core.interfaces.IUserRepository
import com.example.socialnetwork.core.models.User
import com.example.socialnetwork.firebase.FirebaseAuthService
import com.example.socialnetwork.firebase.FirebaseUserRepository
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private val userRepository: IUserRepository = FirebaseUserRepository()
    private val authService: IAuthService = FirebaseAuthService()

    private lateinit var recyclerView: RecyclerView
    private lateinit var edtSearch: TextInputEditText
    private lateinit var adapter: UserAdapter

    private var allUsers: List<User> = emptyList()
    private var currentUserId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.search_friend, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.recyclerView)
        edtSearch = view.findViewById(R.id.edtSearch)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = UserAdapter { user -> showBottomSheet(user) }
        recyclerView.adapter = adapter

        currentUserId = authService.getCurrentUserId()

        lifecycleScope.launch {
            allUsers = (userRepository as FirebaseUserRepository).getAllUsers()
                .filter { it.id != currentUserId }
//            adapter.updateList(allUsers)   // mới mở lên là thấy all user
        }

        edtSearch.addTextChangedListener {
            val query = it.toString().trim().lowercase()
            val filtered = if (query.isEmpty()) {
                emptyList()
            } else {
                allUsers.filter { user ->
                    user.name.lowercase().contains(query)
                }
            }
            adapter.updateList(filtered)
        }
    }

    private fun showBottomSheet(user: User) {
        val sheet = UserBottomSheet(user)
        sheet.show(parentFragmentManager, "UserBottomSheet")
    }
}