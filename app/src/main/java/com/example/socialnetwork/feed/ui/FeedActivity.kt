package com.example.socialnetwork.feed.ui

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialnetwork.R
import com.example.socialnetwork.core.models.Post
import com.example.socialnetwork.features.feed.repository.FeedRepository

class FeedActivity() : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: PostAdapter
    private lateinit var viewModel: FeedViewModel
    private lateinit var currentUserId: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feed)
        currentUserId = intent.getStringExtra("currentUserId") ?: ""

        initViews()
        setupViewModel()
        setupRecyclerView()
        observeViewModel()

        viewModel.loadPosts()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerFeed)
        progressBar = findViewById(R.id.progressBar)
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
        recyclerView.layoutManager = LinearLayoutManager(this)
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
        dialog.show(supportFragmentManager, "CommentDialog")
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
        viewModel.posts.observe(this) { posts ->
            adapter.updatePosts(posts)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
        viewModel.likeStateChanged.observe(this) { (postId, isLiked) ->
            adapter.updateLikeState(postId, isLiked)
        }
    }
}