package com.example.socialnetwork.ui

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.*
import android.widget.*
import com.google.android.material.textfield.TextInputEditText
import com.example.socialnetwork.R
import androidx.lifecycle.lifecycleScope
import com.example.socialn.core.interfaces.IUserRepository
import com.example.socialnetwork.mock.MockUserRepository
import kotlinx.coroutines.launch

class ChatBox : Fragment() {
    private lateinit var imgAvatar: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvStatus: TextView

    private lateinit var rvMessages: RecyclerView
    private lateinit var edtMessage: TextInputEditText
    private lateinit var btnSend: ImageButton

    private val messages = mutableListOf<String>()
    private val userRepository: IUserRepository = MockUserRepository
    private lateinit var adapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.view_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        imgAvatar = view.findViewById(R.id.imgAvatar)
        tvName = view.findViewById(R.id.tvName)
        tvStatus = view.findViewById(R.id.tvStatus)
        rvMessages = view.findViewById(R.id.rvMessages)
        edtMessage = view.findViewById(R.id.edtMessage)
        btnSend = view.findViewById(R.id.btnSend)
        val btnBack = view.findViewById<View>(R.id.btnBack)

        adapter = ChatAdapter(messages)
        rvMessages.layoutManager = LinearLayoutManager(requireContext())
        rvMessages.adapter = adapter

        btnSend.setOnClickListener {
            val text = edtMessage.text.toString()
            if (text.isNotBlank()) {
                sendMessage(text)
                edtMessage.text?.clear()
            }
        }

        btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        val userId = arguments?.getString("userId")
        if (userId != null) {
            lifecycleScope.launch {
                val user = userRepository.getUserProfile(userId)
                user?.let {
                    tvName.text = it.name
                    tvStatus.text = "Active now"
                    val resId = resources.getIdentifier(it.avatarUrl, "drawable", requireContext().packageName)
                    imgAvatar.setImageResource(if (resId != 0) resId else R.drawable.profile)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().findViewById<View>(R.id.bottomNav)?.visibility = View.GONE
    }
    override fun onStop() {
        super.onStop()
        requireActivity().findViewById<View>(R.id.bottomNav)?.visibility = View.VISIBLE
    }
    private fun sendMessage(message: String) {
        messages.add(message)
        adapter.notifyItemInserted(messages.size - 1)
        rvMessages.scrollToPosition(messages.size - 1)
    }
}