package com.example.socialnetwork.ui

import android.os.Bundle
import android.view.*
import android.widget.*
import com.bumptech.glide.Glide
import com.example.socialnetwork.*
import com.example.socialnetwork.core.models.User
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class UserBottomSheet(private val user: User) : BottomSheetDialogFragment() {
    companion object {
        private const val KEY_USER_ID = "userId"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.bottom_sheet_user, container, false)

        val avatar = view.findViewById<ImageView>(R.id.imgAvatar)
        val name = view.findViewById<TextView>(R.id.txtName)
        val bio = view.findViewById<TextView>(R.id.txtBio)
        val btnProfile = view.findViewById<Button>(R.id.btnProfile)
        val btnChat = view.findViewById<Button>(R.id.btnChat)
        val btnCancel = view.findViewById<TextView>(R.id.btnCancel)

        bindUserData(avatar, name, bio)
        setupActions(btnProfile, btnChat, btnCancel)

        return view
    }

    private fun bindUserData(avatar: ImageView, name: TextView, bio: TextView) {
        name.text = user.name
        bio.text = user.bio

        Glide.with(requireContext())
            .load(user.avatarUrl)
            .placeholder(R.drawable.profile)
            .into(avatar)
    }

    private fun setupActions(btnProfile: Button, btnChat: Button, btnCancel: TextView) {
        btnProfile.setOnClickListener {
            navigate(ProfileFragment())
            dismiss()
        }
        btnChat.setOnClickListener {
            // ================= NOTE =================
            // Node 5 chỉ gửi user.id để Node 4 handle ChatBox + Firebase logic
            // Node 3 handle Media (Camera/Gallery, nén, upload)
            // Tôi có viết func navigate cho tái xử dụng
            Toast.makeText(requireContext(), "Lấy id user để getIn4 cho UI", Toast.LENGTH_SHORT).show()
            dismiss()
        }
        btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun navigate(fragment: androidx.fragment.app.Fragment) {
        fragment.arguments = Bundle().apply {
            putString(KEY_USER_ID, user.id)
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}