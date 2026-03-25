package com.example.socialnetwork.ui

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.socialn.core.interfaces.IUserRepository
import com.example.socialnetwork.R
import com.example.socialnetwork.core.models.User
import com.example.socialnetwork.mock.MockUserRepository
import kotlinx.coroutines.launch

class EditProfileDialog : DialogFragment() {

    private var userId: String? = null
    private val userRepository: IUserRepository = MockUserRepository

    private lateinit var avt: ImageView
    private lateinit var edtName: EditText
    private lateinit var edtBio: EditText
    private lateinit var limitText: TextView

    var listener: OnProfileUpdatedListener? = null

    interface OnProfileUpdatedListener {
        fun onProfileUpdated(user: User)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = arguments?.getString("userId")
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                avt.setImageURI(it)
                avt.tag = it.toString()
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
            val newAvatarUrl = avt.tag as? String ?: "profile"

            val updatedUser = User(
                id = userId ?: "me",
                name = newName,
                bio = newBio,
                avatarUrl = newAvatarUrl
            )

            lifecycleScope.launch {
                if (userRepository is MockUserRepository) {
                    (userRepository as MockUserRepository).updateUser(updatedUser)
                }

                listener?.onProfileUpdated(updatedUser)
                saveAvatarUri(newAvatarUrl)
                dismiss()
            }
        }

        lifecycleScope.launch {
            val id = userId ?: return@launch
            val user = userRepository.getUserProfile(id)
            user?.let {
                edtName.setText(it.name)
                edtBio.setText(it.bio)

                avt.tag = it.avatarUrl

                if (it.avatarUrl.startsWith("content://")) {
                    avt.setImageURI(android.net.Uri.parse(it.avatarUrl))
                } else {
                    val resId = resources.getIdentifier(
                        it.avatarUrl,
                        "drawable",
                        requireContext().packageName
                    )
                    avt.setImageResource(
                        if (resId != 0) resId else R.drawable.profile
                    )
                }
                limitText.text = "${it.bio?.length ?: 0}/150"
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

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun saveAvatarUri(uri: String) {
        val prefs = requireContext().getSharedPreferences("profile_prefs", 0)
        prefs.edit().putString("my_avatar_uri", uri).apply()
    }
}