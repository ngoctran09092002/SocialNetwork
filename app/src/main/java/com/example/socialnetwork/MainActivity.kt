package com.example.socialnetwork

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.socialnetwork.ui.ChatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Cách 1: Tự động chuyển sang màn hình Chat ngay khi mở App
        val btnGoToChat = findViewById<Button>(R.id.btnGoToChat)

        btnGoToChat.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)

            // KHÔNG viết finish() ở đây nữa
        }
    }
}