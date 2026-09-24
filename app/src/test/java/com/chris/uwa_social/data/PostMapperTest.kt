package com.chris.uwa_social.data

import com.chris.uwa_social.data.local.PostEntity
import com.chris.uwa_social.data.mapper.toDomain
import com.chris.uwa_social.data.mapper.toEntity
import com.chris.uwa_social.data.remote.MediaDto
import com.chris.uwa_social.data.remote.PostDto
import com.chris.uwa_social.data.remote.UserDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class PostMapperTest {

    @Test
    fun `PostDto toEntity maps all fields correctly`() {
        val dto = PostDto(
            id = "p1",
            user = UserDto(id = "u1", name = "Ada Lovelace", profileImage = "https://image.url/avatar.png"),
            text = "Writing the first algorithm.",
            media = MediaDto(type = "image", url = "https://image.url/media.jpg"),
            location = "London, UK",
            createdAt = "2026-09-24T12:00:00Z",
            likesCount = 42,
            commentsCount = 7,
            likedByCurrentUser = true
        )

        val entity = dto.toEntity(orderIndex = 5)

        assertEquals("p1", entity.id)
        assertEquals("u1", entity.userId)
        assertEquals("Ada Lovelace", entity.userName)
        assertEquals("https://image.url/avatar.png", entity.profileImageUrl)
        assertEquals("Writing the first algorithm.", entity.text)
        assertEquals("https://image.url/media.jpg", entity.mediaUrl)
        assertEquals("London, UK", entity.location)
        assertEquals("2026-09-24T12:00:00Z", entity.createdAt)
        assertEquals(42, entity.likesCount)
        assertEquals(7, entity.commentsCount)
        assertTrue(entity.likedByCurrentUser)
        assertEquals(5, entity.orderIndex)
    }

    @Test
    fun `PostEntity toDomain parses timestamp and nested User correctly`() {
        val entity = PostEntity(
            id = "p2",
            userId = "u2",
            userName = "Alan Turing",
            profileImageUrl = null,
            text = "Testing the universal machine.",
            mediaUrl = null,
            location = null,
            createdAt = "2026-09-24T14:30:00Z",
            likesCount = 100,
            commentsCount = 12,
            likedByCurrentUser = false,
            orderIndex = 0
        )

        val domain = entity.toDomain()

        assertEquals("p2", domain.id)
        assertEquals("u2", domain.user.id)
        assertEquals("Alan Turing", domain.user.name)
        assertNull(domain.user.profileImageUrl)
        assertEquals("Testing the universal machine.", domain.text)
        assertNull(domain.mediaUrl)
        assertNull(domain.location)
        assertEquals(Instant.parse("2026-09-24T14:30:00Z"), domain.createdAt)
        assertEquals(100, domain.likesCount)
        assertEquals(12, domain.commentsCount)
        assertFalse(domain.isLiked)
    }

    @Test
    fun `PostEntity toDomain handles invalid timestamp without throwing`() {
        val entity = PostEntity(
            id = "p3",
            userId = "u3",
            userName = "Grace Hopper",
            profileImageUrl = null,
            text = "Compiler bug found.",
            mediaUrl = null,
            location = null,
            createdAt = "not-a-valid-timestamp",
            likesCount = 5,
            commentsCount = 1,
            likedByCurrentUser = false,
            orderIndex = 0
        )

        val domain = entity.toDomain()

        assertEquals(Instant.EPOCH, domain.createdAt)
    }
}
