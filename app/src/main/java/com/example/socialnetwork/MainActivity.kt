package com.example.socialnetwork

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.socialnetwork.ui.ProfileFragment
import com.example.socialnetwork.ui.SearchFragment
import com.example.socialnetwork.feed.ChatListFragment
import com.example.socialnetwork.feed.FeedFragment
import com.example.socialnetwork.feed.PostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.socialnetwork.ui.ChatActivity
import com.example.socialnetwork.feed.ui.FeedActivity
import com.example.socialnetwork.media.MediaTestFragment

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Hiển thị mặc định màn hình Feed
        if (savedInstanceState == null) {
            replaceFragment(FeedFragment())
            bottomNav.selectedItemId = R.id.nav_feed
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_feed -> {
                    replaceFragment(FeedFragment()) // Node 2
                    true
                }
                R.id.nav_chat -> {
                    replaceFragment(ChatListFragment()) // Node 4
                    true
                }
                R.id.nav_post -> {
                    replaceFragment(PostFragment()) // Node 3
                    true
                }
                R.id.nav_profile -> {
                    replaceFragment(ProfileFragment()) // Node 5
                    true
                }
                R.id.nav_search -> {
                    replaceFragment(SearchFragment())//Tìm kiếm
                    true
                }

                else -> false
            }
        }
    }

    // Hàm chuyển tab các màn hình
    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment_container, fragment)
        fragmentTransaction.commit()
    }
}