package com.example.socialnetwork

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.socialnetwork.media.MediaTestFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Load fragment test — xóa 3 dòng này sau khi demo xong
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, MediaTestFragment())
                .commit()
        }
    }
}