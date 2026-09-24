package com.chris.uwa_social.presentation.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.chris.uwa_social.SocialFeedApp
import com.chris.uwa_social.data.connectivity.NetworkMonitor
import com.chris.uwa_social.data.remote.MockPostApi
import com.chris.uwa_social.domain.model.Post
import com.chris.uwa_social.domain.repository.PostRepository
import com.chris.uwa_social.domain.usecase.GetFeedUseCase
import com.chris.uwa_social.domain.usecase.ToggleLikeUseCase
import com.chris.uwa_social.presentation.feed.components.CommentItem
import com.chris.uwa_social.presentation.feed.components.FacebookReaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FeedViewModel(
    private val getFeedUseCase: GetFeedUseCase,
    private val toggleLikeUseCase: ToggleLikeUseCase,
    private val postRepository: PostRepository,
    private val networkMonitor: NetworkMonitor,
    val mockPostApi: MockPostApi? = null
) : ViewModel() {

    val postsFlow: Flow<PagingData<Post>> = getFeedUseCase()
        .cachedIn(viewModelScope)

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = true
        )

    private val _uiState = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    private val _userReactions = MutableStateFlow<Map<String, com.chris.uwa_social.presentation.feed.components.FacebookReaction>>(emptyMap())
    val userReactions: StateFlow<Map<String, com.chris.uwa_social.presentation.feed.components.FacebookReaction>> = _userReactions.asStateFlow()

    fun onReactionSelected(postId: String, reaction: com.chris.uwa_social.presentation.feed.components.FacebookReaction, isCurrentlyLiked: Boolean) {
        _userReactions.value = _userReactions.value + (postId to reaction)
        if (!isCurrentlyLiked) {
            toggleLike(postId)
        }
    }

    fun onLikeTapped(postId: String, isCurrentlyLiked: Boolean) {
        if (isCurrentlyLiked) {
            _userReactions.value = _userReactions.value - postId
        } else {
            _userReactions.value = _userReactions.value + (postId to FacebookReaction.LIKE)
        }
        toggleLike(postId)
    }

    private val _activeCommentPost = MutableStateFlow<Post?>(null)
    val activeCommentPost: StateFlow<Post?> = _activeCommentPost.asStateFlow()

    private val _postComments = MutableStateFlow<Map<String, List<CommentItem>>>(emptyMap())
    val postComments: StateFlow<Map<String, List<CommentItem>>> = _postComments.asStateFlow()

    fun openCommentsFor(post: Post) {
        _activeCommentPost.value = post
        if (!_postComments.value.containsKey(post.id)) {
            _postComments.value = _postComments.value + (post.id to generateInitialCommentsFor(post))
        }
    }

    fun closeComments() {
        _activeCommentPost.value = null
    }

    fun addComment(postId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val newComment = CommentItem(
            id = "c_${System.currentTimeMillis()}",
            authorName = "You",
            authorAvatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&h=200&fit=crop",
            text = trimmed,
            timeAgo = "Just now",
            likesCount = 0,
            isLiked = false
        )

        val existing = _postComments.value[postId] ?: emptyList()
        _postComments.value = _postComments.value + (postId to (existing + newComment))

        _activeCommentPost.value?.let { current ->
            if (current.id == postId) {
                _activeCommentPost.value = current.copy(commentsCount = current.commentsCount + 1)
            }
        }

        viewModelScope.launch {
            postRepository.incrementCommentCount(postId)
        }
    }

    fun toggleCommentLike(postId: String, commentId: String) {
        val currentComments = _postComments.value[postId] ?: return
        val updated = currentComments.map { c ->
            if (c.id == commentId) {
                val newLiked = !c.isLiked
                val newCount = if (newLiked) c.likesCount + 1 else (c.likesCount - 1).coerceAtLeast(0)
                c.copy(isLiked = newLiked, likesCount = newCount)
            } else c
        }
        _postComments.value = _postComments.value + (postId to updated)
    }

    private fun generateInitialCommentsFor(post: Post): List<CommentItem> {
        return listOf(
            CommentItem(
                id = "${post.id}_c1",
                authorName = "Tunde Adeleke",
                authorAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&h=200&fit=crop",
                text = "Completely agree with this perspective. The architecture here sets a really solid benchmark.",
                timeAgo = "1h",
                likesCount = 3,
                isLiked = false
            ),
            CommentItem(
                id = "${post.id}_c2",
                authorName = "Ngozi Eze",
                authorAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&h=200&fit=crop",
                text = "Great insight! Especially loved the clean separation between domain and data layers.",
                timeAgo = "35m",
                likesCount = 1,
                isLiked = false
            )
        )
    }

    private var latestItemCount: Int = 0
    private var latestLoadStates: CombinedLoadStates? = null

    init {
        viewModelScope.launch {
            isOnline.collect { online ->
                latestLoadStates?.let { states ->
                    updateUiState(states, latestItemCount, online)
                }
            }
        }
    }

    fun onLoadStatesChanged(loadStates: CombinedLoadStates, itemCount: Int) {
        latestLoadStates = loadStates
        latestItemCount = itemCount
        updateUiState(loadStates, itemCount, isOnline.value)
    }

    private fun updateUiState(
        loadStates: CombinedLoadStates,
        itemCount: Int,
        online: Boolean
    ) {
        val refresh = loadStates.refresh

        when {
            // Initial loading with zero items
            refresh is LoadState.Loading && itemCount == 0 -> {
                _uiState.value = FeedUiState.Loading
            }

            // Refresh error handling
            refresh is LoadState.Error -> {
                if (itemCount > 0) {
                    // Cache exists; show content with offline/cached banner
                    _uiState.value = FeedUiState.Content(
                        isOffline = true,
                        appendState = mapAppendState(loadStates.append)
                    )
                } else {
                    if (!online) {
                        _uiState.value = FeedUiState.Offline
                    } else {
                        _uiState.value = FeedUiState.Error(
                            loadStates.refresh.let { (it as LoadState.Error).error.message }
                                ?: "We couldn't load the posts. Please try again."
                        )
                    }
                }
            }

            // Successfully loaded (or reading from cache)
            refresh is LoadState.NotLoading -> {
                if (itemCount == 0 && loadStates.append.endOfPaginationReached) {
                    _uiState.value = FeedUiState.Empty
                } else {
                    val appendState = mapAppendState(loadStates.append)
                    _uiState.value = FeedUiState.Content(
                        isOffline = !online,
                        appendState = appendState
                    )
                }
            }
        }
    }

    private fun mapAppendState(append: LoadState): AppendState {
        return when (append) {
            is LoadState.Loading -> AppendState.Loading
            is LoadState.Error -> AppendState.Error("Couldn't load more posts.")
            is LoadState.NotLoading -> AppendState.Idle
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            try {
                toggleLikeUseCase(postId)
            } catch (_: Exception) {
                _snackbarEvent.emit("Couldn't update like. Please try again.")
            }
        }
    }

    fun toggleForceError(onComplete: () -> Unit = {}): Boolean {
        mockPostApi?.let { api ->
            api.forceError = !api.forceError
            viewModelScope.launch {
                if (api.forceError) {
                    postRepository.clearCache()
                }
                onComplete()
            }
            return api.forceError
        }
        return false
    }

    fun toggleForceEmpty(onComplete: () -> Unit = {}): Boolean {
        mockPostApi?.let { api ->
            api.forceEmpty = !api.forceEmpty
            viewModelScope.launch {
                if (api.forceEmpty) {
                    postRepository.clearCache()
                }
                onComplete()
            }
            return api.forceEmpty
        }
        return false
    }

    fun toggleForceNextPageError(): Boolean {
        mockPostApi?.let { api ->
            api.forceNextPageError = !api.forceNextPageError
            viewModelScope.launch {
                if (api.forceNextPageError) {
                    _snackbarEvent.emit("Next-page error armed. Scroll to the bottom to trigger.")
                } else {
                    _snackbarEvent.emit("Next-page error disabled.")
                }
            }
            return api.forceNextPageError
        }
        return false
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SocialFeedApp)
                val container = app.container
                FeedViewModel(
                    getFeedUseCase = container.getFeedUseCase,
                    toggleLikeUseCase = container.toggleLikeUseCase,
                    postRepository = container.postRepository,
                    networkMonitor = container.networkMonitor,
                    mockPostApi = container.mockPostApi
                )
            }
        }
    }
}
