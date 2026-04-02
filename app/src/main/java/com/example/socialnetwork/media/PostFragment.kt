package com.example.socialnetwork.feed

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.socialnetwork.R
import com.example.socialnetwork.core.models.Post
import com.example.socialnetwork.features.feed.repository.FeedRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class PostFragment : Fragment(R.layout.fragment_post) {

    private var selectedImageUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            view?.findViewById<ImageView>(R.id.imgPreview)?.setImageURI(it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imgPreview = view.findViewById<ImageView>(R.id.imgPreview)
        val btnPickImage = view.findViewById<Button>(R.id.btnPickImage)
        val edtCaption = view.findViewById<EditText>(R.id.edtCaption)
        val btnPost = view.findViewById<Button>(R.id.btnPost)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        btnPickImage.setOnClickListener {
            pickImage.launch("image/*")
        }

        btnPost.setOnClickListener {
            val caption = edtCaption.text.toString().trim()
            if (caption.isEmpty() && selectedImageUri == null) {
                Toast.makeText(requireContext(), "Vui lòng nhập nội dung hoặc chọn ảnh", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserId == null) {
                Toast.makeText(requireContext(), "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnPost.isEnabled = false
            progressBar.visibility = View.VISIBLE

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val imageUrl = selectedImageUri?.let { uri ->
                        val storageRef = FirebaseStorage.getInstance()
                            .reference
                            .child("posts/${UUID.randomUUID()}.jpg")
                        storageRef.putFile(uri).await()
                        storageRef.downloadUrl.await().toString()
                    } ?: ""

                    val post = Post(
                        id = UUID.randomUUID().toString(),
                        authorId = currentUserId,
                        imageUrl = imageUrl,
                        caption = caption,
                        timestamp = System.currentTimeMillis()
                    )

                    FeedRepository().createPost(post)

                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        btnPost.isEnabled = true
                        edtCaption.text.clear()
                        selectedImageUri = null
                        imgPreview.setImageResource(R.drawable.ic_launcher_background)
                        Toast.makeText(requireContext(), "Đăng bài thành công!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        btnPost.isEnabled = true
                        Toast.makeText(requireContext(), "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
