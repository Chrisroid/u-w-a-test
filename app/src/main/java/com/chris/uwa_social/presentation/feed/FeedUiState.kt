package com.chris.uwa_social.presentation.feed

import com.chris.uwa_social.domain.model.Post

sealed interface FeedUiState {
    data object Loading : FeedUiState
    data object Empty : FeedUiState
    data class Error(val message: String) : FeedUiState
    data object Offline : FeedUiState // no network AND no cache
    data class Content(
        val posts: List<Post> = emptyList(),
        val isOffline: Boolean = false, // network down, cache shown
        val appendState: AppendState = AppendState.Idle
    ) : FeedUiState
}

sealed interface AppendState {
    data object Idle : AppendState
    data object Loading : AppendState
    data class Error(val message: String) : AppendState
}
