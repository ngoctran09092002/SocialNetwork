package com.example.socialnetwork.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialnetwork.R
import com.example.socialnetwork.auth.LoginActivity
import com.example.socialnetwork.core.interfaces.IAuthService
import com.example.socialnetwork.core.interfaces.IUserRepository
import com.example.socialnetwork.core.models.Post
import com.example.socialnetwork.core.models.User
import com.example.socialnetwork.features.feed.repository.FeedRepository
import com.example.socialnetwork.feed.ui.CommentDialog
import com.example.socialnetwork.feed.ui.PostAdapter
import com.example.socialnetwork.firebase.FirebaseAuthService
import com.example.socialnetwork.firebase.FirebaseUserRepository
import com.example.socialnetwork.util.CloudinaryUploader
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val feedRepository = FeedRepository()
    private lateinit var recyclerPosts: RecyclerView
    private lateinit var postAdapter: PostAdapter

    private lateinit var imgAvatar: ImageView
    private lateinit var imgCover: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvPostCount: TextView
    private lateinit var tvFriendCount: TextView
    private lateinit var btnEditProfile: View
    private lateinit var btnBack: ImageButton
    private lateinit var btnLogout: ImageButton
    private lateinit var btnChangeCover: ImageButton

    private val userRepository: IUserRepository = FirebaseUserRepository()
    private val authService: IAuthService = FirebaseAuthService()

    private var userId: String? = null
    private var currentUserId: String? = null

    // Cover photo picker
    private val pickCover = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        Toast.makeText(requireContext(), "Đang tải ảnh bìa...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            try {
                val coverUrl = withContext(Dispatchers.IO) {
                    CloudinaryUploader.upload(requireContext(), uri)
                }
                // Lưu vào Firestore
                val uid = currentUserId ?: return@launch
                withContext(Dispatchers.IO) {
                    FirebaseFirestore.getInstance().collection("users").document(uid)
                        .update("coverUrl", coverUrl).await()
                }
                // Cập nhật UI
                Glide.with(requireContext()).load(coverUrl).centerCrop().into(imgCover)
                Toast.makeText(requireContext(), "Đã cập nhật ảnh bìa", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Upload cover failed: ${e.message}")
                Toast.makeText(requireContext(), "Lỗi tải ảnh bìa", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = arguments?.getString("userId")
        currentUserId = authService.getCurrentUserId()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imgAvatar = view.findViewById(R.id.imgAvatar)
        imgCover = view.findViewById(R.id.imgCover)
        tvName = view.findViewById(R.id.tvName)
        tvBio = view.findViewById(R.id.tvBio)
        tvPostCount = view.findViewById(R.id.tvPostCount)
        tvFriendCount = view.findViewById(R.id.tvFollowerCount)
        btnEditProfile = view.findViewById(R.id.btnEditProfile)
        btnBack = view.findViewById(R.id.btnBack)
        btnLogout = view.findViewById(R.id.btnLogout)
        btnChangeCover = view.findViewById(R.id.btnChangeCover)
        recyclerPosts = view.findViewById(R.id.recyclerPosts)

        val profileId = userId ?: currentUserId
        val isMyProfile = profileId == currentUserId

        btnEditProfile.visibility = if (isMyProfile) View.VISIBLE else View.GONE
        btnBack.visibility = if (isMyProfile) View.GONE else View.VISIBLE
        btnLogout.visibility = if (isMyProfile) View.VISIBLE else View.GONE
        btnChangeCover.visibility = if (isMyProfile) View.VISIBLE else View.GONE

        postAdapter = PostAdapter(
            posts = emptyList(),
            onLikeClick = { post ->
                handleLikePost(post)
            },
            onCommentClick = { post ->
                openCommentDialog(post)
            },
            onDeletePost = { post, position ->
                // Chỉ cho phép xóa nếu đang xem profile của chính mình
                if (isMyProfile) {
                    deletePostInProfile(post, position)
                }
            },
            currentUserId = currentUserId ?: ""
        )

        recyclerPosts.layoutManager = LinearLayoutManager(requireContext())
        recyclerPosts.adapter = postAdapter

        loadProfile(profileId)

        btnEditProfile.setOnClickListener { showEditDialog() }
        btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        btnChangeCover.setOnClickListener { pickCover.launch("image/*") }
        btnLogout.setOnClickListener { performLogout() }
    }

    private fun handleLikePost(post: Post) {
        lifecycleScope.launch {
            try {
                val userId = currentUserId ?: return@launch

                val db = FirebaseFirestore.getInstance()
                val likeDoc = db.collection("posts")
                    .document(post.id)
                    .collection("likes")
                    .document(userId)
                    .get()
                    .await()

                val isCurrentlyLiked = likeDoc.exists()
                feedRepository.likePost(post.id, userId)

                // Cập nhật UI
                val currentPosts = postAdapter.getPosts()
                val updatedPosts = currentPosts.map { p ->
                    if (p.id == post.id) {
                        val newLikeCount = if (isCurrentlyLiked) {
                            p.likesCount - 1
                        } else {
                            p.likesCount + 1
                        }
                        p.copy(likesCount = newLikeCount)
                    } else {
                        p
                    }
                }
                postAdapter.updatePosts(updatedPosts)
                postAdapter.updateLikeState(post.id, !isCurrentlyLiked)

            } catch (e: Exception) {
                Log.e("ProfileFragment", "Error liking post: ${e.message}", e)
                Toast.makeText(requireContext(), "Không thể cập nhật like", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openCommentDialog(post: Post) {
        val dialog = CommentDialog.newInstance(
            postId = post.id,
            postCaption = post.caption,
            postImageUrl = post.imageUrl,
            postAuthorId = post.authorId,
            currentUserId = currentUserId ?: "",
            onCommentCountChanged = { newCommentCount ->
                updatePostCommentCount(post.id, newCommentCount)
            }
        )
        dialog.show(parentFragmentManager, "CommentDialog")
    }

    private fun updatePostCommentCount(postId: String, newCount: Int) {
        val currentPosts = postAdapter.getPosts()
        val updatedPosts = currentPosts.map { post ->
            if (post.id == postId) {
                post.copy(commentCount = newCount)
            } else {
                post
            }
        }
        postAdapter.updatePosts(updatedPosts)
    }

    private fun deletePostInProfile(post: Post, position: Int) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Xóa bài viết")
            .setMessage("Bạn có chắc chắn muốn xóa bài viết này không?")
            .setPositiveButton("Xóa") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val success = feedRepository.deletePost(post.id)
                        if (success) {
                            // Xóa post khỏi danh sách hiện tại
                            val currentPosts = postAdapter.getPosts().toMutableList()
                            currentPosts.removeAll { it.id == post.id }
                            postAdapter.updatePosts(currentPosts)
                            // Cập nhật số lượng bài viết
                            tvPostCount.text = currentPosts.size.toString()
                            Toast.makeText(requireContext(), "Đã xóa bài viết", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Không thể xóa bài viết", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("ProfileFragment", "Error deleting post: ${e.message}", e)
                        Toast.makeText(requireContext(), "Lỗi khi xóa bài viết", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
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
            .error(R.drawable.profile)
            .circleCrop()
            .into(imgAvatar)

        // Load ảnh bìa
        if (user.coverUrl.isNotEmpty()) {
            Glide.with(requireContext())
                .load(user.coverUrl)
                .centerCrop()
                .into(imgCover)
        }
    }

    private fun performLogout() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Đăng xuất")
            .setMessage("Bạn có chắc muốn đăng xuất?")
            .setPositiveButton("Đăng xuất") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showEditDialog() {
        val dialog = EditProfileDialog().apply {
            arguments = Bundle().apply {
                putString("userId", userId ?: currentUserId)
            }
            listener = object : EditProfileDialog.OnProfileUpdatedListener {
                override fun onProfileUpdated(user: User) {
                    val postCount = tvPostCount.text.toString().toIntOrNull() ?: 0
                    val friendCount = tvFriendCount.text.toString().toIntOrNull() ?: 0
                    setProfile(user, postCount, friendCount)
                    postAdapter.clearUserCache()

                    lifecycleScope.launch {
                        val profileId = userId ?: currentUserId
                        val posts = feedRepository.getPostsByUser(profileId!!)
                        postAdapter.updatePosts(posts)
                    }
                }
            }
        }
        dialog.show(parentFragmentManager, "EditProfileDialog")
    }
}