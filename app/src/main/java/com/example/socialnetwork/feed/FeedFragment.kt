package com.example.socialnetwork.feed

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialnetwork.R
import com.example.socialnetwork.core.models.Post
import com.example.socialnetwork.features.feed.repository.FeedRepository
import com.example.socialnetwork.feed.ui.CommentDialog
import com.example.socialnetwork.feed.ui.FeedViewModel
import com.example.socialnetwork.feed.ui.FeedViewModelFactory
import com.example.socialnetwork.feed.ui.PostAdapter
import com.google.firebase.auth.FirebaseAuth

class FeedFragment : Fragment(R.layout.fragment_feed) {

    private lateinit var viewModel: FeedViewModel
    private lateinit var adapter: PostAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "testUser"

        recyclerView = view.findViewById(R.id.recyclerFeed)
        progressBar = view.findViewById(R.id.progressBar)

        val factory = FeedViewModelFactory(FeedRepository(), currentUserId)
        viewModel = ViewModelProvider(this, factory)[FeedViewModel::class.java]

        adapter = PostAdapter(
            posts = emptyList(),
            onLikeClick = { post -> viewModel.likePost(post.id) },
            onCommentClick = { post -> openCommentDialog(post, currentUserId) },
            currentUserId = currentUserId
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            adapter.updatePosts(posts)
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
        viewModel.likeStateChanged.observe(viewLifecycleOwner) { (postId, isLiked) ->
            adapter.updateLikeState(postId, isLiked)
        }

        viewModel.loadPosts()
    }

    private fun openCommentDialog(post: Post, currentUserId: String) {
        val dialog = CommentDialog.newInstance(
            postId = post.id,
            postCaption = post.caption,
            postImageUrl = post.imageUrl,
            postAuthorId = post.authorId,
            currentUserId = currentUserId,
            onCommentCountChanged = { newCount ->
                adapter.updatePostCommentCount(post.id, newCount)
            }
        )
        dialog.show(parentFragmentManager, "CommentDialog")
    }
}
