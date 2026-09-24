package com.chris.uwa_social.presentation

import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import com.chris.uwa_social.data.connectivity.NetworkMonitor
import com.chris.uwa_social.domain.model.Post
import com.chris.uwa_social.domain.repository.PostRepository
import com.chris.uwa_social.domain.usecase.GetFeedUseCase
import com.chris.uwa_social.domain.usecase.ToggleLikeUseCase
import com.chris.uwa_social.presentation.feed.AppendState
import com.chris.uwa_social.presentation.feed.FeedUiState
import com.chris.uwa_social.presentation.feed.FeedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeIsOnline = MutableStateFlow(true)

    private val fakeNetworkMonitor = object : NetworkMonitor {
        override val isOnline: Flow<Boolean> = fakeIsOnline
    }

    private val fakeRepository = object : PostRepository {
        var toggleLikeCalled = false
        override fun getPosts(): Flow<PagingData<Post>> = emptyFlow()
        override suspend fun toggleLike(postId: String) {
            toggleLikeCalled = true
        }
        override suspend fun getCachedPostCount(): Int = 0
    }

    private lateinit var viewModel: FeedViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeIsOnline.value = true
        viewModel = FeedViewModel(
            getFeedUseCase = GetFeedUseCase(fakeRepository),
            toggleLikeUseCase = ToggleLikeUseCase(fakeRepository),
            postRepository = fakeRepository,
            networkMonitor = fakeNetworkMonitor
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createLoadStates(
        refresh: LoadState = LoadState.NotLoading(endOfPaginationReached = false),
        append: LoadState = LoadState.NotLoading(endOfPaginationReached = false),
        prepend: LoadState = LoadState.NotLoading(endOfPaginationReached = true)
    ): CombinedLoadStates {
        val states = LoadStates(refresh = refresh, prepend = prepend, append = append)
        return CombinedLoadStates(
            refresh = refresh,
            prepend = prepend,
            append = append,
            source = states
        )
    }

    @Test
    fun `initial state is Loading when refresh is Loading and count is 0`() = runTest {
        viewModel.onLoadStatesChanged(
            loadStates = createLoadStates(refresh = LoadState.Loading),
            itemCount = 0
        )
        advanceUntilIdle()

        assertEquals(FeedUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `state is Content when refresh is NotLoading and items are present`() = runTest {
        viewModel.onLoadStatesChanged(
            loadStates = createLoadStates(refresh = LoadState.NotLoading(false)),
            itemCount = 10
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is FeedUiState.Content)
        assertEquals(false, (state as FeedUiState.Content).isOffline)
        assertEquals(AppendState.Idle, state.appendState)
    }

    @Test
    fun `state is Empty when refresh is NotLoading and end of pagination is reached with 0 items`() = runTest {
        viewModel.onLoadStatesChanged(
            loadStates = createLoadStates(
                refresh = LoadState.NotLoading(true),
                append = LoadState.NotLoading(true)
            ),
            itemCount = 0
        )
        advanceUntilIdle()

        assertEquals(FeedUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `state is Offline when refresh fails, device is offline, and cache is empty`() = runTest {
        fakeIsOnline.value = false
        advanceUntilIdle()

        viewModel.onLoadStatesChanged(
            loadStates = createLoadStates(refresh = LoadState.Error(IOException("Network error"))),
            itemCount = 0
        )
        advanceUntilIdle()

        assertEquals(FeedUiState.Offline, viewModel.uiState.value)
    }

    @Test
    fun `state is Content with isOffline true when refresh fails but cache has items`() = runTest {
        fakeIsOnline.value = false
        advanceUntilIdle()

        viewModel.onLoadStatesChanged(
            loadStates = createLoadStates(refresh = LoadState.Error(IOException("Network error"))),
            itemCount = 5
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is FeedUiState.Content)
        assertTrue((state as FeedUiState.Content).isOffline)
    }

    @Test
    fun `state is Error when refresh fails with server error while online and cache is empty`() = runTest {
        fakeIsOnline.value = true
        advanceUntilIdle()

        viewModel.onLoadStatesChanged(
            loadStates = createLoadStates(refresh = LoadState.Error(RuntimeException("Server 500"))),
            itemCount = 0
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is FeedUiState.Error)
        assertEquals("Server 500", (state as FeedUiState.Error).message)
    }

    @Test
    fun `pagination error sets appendState to Error while maintaining Content state`() = runTest {
        fakeIsOnline.value = true
        advanceUntilIdle()

        viewModel.onLoadStatesChanged(
            loadStates = createLoadStates(
                refresh = LoadState.NotLoading(false),
                append = LoadState.Error(IOException("Failed to load page 2"))
            ),
            itemCount = 10
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is FeedUiState.Content)
        assertTrue((state as FeedUiState.Content).appendState is AppendState.Error)
    }

    @Test
    fun `toggleLike delegates to use case`() = runTest {
        viewModel.toggleLike("post_123")
        advanceUntilIdle()

        assertTrue(fakeRepository.toggleLikeCalled)
    }

    @Test
    fun `onReactionSelected retains selected reaction and delegates like if unliked`() = runTest {
        viewModel.onReactionSelected(
            postId = "post_999",
            reaction = com.chris.uwa_social.presentation.feed.components.FacebookReaction.LOVE,
            isCurrentlyLiked = false
        )
        advanceUntilIdle()

        assertEquals(
            com.chris.uwa_social.presentation.feed.components.FacebookReaction.LOVE,
            viewModel.userReactions.value["post_999"]
        )
        assertTrue(fakeRepository.toggleLikeCalled)
    }

    @Test
    fun `onLikeTapped removes reaction from map when previously liked`() = runTest {
        viewModel.onReactionSelected(
            postId = "post_888",
            reaction = com.chris.uwa_social.presentation.feed.components.FacebookReaction.HAHA,
            isCurrentlyLiked = false
        )
        advanceUntilIdle()

        viewModel.onLikeTapped(postId = "post_888", isCurrentlyLiked = true)
        advanceUntilIdle()

        assertEquals(null, viewModel.userReactions.value["post_888"])
    }
}
