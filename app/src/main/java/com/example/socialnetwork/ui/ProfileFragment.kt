package com.example.socialnetwork.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.socialnetwork.R
import com.example.socialn.core.interfaces.IAuthService
import com.example.socialn.core.interfaces.IUserRepository
import com.example.socialnetwork.core.models.User
import com.example.socialnetwork.firebase.FirebaseAuthService
import com.example.socialnetwork.firebase.FirebaseUserRepository
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(R.layout.profile) {

    private lateinit var avt: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvBio: TextView
    private lateinit var btnMessage: View
    private lateinit var btnBack: View
    private lateinit var btnEdit: View

    private val userRepository: IUserRepository = FirebaseUserRepository()
    private val authService: IAuthService = FirebaseAuthService()
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
        btnMessage = view.findViewById(R.id.btnMessage)
        btnBack = view.findViewById(R.id.btnBack)
        btnEdit = view.findViewById(R.id.btnEdit)

        val currentUserId = authService.getCurrentUserId()
        val profileId = userId ?: currentUserId
        val isMyProfile = profileId == currentUserId

        if (isMyProfile) {
            btnMessage.visibility = View.GONE
            btnBack.visibility = View.GONE
            btnEdit.visibility = View.VISIBLE
        } else {
            btnMessage.visibility = View.VISIBLE
            btnBack.visibility = View.VISIBLE
            btnEdit.visibility = View.GONE
        }

        loadProfile(profileId)

        btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        btnMessage.setOnClickListener {
            profileId?.let {
                Toast.makeText(requireContext(), "Lấy id user để getIn4 cho UI", Toast.LENGTH_SHORT).show()
            }
        }

        btnEdit.setOnClickListener { showEditDialog() }
    }

    private fun loadProfile(profileId: String?) {
        if (profileId == null) return
        lifecycleScope.launch {
            val user = userRepository.getUserProfile(profileId)
            user?.let { setProfile(it) }
        }
    }

    private fun setProfile(user: User) {
        tvName.text = user.name
        tvBio.text = user.bio

        // Glide tự quản lý cache, tránh xung đột với ảnh chọn từ library
        Glide.with(requireContext())
            .load(user.avatarUrl)
            .placeholder(R.drawable.profile)
            .into(avt)
    }

    private fun showEditDialog() {
        val dialog = EditProfileDialog().apply {
            arguments = Bundle().apply {
                putString("userId", userId ?: authService.getCurrentUserId())
            }
            listener = object : EditProfileDialog.OnProfileUpdatedListener {
                override fun onProfileUpdated(user: User) {
                    setProfile(user) // update ngay khi chỉnh sửa xong
                }
            }
        }
        dialog.show(parentFragmentManager, "EditProfileDialog")
    }
}