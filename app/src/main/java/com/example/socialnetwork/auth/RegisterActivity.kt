package com.example.socialnetwork.auth

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.socialnetwork.MainActivity
import com.example.socialnetwork.R
import com.example.socialnetwork.core.models.User
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // Khai báo các view
    private lateinit var edtName: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtPass: EditText
    private lateinit var edtConfirmPass: EditText
    private lateinit var btnRegister: Button
    private lateinit var pbLoading: ProgressBar
    private lateinit var tvToLogin: TextView

    private lateinit var tilName: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPass: TextInputLayout
    private lateinit var tilConfirmPass: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // 1. Khởi tạo Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // 2. Ánh xạ View
        initViews()

        // 3. Sự kiện Click Đăng ký
        btnRegister.setOnClickListener {
            performRegistration()
        }
        setupSignInLink()
    }

    private fun initViews() {
        edtName = findViewById(R.id.edtRegName)
        edtEmail = findViewById(R.id.edtRegEmail)
        edtPass = findViewById(R.id.edtRegPassword)
        edtConfirmPass = findViewById(R.id.edtRegConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        pbLoading = findViewById(R.id.pbRegisterLoading)
        tvToLogin = findViewById(R.id.tvSignInLink)
        tilName = findViewById(R.id.tilRegName)
        tilEmail = findViewById(R.id.tilRegEmail)
        tilPass = findViewById(R.id.tilRegPassword)
        tilConfirmPass = findViewById(R.id.tilRegConfirmPassword)
    }

    private fun setupSignInLink() {
        val fullText = "Already have an account? Sign In"
        val spannable = SpannableString(fullText)
        val start = fullText.indexOf("Sign In")
        val end = start + "Sign In".length

        if (start != -1) {
            spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(ForegroundColorSpan(Color.WHITE), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    finish() // Quay lại LoginActivity
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false // Tắt gạch chân
                }
            }
            spannable.setSpan(clickableSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        // Gán vào TextView
        tvToLogin.text = spannable
        tvToLogin.movementMethod = LinkMovementMethod.getInstance()
        tvToLogin.highlightColor = Color.TRANSPARENT
    }

    private fun performRegistration() {
        val name = edtName.text.toString().trim()
        val email = edtEmail.text.toString().trim()
        val pass = edtPass.text.toString().trim()
        val confirmPass = edtConfirmPass.text.toString().trim()

        tilName.error = null
        tilEmail.error = null
        tilPass.error = null
        tilConfirmPass.error = null

        // --- VALIDATION ---
        if (name.isEmpty()) {
            tilName.error = "Tên không được để trống"
            return
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Email không hợp lệ"
            return
        }
        if (pass.length < 6) {
            tilPass.error = "Mật khẩu phải từ 6 ký tự trở lên"
            return
        }
        if (pass != confirmPass) {
            tilConfirmPass.error = "Mật khẩu xác nhận không trùng khớp"
            return
        }

        setLoadingState(true)

        // Thực hiện tạo tài khoản
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: ""
                    // GỌI HÀM LƯU DỮ LIỆU VÀO DATABASE
                    saveUserToFirestore(uid, name)
                } else {
                    setLoadingState(false) // Tắt loading nếu lỗi
                    Toast.makeText(this, "Lỗi: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun saveUserToFirestore(uid: String, name: String) {
        val email = edtEmail.text.toString().trim()
        val newUser = User(
            id = uid,
            name = name,
            email = email,
            avatarUrl = "",
            bio = "Chào mừng bạn đến với LINK Social!"
        )

        firestore.collection("users").document(uid)
            .set(newUser)
            .addOnSuccessListener {
                setLoadingState(false)
                Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()

                // Chuyển vào màn hình Login
                FirebaseAuth.getInstance().signOut()
                finish()
            }
            .addOnFailureListener { e ->
                setLoadingState(false)
                Toast.makeText(this, "Lưu thông tin thất bại: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setLoadingState(isLoading: Boolean) {
        if (isLoading) {
            btnRegister.text = ""
            btnRegister.isEnabled = false
            pbLoading.visibility = View.VISIBLE
        } else {
            btnRegister.text = "CREATE ACCOUNT"
            btnRegister.isEnabled = true
            pbLoading.visibility = View.GONE
        }
    }
}