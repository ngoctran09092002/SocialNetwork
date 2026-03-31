package com.example.socialnetwork.media

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.socialn.core.interfaces.IMediaService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.io.File

/**
 *  — Delegate helper gắn vào bất kỳ Fragment nào.
 *
 * Cách dùng trong PostFragment:
 *
 *   private lateinit var mediaHelper: MediaHelper
 *
 *   override fun onCreate(savedInstanceState: Bundle?) {
 *       super.onCreate(savedInstanceState)
 *       val mediaService = MediaServiceImpl(requireContext())
 *       mediaHelper = MediaHelper(
 *           fragment      = this,
 *           mediaService  = mediaService,
 *           onSuccess     = { imageUrl -> viewModel.setImageUrl(imageUrl) },
 *           onError       = { e -> showError(e.message) }
 *       )
 *   }
 *
 *   // Gắn vào nút:
 *   btnPickImage.setOnClickListener { mediaHelper.openChooser() }
 *
 * QUAN TRỌNG: Phải khởi tạo MediaHelper trong onCreate() hoặc onAttach(),
 * KHÔNG khởi tạo trong onViewCreated() vì Activity Result launcher
 * phải đăng ký trước khi Fragment STARTED.
 */
class MediaHelper(
    private val fragment: Fragment,
    private val mediaService: IMediaService,
    private val onSuccess: (imageUrl: String) -> Unit,
    private val onError: (error: Exception) -> Unit = { e ->
        Toast.makeText(fragment.requireContext(), "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
    }
) {
    // URI tạm để lưu ảnh chụp từ camera (dùng cache dir qua FileProvider)
    private var pendingCameraUri: Uri? = null

    // Launcher cho Photo Picker (Android 13+, không cần xin quyền đọc storage)
    private val galleryLauncher: ActivityResultLauncher<PickVisualMediaRequest> =
        fragment.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { handleImageSelected(it) }
        }

    // Launcher chụp ảnh từ camera, nhận true/false (ảnh có được lưu không)
    private val cameraLauncher: ActivityResultLauncher<Uri> =
        fragment.registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                pendingCameraUri?.let { handleImageSelected(it) }
            }
        }

    // Launcher xin quyền CAMERA
    private val permissionLauncher: ActivityResultLauncher<String> =
        fragment.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                launchCamera()
            } else {
                Toast.makeText(fragment.requireContext(), "Cần cấp quyền Camera", Toast.LENGTH_SHORT).show()
            }
        }

    // Mở dialog cho user chọn: Camera hoặc Thư viện
    fun openChooser() {
        MaterialAlertDialogBuilder(fragment.requireContext())
            .setTitle("Chọn ảnh từ")
            .setItems(arrayOf("Camera", "Thư viện")) { _, which ->
                when (which) {
                    0 -> requestCameraPermissionAndOpen()
                    1 -> openGallery()
                }
            }
            .show()
    }

    // Mở thư viện ảnh (gọi trực tiếp nếu muốn bỏ qua dialog)
    fun openGallery() {
        galleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    // Mở camera (gọi trực tiếp nếu muốn bỏ qua dialog)
    fun openCamera() {
        requestCameraPermissionAndOpen()
    }

    // Kiểm tra permission trước khi mở camera
    private fun requestCameraPermissionAndOpen() {
        val ctx = fragment.requireContext()
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Tạo file tạm trong cache → lấy URI qua FileProvider → mở camera
    private fun launchCamera() {
        val ctx = fragment.requireContext()
        val tempFile = File(ctx.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", tempFile)
        pendingCameraUri = uri
        cameraLauncher.launch(uri)
    }

    // Sau khi có URI (từ gallery hoặc camera): nén + upload, rồi callback kết quả
    private fun handleImageSelected(uri: Uri) {
        fragment.lifecycleScope.launch {
            try {
                val imageUrl = mediaService.uploadImage(uri.toString())
                onSuccess(imageUrl)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
}
