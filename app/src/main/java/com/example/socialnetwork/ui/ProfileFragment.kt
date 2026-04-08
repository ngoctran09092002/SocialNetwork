package com.example.socialnetwork.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialnetwork.R
import com.example.socialnetwork.core.interfaces.IAuthService
import com.example.socialnetwork.core.interfaces.IUserRepository
import com.example.socialnetwork.core.models.User
import com.example.socialnetwork.features.feed.repository.FeedRepository
import com.example.socialnetwork.feed.ui.PostAdapter
import com.example.socialnetwork.firebase.FirebaseAuthService
import com.example.socialnetwork.firebase.FirebaseUserRepository
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val feedRepository = FeedRepository()
    private lateinit var recyclerPosts: RecyclerView
    private lateinit var postAdapter: PostAdapter

    private lateinit var imgAvatar: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvPostCount: TextView
    private lateinit var tvFriendCount: TextView
    private lateinit var btnEditProfile: View
    private lateinit var btnBack: ImageButton

    private val userRepository: IUserRepository = FirebaseUserRepository()
    private val authService: IAuthService = FirebaseAuthService()

    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = arguments?.getString("userId")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imgAvatar = view.findViewById(R.id.imgAvatar)
        tvName = view.findViewById(R.id.tvName)
        tvBio = view.findViewById(R.id.tvBio)
        tvPostCount = view.findViewById(R.id.tvPostCount)
        tvFriendCount = view.findViewById(R.id.tvFollowerCount)
        btnEditProfile = view.findViewById(R.id.btnEditProfile)
        btnBack = view.findViewById(R.id.btnBack)
        recyclerPosts = view.findViewById(R.id.recyclerPosts)

        val currentUserId = authService.getCurrentUserId()
        val profileId = userId ?: currentUserId
        val isMyProfile = profileId == currentUserId

        btnEditProfile.visibility = if (isMyProfile) View.VISIBLE else View.GONE
        btnBack.visibility = if (isMyProfile) View.GONE else View.VISIBLE

        postAdapter = PostAdapter(
            posts = emptyList(),
            onLikeClick = { /* handle like if needed */ },
            onCommentClick = { /* handle comment if needed */ },
            currentUserId = currentUserId ?: ""
        )

        recyclerPosts.layoutManager = LinearLayoutManager(requireContext())
        recyclerPosts.adapter = postAdapter

        loadProfile(profileId)

        btnEditProfile.setOnClickListener { showEditDialog() }
        btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
    }

    private fun loadProfile(profileId: String?) {
        if (profileId == null) return

        lifecycleScope.launch {
            try {
                val user = userRepository.getUserProfile(profileId)
                val postCount = userRepository.getUserPostCount(profileId)
                val friendCount = userRepository.getFriendCount(profileId)

                user?.let { setProfile(it, postCount, friendCount) }

                val posts = feedRepository.getPostsByUser(profileId)
                postAdapter.updatePosts(posts)

            } catch (e: Exception) {
                Log.e("ProfileFragment", "Error loading profile: ${e.message}", e)
                Toast.makeText(requireContext(), "Không thể tải profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setProfile(user: User, postCount: Int, friendCount: Int) {
        tvName.text = user.name
        tvBio.text = user.bio.ifBlank { "Chưa có tiểu sử" }
        tvPostCount.text = postCount.toString()
        tvFriendCount.text = friendCount.toString()

        Glide.with(requireContext())
            .load(user.avatarUrl)
            .placeholder(R.drawable.profile)
            .into(imgAvatar)
    }

    private fun showEditDialog() {
        val dialog = EditProfileDialog().apply {
            arguments = Bundle().apply {
                putString("userId", userId ?: authService.getCurrentUserId())
            }
            listener = object : EditProfileDialog.OnProfileUpdatedListener {
                override fun onProfileUpdated(user: User) {
                    val postCount = tvPostCount.text.toString().toIntOrNull() ?: 0
                    val friendCount = tvFriendCount.text.toString().toIntOrNull() ?: 0
                    setProfile(user, postCount, friendCount)
                    postAdapter.clearUserCache()
                }
            }
        }
        dialog.show(parentFragmentManager, "EditProfileDialog")
    }
}