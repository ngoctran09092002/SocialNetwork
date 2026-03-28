package com.example.socialnetwork.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.socialnetwork.MainActivity
import com.example.socialnetwork.R
import com.google.firebase.auth.FirebaseAuth

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 1. Ánh xạ các View
        val tvAppName = findViewById<TextView>(R.id.tvAppName)
        val tvTagline = findViewById<TextView>(R.id.tvTagline)
        val pbLoading = findViewById<ProgressBar>(R.id.pbLoading)

        // 2. Thiết lập trạng thái ban đầu cho chuyển động
        tvAppName.alpha = 0f
        tvAppName.scaleX = 0.8f
        tvAppName.scaleY = 0.8f

        tvTagline.alpha = 0f
        pbLoading.alpha = 0f

        // 3. Thực hiện chuyển động nối tiếp (Chained Animations)
        tvAppName.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(1200)
            .setStartDelay(200)
            .withEndAction {
                // b. Sau khi App Name xong: Tagline Fades In nhẹ
                tvTagline.visibility = View.VISIBLE
                tvTagline.animate().alpha(1f).setDuration(800).start()

                // c. Vòng xoay Loading Fades In
                pbLoading.visibility = View.VISIBLE
                pbLoading.animate().alpha(1f).setDuration(500).setStartDelay(300).start()

                // d. Đợi thêm một chút để hoàn tất chuyển động rồi mới kiểm tra Login
                Handler(Looper.getMainLooper()).postDelayed({
                    checkLoginStatus()
                }, 1500)
            }
            .start()
    }

    private fun checkLoginStatus() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }
}