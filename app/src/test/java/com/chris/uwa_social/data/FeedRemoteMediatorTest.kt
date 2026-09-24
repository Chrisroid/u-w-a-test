package com.chris.uwa_social.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.chris.uwa_social.data.local.AppDatabase
import com.chris.uwa_social.data.local.PostDao
import com.chris.uwa_social.data.local.PostEntity
import com.chris.uwa_social.data.local.RemoteKeysDao
import com.chris.uwa_social.data.paging.FeedRemoteMediator
import com.chris.uwa_social.data.remote.PostApi
import com.chris.uwa_social.data.remote.PostApiResponse
import com.chris.uwa_social.data.remote.PostDto
import com.chris.uwa_social.data.remote.UserDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class FeedRemoteMediatorTest {

    private val database = mockk<AppDatabase>(relaxed = true)
    private val postDao = mockk<PostDao>(relaxed = true)
    private val remoteKeysDao = mockk<RemoteKeysDao>(relaxed = true)
    private val api = mockk<PostApi>()

    private lateinit var mediator: FeedRemoteMediator

    @Before
    fun setUp() {
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { database.withTransaction(any<suspend () -> Any>()) } coAnswers {
            secondArg<suspend () -> Any>().invoke()
        }

        coEvery { database.postDao() } returns postDao
        coEvery { database.remoteKeysDao() } returns remoteKeysDao

        mediator = FeedRemoteMediator(database, api)
    }

    private fun createPagingState(
        posts: List<PostEntity> = emptyList(),
        pageSize: Int = 10
    ): PagingState<Int, PostEntity> {
        return PagingState(
            pages = emptyList(),
            anchorPosition = null,
            config = PagingConfig(pageSize = pageSize),
            leadingPlaceholderCount = 0
        )
    }

    @Test
    fun `refresh loadType clears cache and returns Success with endOfPaginationReached false when more pages exist`() = runTest {
        val mockPosts = (1..10).map { i ->
            PostDto(
                id = "p_$i",
                user = UserDto("u_$i", "User $i", null),
                text = "Text $i",
                createdAt = "2026-09-24T12:00:00Z",
                likesCount = 0,
                commentsCount = 0
            )
        }

        coEvery { api.getPosts(page = 1, pageSize = 10) } returns PostApiResponse(
            posts = mockPosts,
            page = 1,
            pageSize = 10,
            hasNextPage = true
        )

        val result = mediator.load(LoadType.REFRESH, createPagingState())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertFalse((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)

        coVerify { postDao.clearAll() }
        coVerify { remoteKeysDao.clearRemoteKeys() }
        coVerify { postDao.insertAll(any()) }
        coVerify { remoteKeysDao.insertAll(any()) }
    }

    @Test
    fun `refresh returns Success with endOfPaginationReached true when hasNextPage is false`() = runTest {
        val mockPosts = listOf(
            PostDto(
                id = "p_final",
                user = UserDto("u_1", "User 1", null),
                text = "Final post",
                createdAt = "2026-09-24T12:00:00Z",
                likesCount = 0,
                commentsCount = 0
            )
        )

        coEvery { api.getPosts(page = 1, pageSize = 10) } returns PostApiResponse(
            posts = mockPosts,
            page = 1,
            pageSize = 10,
            hasNextPage = false
        )

        val result = mediator.load(LoadType.REFRESH, createPagingState())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
    }

    @Test
    fun `refresh returns MediatorResult Error when network call throws IOException`() = runTest {
        coEvery { api.getPosts(page = 1, pageSize = 10) } throws IOException("Timeout")

        val result = mediator.load(LoadType.REFRESH, createPagingState())

        assertTrue(result is RemoteMediator.MediatorResult.Error)
        assertTrue((result as RemoteMediator.MediatorResult.Error).throwable is IOException)
    }

    @Test
    fun `prepend loadType returns Success with endOfPaginationReached true immediately`() = runTest {
        val result = mediator.load(LoadType.PREPEND, createPagingState())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
    }
}
