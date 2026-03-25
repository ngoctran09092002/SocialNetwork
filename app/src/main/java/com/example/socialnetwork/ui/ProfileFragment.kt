package com.example.socialnetwork.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.socialn.core.interfaces.IAuthService
import com.example.socialn.core.interfaces.IUserRepository
import com.example.socialnetwork.R
import com.example.socialnetwork.core.models.User
import com.example.socialnetwork.mock.MockAuthService
import com.example.socialnetwork.mock.MockUserRepository
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(R.layout.profile) {

    private lateinit var avt: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvBio: TextView
    private lateinit var btnAdd: View
    private lateinit var btnMessage: View
    private lateinit var btnBack: View
    private lateinit var btnEdit: View

    private val userRepository: IUserRepository = MockUserRepository
    private val authService: IAuthService = MockAuthService()
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = arguments?.getString("userId")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        avt = view.findViewById(R.id.imgAvatar)
        tvName = view.findViewById(R.id.tvName)
        tvBio = view.findViewById(R.id.tvBio)
        btnAdd = view.findViewById(R.id.btnAddFriend)
        btnMessage = view.findViewById(R.id.btnMessage)
        btnBack = view.findViewById(R.id.btnBack)
        btnEdit = view.findViewById(R.id.btnEdit)

        val currentUserId = authService.getCurrentUserId()
        val profileId = userId ?: currentUserId
        val isMyProfile = profileId == currentUserId

        if (isMyProfile) {
            btnAdd.visibility = View.GONE
            btnMessage.visibility = View.GONE
            btnBack.visibility = View.GONE
            btnEdit.visibility = View.VISIBLE
        } else {
            btnAdd.visibility = View.VISIBLE
            btnMessage.visibility = View.VISIBLE
            btnBack.visibility = View.VISIBLE
            btnEdit.visibility = View.GONE
        }

        lifecycleScope.launch {
            if (profileId == null) return@launch
            val user = userRepository.getUserProfile(profileId)
            user?.let { setProfile(it) }
        }

        btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        btnMessage.setOnClickListener {
            profileId?.let { userId ->
                // ================= NOTE =================
                // Node 5 hiện tại chỉ gửi userId để Node 4 handle chat
                // Node 4 sẽ implement ChatBox / Firebase Realtime DB
                // Node 3 sẽ handle Media upload, nén, trả URL nếu cần
                Toast.makeText(requireContext(), "Lấy id user để getIn4 cho UI", Toast.LENGTH_SHORT).show()
            }
        }

        btnEdit.setOnClickListener { showEditDialog() }
    }

    private fun setProfile(user: User) {
        tvName.text = user.name
        tvBio.text = user.bio

        if (user.avatarUrl.startsWith("content://")) {
            avt.setImageURI(android.net.Uri.parse(user.avatarUrl))
        } else {
            val resId = resources.getIdentifier(
                user.avatarUrl,
                "drawable",
                requireContext().packageName
            )
            avt.setImageResource(if (resId != 0) resId else R.drawable.profile)
        }
    }

    private fun showEditDialog() {
        val dialog = EditProfileDialog().apply {
            arguments = Bundle().apply {
                putString("userId", userId ?: authService.getCurrentUserId())
            }
            listener = object : EditProfileDialog.OnProfileUpdatedListener {
                override fun onProfileUpdated(user: User) {
                    setProfile(user)
                }
            }
        }
        dialog.show(parentFragmentManager, "EditProfileDialog")
    }
}