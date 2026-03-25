package com.example.socialnetwork.ui

import android.os.Bundle
import android.view.*
import android.widget.*
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

        val resId = resources.getIdentifier(
            user.avatarUrl,
            "drawable",
            requireContext().packageName
        )
        avatar.setImageResource(if (resId != 0) resId else R.drawable.profile)
    }

    private fun setupActions(btnProfile: Button, btnChat: Button, btnCancel: TextView) {
        btnProfile.setOnClickListener {
            navigate(ProfileFragment())
        }
        btnChat.setOnClickListener {
            navigate(ChatBox())
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
            .replace(R.id.nav_host_fragment, fragment)
            .addToBackStack(null)
            .commit()
        dismiss()
    }
}