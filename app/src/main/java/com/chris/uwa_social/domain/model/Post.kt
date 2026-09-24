package com.chris.uwa_social.domain.model

import java.time.Instant

data class Post(
    val id: String,
    val user: User,
    val text: String,
    val mediaUrl: String?,
    val location: String?,
    val createdAt: Instant,
    val likesCount: Int,
    val commentsCount: Int,
    val isLiked: Boolean
)
