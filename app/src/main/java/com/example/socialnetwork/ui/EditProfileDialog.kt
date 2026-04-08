package com.example.socialnetwork.ui

import android.app.Dialog
import android.app.ProgressDialog
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.socialnetwork.core.interfaces.IUserRepository
import com.example.socialnetwork.R
import com.example.socialnetwork.core.models.User
import com.example.socialnetwork.firebase.FirebaseUserRepository
import com.example.socialnetwork.services.CloudinaryService
import kotlinx.coroutines.launch
import com.bumptech.glide.Glide

class EditProfileDialog : DialogFragment() {

    private var userId: String? = null
    private val userRepository: IUserRepository = FirebaseUserRepository()

    private val cloudName = "dn1r6do9q"
    private val uploadPreset = "social_network_uploads"
    private lateinit var cloudinaryService: CloudinaryService

    private lateinit var avt: ImageView
    private lateinit var edtName: EditText
    private lateinit var edtBio: EditText
    private lateinit var limitText: TextView

    private var selectedImageUri: Uri? = null

    var listener: OnProfileUpdatedListener? = null

    interface OnProfileUpdatedListener {
        fun onProfileUpdated(user: User)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = arguments?.getString("userId")
        cloudinaryService = CloudinaryService(cloudName, uploadPreset)
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                selectedImageUri = it  // Lưu URI tạm thời, không set tag
                avt.setImageURI(it)
            }
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_edit_profile)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCanceledOnTouchOutside(true)

        val btnClose = dialog.findViewById<ImageView>(R.id.btnClose)
        val btnCancel = dialog.findViewById<TextView>(R.id.btnCancel)
        val btnSave = dialog.findViewById<TextView>(R.id.btnSave)

        avt = dialog.findViewById(R.id.avt)
        edtName = dialog.findViewById(R.id.edtName)
        edtBio = dialog.findViewById(R.id.edtBio)
        limitText = dialog.findViewById(R.id.limitText)

        btnClose.setOnClickListener { dismiss() }
        btnCancel.setOnClickListener { dismiss() }

        btnSave.setOnClickListener {
            val newName = edtName.text.toString()
            val newBio = edtBio.text.toString()

            if (newName.isBlank()) {
                Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Nếu có ảnh mới được chọn, upload lên Cloudinary
            if (selectedImageUri != null) {
                uploadAvatarAndSave(newName, newBio)
            } else {
                saveUserProfile(newName, newBio, null)
            }
        }

        lifecycleScope.launch {
            val id = userId ?: return@launch
            val user = userRepository.getUserProfile(id)
            user?.let {
                edtName.setText(it.name)
                edtBio.setText(it.bio)

                // Load avatar hiện tại
                if (it.avatarUrl.isNotEmpty()) {
                    Glide.with(requireContext())
                        .load(it.avatarUrl)
                        .placeholder(R.drawable.profile)
                        .into(avt)
                }

                limitText.text = "${it.bio.length}/150"
            }
        }

        edtBio.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val length = s?.length ?: 0
                limitText.text = "$length/150"
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        avt.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        return dialog
    }

    private fun uploadAvatarAndSave(newName: String, newBio: String) {
        val imageUri = selectedImageUri ?: return

        val progressDialog = ProgressDialog(requireContext()).apply {
            setMessage("Uploading avatar...")
            setCancelable(false)
            show()
        }

        lifecycleScope.launch {
            try {
                // Upload lên Cloudinary
                val imageUrl = cloudinaryService.uploadImage(imageUri, requireContext().contentResolver)

                progressDialog.dismiss()

                if (imageUrl != null) {
                    // Lưu URL Cloudinary vào Firestore
                    saveUserProfile(newName, newBio, imageUrl)
                } else {
                    Toast.makeText(requireContext(), "Upload failed, please try again", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUserProfile(newName: String, newBio: String, newAvatarUrl: String?) {
        lifecycleScope.launch {
            try {
                val currentUserId = userId ?: return@launch

                // Lấy user hiện tại để giữ avatar cũ nếu cần
                val currentUser = userRepository.getUserProfile(currentUserId)

                // Xác định URL cuối cùng
                val finalAvatarUrl = when {
                    newAvatarUrl != null -> newAvatarUrl  // URL mới từ Cloudinary
                    !currentUser?.avatarUrl.isNullOrEmpty() -> currentUser?.avatarUrl ?: ""
                    else -> ""
                }

                val updatedUser = User(
                    id = currentUserId,
                    name = newName,
                    bio = newBio,
                    avatarUrl = finalAvatarUrl
                )

                val success = userRepository.updateUser(updatedUser)
                if (success) {
                    listener?.onProfileUpdated(updatedUser)
                    Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    dismiss()
                } else {
                    Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }
}