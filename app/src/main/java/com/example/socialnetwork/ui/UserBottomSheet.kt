package com.example.socialnetwork.ui

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import com.bumptech.glide.Glide
import com.example.socialnetwork.R
import com.example.socialnetwork.core.models.User
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class UserBottomSheet : BottomSheetDialogFragment() {

    private var user: User? = null
    fun setUser(u: User) { user = u}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.bottom_sheet_user, container, false)

        val avatar = view.findViewById<ImageView>(R.id.imgAvatar)
        val name = view.findViewById<TextView>(R.id.txtName)
        val bio = view.findViewById<TextView>(R.id.txtBio)

        val btnProfile = view.findViewById<TextView>(R.id.btnProfile)
        val btnChat = view.findViewById<TextView>(R.id.btnChat)
        val btnCancel = view.findViewById<TextView>(R.id.btnCancel)

        val u = user
        if (u == null) {
            dismiss()
            return view
        }

        name.text = u.name
        bio.text = u.bio ?: ""

        Glide.with(requireContext())
            .load(u.avatarUrl ?: R.drawable.profile)
            .placeholder(R.drawable.profile)
            .circleCrop()
            .into(avatar)

        // Profile
        btnProfile.setOnClickListener {
            val fragment = ProfileFragment().apply {
                arguments = Bundle().apply {
                    putString("userId", u.id)
                }
            }

            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.fragment_container, fragment)
                ?.addToBackStack(null)
                ?.commit()

            dismiss()
        }

        // Chat
        btnChat.setOnClickListener {
            context?.let { ctx ->
                val intent = Intent(ctx, ChatActivity::class.java).apply {
                    putExtra("receiverId", u.id)
                    putExtra("receiverName", u.name)
                    putExtra("receiverAvatar", u.avatarUrl ?: "")
                }
                startActivity(intent)
            }
            dismiss()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        return view
    }

    override fun onStart() {
        super.onStart()

        val dialog = dialog as? com.google.android.material.bottomsheet.BottomSheetDialog
        val bottomSheet = dialog?.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        )

        bottomSheet?.layoutParams?.height = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}