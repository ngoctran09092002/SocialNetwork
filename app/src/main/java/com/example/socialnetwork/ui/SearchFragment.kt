package com.example.socialnetwork.ui

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.*
import com.example.socialn.core.interfaces.IUserRepository
import com.example.socialnetwork.R
import com.example.socialnetwork.core.models.User
import com.example.socialnetwork.mock.MockUserRepository
import com.google.android.material.textfield.TextInputEditText

import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.example.socialnetwork.adapter.UserAdapter
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private val userRepository: IUserRepository = MockUserRepository

    private lateinit var recyclerView: RecyclerView
    private lateinit var edtSearch: TextInputEditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.search_friend, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.recyclerView)
        edtSearch = view.findViewById(R.id.edtSearch)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        edtSearch.addTextChangedListener {
            val query = it.toString()

            if (query.isBlank()) {
                recyclerView.adapter = null
                return@addTextChangedListener
            }

            lifecycleScope.launch {
                val users = userRepository.searchUsers(query)
                recyclerView.adapter = UserAdapter(users) { user ->
                    showBottomSheet(user)
                }
            }
        }
    }
    private fun showBottomSheet(user: User) {
        val sheet = UserBottomSheet(user)
        sheet.show(parentFragmentManager, "UserBottomSheet")
    }
}