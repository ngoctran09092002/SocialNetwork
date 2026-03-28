package com.example.socialnetwork.feed.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.socialnetwork.features.feed.repository.FeedRepository

class FeedViewModelFactory(
    private val repository: FeedRepository,
    private val currentUserId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FeedViewModel(repository, currentUserId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}