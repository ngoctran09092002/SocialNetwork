package com.example.socialnetwork.feed.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.coroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialnetwork.R
import com.example.socialnetwork.core.models.Comment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CommentDialog : BottomSheetDialogFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: CommentAdapter
    private lateinit var viewModel: CommentViewModel
    private lateinit var tvTitle: TextView
    private lateinit var btnClose: ImageButton
    private lateinit var edtComment: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var imgPostPreview: ImageView
    private lateinit var tvPostCaption: TextView

    private var postId: String = ""
    private var postCaption: String = ""
    private var postImageUrl: String = ""
    private var postAuthorId: String = ""
    private var currentUserId: String = ""
    private var onCommentCountChanged: ((Int) -> Unit)? = null
    private val comments = mutableListOf<Comment>()

    companion object {
        fun newInstance(
            postId: String,
            postCaption: String,
            postImageUrl: String,
            postAuthorId: String,
            currentUserId: String,
            onCommentCountChanged: ((Int) -> Unit)? = null
        ): CommentDialog {
            val fragment = CommentDialog()
            val args = Bundle()
            args.putString("postId", postId)
            args.putString("postCaption", postCaption)
            args.putString("postImageUrl", postImageUrl)
            args.putString("postAuthorId", postAuthorId)
            args.putString("currentUserId", currentUserId)
            fragment.arguments = args
            fragment.onCommentCountChanged = onCommentCountChanged
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme)
        arguments?.let {
            postId = it.getString("postId") ?: ""
            postCaption = it.getString("postCaption") ?: ""
            postImageUrl = it.getString("postImageUrl") ?: ""
            postAuthorId = it.getString("postAuthorId") ?: ""
            currentUserId = it.getString("currentUserId") ?: ""
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_comment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupViewModel()
        setupRecyclerView()
        setupPostPreview()
        setupCommentInput()
        observeViewModel()

        if (postId.isNotEmpty()) {
            viewModel.loadComments(postId)
        }
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerComments)
        progressBar = view.findViewById(R.id.progressBar)
        tvTitle = view.findViewById(R.id.tvTitle)
        btnClose = view.findViewById(R.id.btnClose)
        edtComment = view.findViewById(R.id.edtComment)
        btnSend = view.findViewById(R.id.btnSend)
        imgPostPreview = view.findViewById(R.id.imgPostPreview)
        tvPostCaption = view.findViewById(R.id.tvPostCaption)

        btnClose.setOnClickListener {
            dismiss()
        }

        tvTitle.text = "Comments"

        // Set initial send button color
        btnSend.isEnabled = false
        btnSend.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gray))
    }

    private fun setupPostPreview() {
        tvPostCaption.text = postCaption
        Glide.with(requireContext())
            .load(postImageUrl)
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_background)
            .into(imgPostPreview)
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[CommentViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = CommentAdapter(
            comments = comments,
            currentUserId = currentUserId,
            postAuthorId = postAuthorId,
            onDeleteClick = { comment ->
                showDeleteConfirmation(comment)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun showDeleteConfirmation(comment: Comment) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận xóa")
            .setMessage("Chắc chắn muốn xóa?")
            .setPositiveButton("Xóa") { _, _ ->
                val commentId = comment.id
                if (commentId.isNotEmpty()) {
                    viewModel.deleteComment(
                        postId = postId,
                        commentId = commentId,
                        currentUserId = currentUserId,
                        commentAuthorId = comment.userId,
                        postAuthorId = postAuthorId
                    )
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun setupCommentInput() {
        edtComment.doOnTextChanged { text, _, _, _ ->
            val hasText = !text.isNullOrBlank()
            btnSend.isEnabled = hasText
            if (hasText) {
                btnSend.setColorFilter(ContextCompat.getColor(requireContext(), R.color.blue))
            } else {
                btnSend.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gray))
            }
        }

        btnSend.setOnClickListener {
            val text = edtComment.text.toString()
            if (text.isNotBlank()) {
                lifecycle.coroutineScope.launch {
                    val (username, avatarUrl) = getUserInfo()
                    viewModel.addComment(postId, text, currentUserId, username, avatarUrl)
                    edtComment.text.clear()
                }
            }
        }
    }

    private suspend fun getUserInfo(): Pair<String, String> {
        return try {
            val db = FirebaseFirestore.getInstance()
            val userDoc = db.collection("users").document(currentUserId).get().await()
            val username = userDoc.getString("name") ?: "User"
            val avatarUrl = userDoc.getString("avatarUrl") ?: ""
            Pair(username, avatarUrl)
        } catch (e: Exception) {
            Pair("User", "")
        }
    }

    private fun observeViewModel() {
        viewModel.comments.observe(viewLifecycleOwner) { newComments ->
            comments.clear()
            comments.addAll(newComments)
            adapter.notifyDataSetChanged()

            // Update comment count in title
            val count = newComments.size
            tvTitle.text = "Bình luận ($count)"

            // Notify parent about comment count change
            onCommentCountChanged?.invoke(count)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }
}