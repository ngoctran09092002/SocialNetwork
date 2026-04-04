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
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    // 1. KHAI BÁO BIẾN
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var pbLoading: ProgressBar
    private lateinit var tvToRegister: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // 2. ÁNH XẠ VIEW
        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        btnLogin = findViewById(R.id.btnLogin)
        pbLoading = findViewById(R.id.pbLoginLoading)
        tvToRegister = findViewById(R.id.tvToRegister)

        // 3. XỬ LÝ SỰ KIỆN CLICK
        btnLogin.setOnClickListener {
            validateAndLogin()
        }
        setupSignUpLink()
    }

    private fun setupSignUpLink() {
        val fullText = "Don't have an account? Sign Up"
        val spannable = SpannableString(fullText)
        val start = fullText.indexOf("Sign Up")
        val end = start + "Sign Up".length

        if (start != -1) {
            spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(ForegroundColorSpan(Color.WHITE), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            //Tạo sự kiện Click cho "Sign Up"
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val intent = Intent(this@LoginActivity, RegisterActivity::class.java)
                    startActivity(intent)
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false // Tắt gạch chân
                }
            }
            spannable.setSpan(clickableSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        // Gán vào TextView
        tvToRegister.text = spannable
        tvToRegister.movementMethod = LinkMovementMethod.getInstance()
        tvToRegister.highlightColor = Color.TRANSPARENT
    }

    // 4. HÀM KIỂM TRA DỮ LIỆU VÀ ĐĂNG NHẬP
    private fun validateAndLogin() {
        val email = edtEmail.text.toString().trim()
        val pass = edtPassword.text.toString().trim()

        if (email.isEmpty()) {
            edtEmail.error = "Vui lòng nhập Email"
            edtEmail.requestFocus()
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.error = "Email không đúng định dạng"
            edtEmail.requestFocus()
            return
        }
        if (pass.isEmpty()) {
            edtPassword.error = "Vui lòng nhập mật khẩu"
            edtPassword.requestFocus()
            return
        }

        // Hiện vòng xoay Loading và tắt nút bấm
        setLoadingState(true)

        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                // Tắt Loading khi có kết quả
                setLoadingState(false)

                if (task.isSuccessful) {
                    Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Sai tài khoản hoặc mật khẩu!", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // 5. HÀM ĐIỀU KHIỂN TRẠNG THÁI LOADING
    private fun setLoadingState(isLoading: Boolean) {
        if (isLoading) {
            btnLogin.text = ""
            btnLogin.isEnabled = false
            pbLoading.visibility = View.VISIBLE
        } else {
            btnLogin.text = "SIGN IN"
            btnLogin.isEnabled = true
            pbLoading.visibility = View.GONE
        }
    }
}