package com.example.socialnetwork

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
<<<<<<< Updated upstream
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.socialnetwork.feed.ui.FeedActivity
=======
import androidx.fragment.app.Fragment
import com.example.socialnetwork.ui.ProfileFragment
import com.example.socialnetwork.ui.SearchFragment
import com.example.socialnetwork.feed.ChatListFragment
import com.example.socialnetwork.feed.FeedFragment
import com.example.socialnetwork.feed.PostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.socialnetwork.ui.ChatActivity
<<<<<<< Updated upstream
>>>>>>> Stashed changes
=======
>>>>>>> Stashed changes
import com.example.socialnetwork.media.MediaTestFragment

class MainActivity : AppCompatActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContentView(R.layout.activity_main)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
//    }

  
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        // test feed ui
//         val intent = Intent(this, FeedActivity::class.java)
//         intent.putExtra("currentUserId", "testUser")
//         startActivity(intent)
//         finish()
        setContentView(R.layout.activity_main)

        // Load fragment test — xóa 3 dòng này sau khi demo xong
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, MediaTestFragment())
                .commit()
        }
    }
}