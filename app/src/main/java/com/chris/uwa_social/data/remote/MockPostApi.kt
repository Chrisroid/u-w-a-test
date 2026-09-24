package com.chris.uwa_social.data.remote

import kotlinx.coroutines.delay
import java.io.IOException

class MockPostApi(
    private val simulatedDelayMs: Long = 400L
) : PostApi {

    @Volatile
    var forceError: Boolean = false

    @Volatile
    var forceEmpty: Boolean = false

    @Volatile
    var forceNextPageError: Boolean = false

    private val allPosts: List<PostDto> = generateMockPosts()

    override suspend fun getPosts(page: Int, pageSize: Int): PostApiResponse {
        if (simulatedDelayMs > 0) {
            delay(simulatedDelayMs)
        }

        if (forceError) {
            throw IOException("Simulated network failure: Server unreachable")
        }

        if (page > 1 && forceNextPageError) {
            throw IOException("Simulated pagination error on page $page")
        }

        if (forceEmpty) {
            return PostApiResponse(
                posts = emptyList(),
                page = page,
                pageSize = pageSize,
                hasNextPage = false
            )
        }

        val startIndex = (page - 1) * pageSize
        if (startIndex >= allPosts.size) {
            return PostApiResponse(
                posts = emptyList(),
                page = page,
                pageSize = pageSize,
                hasNextPage = false
            )
        }

        val endIndex = (startIndex + pageSize).coerceAtMost(allPosts.size)
        val pagedPosts = allPosts.subList(startIndex, endIndex)
        val hasNextPage = endIndex < allPosts.size

        return PostApiResponse(
            posts = pagedPosts,
            page = page,
            pageSize = pageSize,
            hasNextPage = hasNextPage
        )
    }

    companion object {
        private fun generateMockPosts(): List<PostDto> {
            val authors = listOf(
                UserDto("u1", "Amina Bello", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&h=200&fit=crop"),
                UserDto("u2", "David Kim", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&h=200&fit=crop"),
                UserDto("u3", "Sarah Jenkins", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&h=200&fit=crop"),
                UserDto("u4", "Carlos Morales", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200&h=200&fit=crop"),
                UserDto("u5", "Zainab Al-Mansoor", "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=200&h=200&fit=crop"),
                UserDto("u6", "Kofi Mensah", "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=200&h=200&fit=crop"),
                UserDto("u7", "Elena Rostova", "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200&h=200&fit=crop"),
                UserDto("u8", "Taro Yamada", "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=200&h=200&fit=crop")
            )

            val locations = listOf(
                "Lagos, Nigeria",
                "San Francisco, CA",
                "Tokyo, Japan",
                "London, UK",
                "Nairobi, Kenya",
                "Berlin, Germany",
                "Sydney, Australia",
                null
            )

            val mediaImages = listOf(
                "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800&fit=crop",
                "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=800&fit=crop",
                null,
                "https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?w=800&fit=crop",
                null,
                "https://images.unsplash.com/photo-1472214103451-9374bd1c798e?w=800&fit=crop",
                "https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=800&fit=crop",
                null
            )

            val postTexts = listOf(
                "Architecture isn't just about syntax; it is about building systems that withstand chaotic edge conditions gracefully.",
                "Sunsets from the new rooftop studio in Lekki. Truly nothing like the evening breeze here.",
                "Quick tip: Don't observe network requests directly in your UI layer. Room as a single source of truth eliminates 90% of state sync bugs.",
                "Morning coffee and getting ready for the design review. Big release coming up next week!",
                "Exploring the coastal trails today. The view across the bay was worth the entire 12km trek.",
                "Zero likes edge test. Sometimes great ideas take time to catch on.",
                "Just launched our new open-source Kotlin multiplatform library. Check out the documentation and let me know your thoughts.",
                "Paging 3 RemoteMediator combined with Room is one of the most resilient patterns for offline-first mobile apps."
            )

            val baseTime = 1774456200000L // 2026-03-25T16:30:00Z epoch ms
            val result = mutableListOf<PostDto>()

            for (i in 0 until 50) {
                val author = authors[i % authors.size]
                val loc = locations[i % locations.size]
                val mediaUrl = mediaImages[i % mediaImages.size]
                val text = if (i % 7 == 0) {
                    "${postTexts[i % postTexts.size]} Here is an extended post with deeper analysis on scalability, latency reduction, and handling memory pressure in resource-constrained Android environments."
                } else {
                    postTexts[i % postTexts.size]
                }
                val likes = if (i == 5 || i == 17) 0 else (12 * (i + 1) + 7)
                val comments = if (i == 5 || i == 17) 0 else (3 * (i + 1))
                val isLiked = if (i == 5 || i == 17) false else (i % 4 == 0)

                // Decreasing time by 25 minutes per post
                val postEpoch = baseTime - (i * 25 * 60 * 1000L)
                val instant = java.time.Instant.ofEpochMilli(postEpoch)

                result.add(
                    PostDto(
                        id = "post_${String.format("%03d", i + 1)}",
                        user = author,
                        text = text,
                        media = mediaUrl?.let { MediaDto("image", it) },
                        location = loc,
                        createdAt = instant.toString(),
                        likesCount = likes,
                        commentsCount = comments,
                        likedByCurrentUser = isLiked
                    )
                )
            }
            return result
        }
    }
}
