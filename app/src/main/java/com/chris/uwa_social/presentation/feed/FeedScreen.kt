package com.chris.uwa_social.presentation.feed

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.chris.uwa_social.presentation.feed.components.CommentBottomSheet
import com.chris.uwa_social.presentation.feed.components.EmptyState
import com.chris.uwa_social.presentation.feed.components.FullScreenErrorState
import com.chris.uwa_social.presentation.feed.components.FullScreenOfflineState
import com.chris.uwa_social.presentation.feed.components.LoadingView
import com.chris.uwa_social.presentation.feed.components.OfflineBanner
import com.chris.uwa_social.presentation.feed.components.PostCard
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.paging.LoadState
import kotlinx.coroutines.launch

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape

private val UwaCanvasColor = Color(0xFFF4F6F5)
private val UwaBrandPrimary = Color(0xFF22A447)
private val UwaBrandAccent = Color(0xFF38B449)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = viewModel(factory = FeedViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val userReactions by viewModel.userReactions.collectAsState()
    val activeCommentPost by viewModel.activeCommentPost.collectAsState()
    val postComments by viewModel.postComments.collectAsState()
    val lazyPagingItems = viewModel.postsFlow.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }
    var forceErrorActive by remember { mutableStateOf(false) }
    var forceEmptyActive by remember { mutableStateOf(false) }
    var forceNextPageErrorActive by remember { mutableStateOf(false) }

    LaunchedEffect(lazyPagingItems.loadState, lazyPagingItems.itemCount) {
        viewModel.onLoadStatesChanged(
            loadStates = lazyPagingItems.loadState,
            itemCount = lazyPagingItems.itemCount
        )
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "UWA",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = UwaBrandPrimary,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Social",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = (-0.5).sp
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(UwaBrandAccent, CircleShape)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(onClick = { lazyPagingItems.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh feed",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Reviewer options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(if (forceErrorActive) "Disable Forced Error" else "Simulate Server Error (Page 1)")
                                },
                                onClick = {
                                    forceErrorActive = viewModel.toggleForceError {
                                        lazyPagingItems.refresh()
                                    }
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(if (forceNextPageErrorActive) "Disable Next-Page Error" else "Simulate Next-Page Error")
                                },
                                onClick = {
                                    forceNextPageErrorActive = viewModel.toggleForceNextPageError()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(if (forceEmptyActive) "Disable Empty State" else "Simulate Empty State")
                                },
                                onClick = {
                                    forceEmptyActive = viewModel.toggleForceEmpty {
                                        lazyPagingItems.refresh()
                                    }
                                    showMenu = false
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(UwaCanvasColor)
        ) {
            when (val state = uiState) {
                is FeedUiState.Loading -> {
                    LoadingView()
                }

                is FeedUiState.Empty -> {
                    EmptyState(onRetry = {
                        if (forceEmptyActive) {
                            forceEmptyActive = viewModel.toggleForceEmpty {
                                lazyPagingItems.refresh()
                            }
                        } else {
                            lazyPagingItems.refresh()
                        }
                    })
                }

                is FeedUiState.Offline -> {
                    FullScreenOfflineState(onRetry = { lazyPagingItems.refresh() })
                }

                is FeedUiState.Error -> {
                    FullScreenErrorState(
                        message = state.message,
                        onRetry = {
                            if (forceErrorActive) {
                                forceErrorActive = viewModel.toggleForceError {
                                    lazyPagingItems.refresh()
                                }
                            } else {
                                lazyPagingItems.refresh()
                            }
                        }
                    )
                }

                is FeedUiState.Content -> {
                    val isRefreshing = lazyPagingItems.loadState.refresh is LoadState.Loading && lazyPagingItems.itemCount > 0

                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { lazyPagingItems.refresh() },
                        modifier = Modifier
                            .fillMaxSize()
                            .background(UwaCanvasColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(UwaCanvasColor)
                        ) {
                        if (state.isOffline) {
                            OfflineBanner()
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(
                                count = lazyPagingItems.itemCount,
                                key = lazyPagingItems.itemKey { it.id }
                            ) { index ->
                                val post = lazyPagingItems[index]
                                if (post != null) {
                                    PostCard(
                                        post = post,
                                        selectedReaction = userReactions[post.id],
                                        onReactionSelected = { reaction ->
                                            viewModel.onReactionSelected(post.id, reaction, post.isLiked)
                                        },
                                        onLikeClicked = { postId ->
                                            viewModel.onLikeTapped(postId, post.isLiked)
                                        },
                                        onCommentClicked = {
                                            viewModel.openCommentsFor(post)
                                        },
                                        onShareClicked = { targetPost ->
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, "Shared post from ${targetPost.user.name}")
                                                putExtra(Intent.EXTRA_TEXT, "${targetPost.user.name}: \"${targetPost.text}\"")
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share post via"))
                                        }
                                    )
                                }
                            }

                            // Append state row at bottom of list
                            item {
                                when (val append = state.appendState) {
                                    is AppendState.Loading -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    }

                                    is AppendState.Error -> {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = append.message,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(onClick = { lazyPagingItems.retry() }) {
                                                Text("Retry")
                                            }
                                        }
                                    }

                                    is AppendState.Idle -> {
                                        // Nothing to show when idle
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

    activeCommentPost?.let { post ->
        CommentBottomSheet(
            post = post,
            comments = postComments[post.id] ?: emptyList(),
            onDismissRequest = { viewModel.closeComments() },
            onAddComment = { text -> viewModel.addComment(post.id, text) },
            onLikeComment = { commentId -> viewModel.toggleCommentLike(post.id, commentId) }
        )
    }
}
