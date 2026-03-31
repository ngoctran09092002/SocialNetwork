package com.example.socialnetwork.media

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.socialnetwork.R

/**
 * Fragment test tạm thời — xác nhận toàn bộ flow Media hoạt động:
 * Chọn ảnh → Nén → Upload Firebase Storage → Nhận URL
 *
 * Xóa file này sau khi demo xong.
 */
class MediaTestFragment : Fragment() {

    // Khởi tạo MediaHelper trong onCreate (BẮT BUỘC, không được để trong onViewCreated)
    private lateinit var mediaHelper: MediaHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mediaService = MediaServiceImpl(requireContext())

        mediaHelper = MediaHelper(
            fragment     = this,
            mediaService = mediaService,
            onSuccess    = { imageUrl -> onUploadSuccess(imageUrl) },
            onError      = { e -> onUploadError(e) }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_media_test, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btnPickImage).setOnClickListener {
            view.findViewById<TextView>(R.id.tvStatus).text = "Đang upload..."
            mediaHelper.openChooser()
        }
    }

    private fun onUploadSuccess(imageUrl: String) {
        view?.let { v ->
            // Hiển thị URL (Node 2 sẽ dùng URL này để lưu vào Post)
            v.findViewById<TextView>(R.id.tvStatus).text = "✓ Upload thành công!\n$imageUrl"

            // Load ảnh từ URL để xác nhận link hợp lệ (dùng Uri đơn giản, không cần Glide)
            v.findViewById<ImageView>(R.id.ivPreview).setImageURI(Uri.parse(imageUrl))
        }
    }

    private fun onUploadError(e: Exception) {
        view?.findViewById<TextView>(R.id.tvStatus)?.text = "✗ Lỗi: ${e.message}"
    }
}
