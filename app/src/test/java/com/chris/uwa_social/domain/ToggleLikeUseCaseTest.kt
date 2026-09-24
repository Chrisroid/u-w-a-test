package com.chris.uwa_social.domain

import androidx.paging.PagingData
import com.chris.uwa_social.domain.model.Post
import com.chris.uwa_social.domain.model.User
import com.chris.uwa_social.domain.repository.PostRepository
import com.chris.uwa_social.domain.usecase.ToggleLikeUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ToggleLikeUseCaseTest {

    private class FakePostRepository : PostRepository {
        var post = Post(
            id = "test_post_1",
            user = User("u1", "Tester", null),
            text = "Testing like toggling",
            mediaUrl = null,
            location = null,
            createdAt = Instant.now(),
            likesCount = 10,
            commentsCount = 2,
            isLiked = false
        )

        override fun getPosts(): Flow<PagingData<Post>> = emptyFlow()

        override suspend fun toggleLike(postId: String) {
            if (post.id == postId) {
                val newIsLiked = !post.isLiked
                val newLikesCount = if (newIsLiked) post.likesCount + 1 else (post.likesCount - 1).coerceAtLeast(0)
                post = post.copy(isLiked = newIsLiked, likesCount = newLikesCount)
            }
        }

        override suspend fun getCachedPostCount(): Int = 1
    }

    @Test
    fun `toggleLike increments likes count and sets isLiked to true when initially unliked`() = runTest {
        val fakeRepository = FakePostRepository()
        val useCase = ToggleLikeUseCase(fakeRepository)

        useCase("test_post_1")

        assertTrue(fakeRepository.post.isLiked)
        assertEquals(11, fakeRepository.post.likesCount)
    }

    @Test
    fun `toggleLike decrements likes count and sets isLiked to false when previously liked`() = runTest {
        val fakeRepository = FakePostRepository()
        val useCase = ToggleLikeUseCase(fakeRepository)

        // First like
        useCase("test_post_1")
        assertTrue(fakeRepository.post.isLiked)
        assertEquals(11, fakeRepository.post.likesCount)

        // Second like (unlike)
        useCase("test_post_1")
        assertFalse(fakeRepository.post.isLiked)
        assertEquals(10, fakeRepository.post.likesCount)
    }
}
