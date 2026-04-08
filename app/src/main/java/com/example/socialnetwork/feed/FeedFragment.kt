package com.example.socialnetwork.feed

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialnetwork.R
import com.example.socialnetwork.core.models.Post
import com.example.socialnetwork.features.feed.repository.FeedRepository
import com.example.socialnetwork.feed.ui.*
import com.google.firebase.auth.FirebaseAuth

class FeedFragment : Fragment(R.layout.fragment_feed) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: PostAdapter
    private lateinit var viewModel: FeedViewModel
    private lateinit var currentUserId: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Lấy currentUserId từ Firebase Auth
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "testUser"

        initViews(view)
        setupViewModel()
        setupRecyclerView()
        observeViewModel()

        viewModel.loadPosts()
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerFeed)
        progressBar = view.findViewById(R.id.progressBar)
    }

    private fun setupViewModel() {
        val repository = FeedRepository()
        val factory = FeedViewModelFactory(repository, currentUserId)
        viewModel = ViewModelProvider(this, factory)[FeedViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = PostAdapter(
            posts = emptyList(),
            onLikeClick = { post ->
                viewModel.likePost(post.id)
            },
            onCommentClick = { post ->
                openCommentDialog(post)
            },
            currentUserId = currentUserId
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun openCommentDialog(post: Post) {
        val dialog = CommentDialog.newInstance(
            postId = post.id,
            postCaption = post.caption,
            postImageUrl = post.imageUrl,
            postAuthorId = post.authorId,
            currentUserId = currentUserId,
            onCommentCountChanged = { newCommentCount ->
                updatePostCommentCount(post.id, newCommentCount)
            }
        )
        dialog.show(parentFragmentManager, "CommentDialog")
    }

    private fun updatePostCommentCount(postId: String, newCount: Int) {
        val currentPosts = adapter.getPosts()
        val updatedPosts = currentPosts.map { post ->
            if (post.id == postId) {
                post.copy(commentCount = newCount)
            } else {
                post
            }
        }
        adapter.updatePosts(updatedPosts)
    }

    private fun observeViewModel() {
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            adapter.updatePosts(posts)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.likeStateChanged.observe(viewLifecycleOwner) { (postId, isLiked) ->
            adapter.updateLikeState(postId, isLiked)
        }
    }
}