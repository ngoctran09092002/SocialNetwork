package com.example.socialnetwork.feed

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.socialnetwork.R
import com.example.socialnetwork.util.CloudinaryUploader
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
            view?.findViewById<ImageView>(R.id.imgPreview)?.apply {
                setImageURI(it)
                visibility = View.VISIBLE
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imgPreview   = view.findViewById<ImageView>(R.id.imgPreview)
        val btnPickImage = view.findViewById<View>(R.id.btnPickImage)
        val edtCaption   = view.findViewById<EditText>(R.id.edtCaption)
        val btnPost      = view.findViewById<View>(R.id.btnPost)
        val progressBar  = view.findViewById<ProgressBar>(R.id.progressBar)

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
                    // Upload ảnh lên Cloudinary (nếu có chọn ảnh)
                    val imageUrl = selectedImageUri?.let { uri ->
                        try {
                            CloudinaryUploader.upload(requireContext(), uri)
                        } catch (ex: Exception) {
                            Log.e("PostFragment", "Upload ảnh thất bại: ${ex.message}")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    requireContext(),
                                    "Không tải được ảnh. Bài đăng không có ảnh.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            ""
                        }
                    } ?: ""

                    // Lưu bài viết vào Firestore
                    val postId = UUID.randomUUID().toString()
                    val postData = hashMapOf(
                        "id"           to postId,
                        "authorId"     to currentUserId,
                        "imageUrl"     to imageUrl,
                        "caption"      to caption,
                        "timestamp"    to System.currentTimeMillis(),
                        "likesCount"   to 0,
                        "commentCount" to 0
                    )
                    FirebaseFirestore.getInstance()
                        .collection("posts")
                        .document(postId)
                        .set(postData)
                        .await()

                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        btnPost.isEnabled = true
                        edtCaption.text.clear()
                        selectedImageUri = null
                        imgPreview.visibility = View.GONE
                        Toast.makeText(requireContext(), "Đăng bài thành công!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("PostFragment", "Đăng bài thất bại: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        btnPost.isEnabled = true
                        Toast.makeText(requireContext(), "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
