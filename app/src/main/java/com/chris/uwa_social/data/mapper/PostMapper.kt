package com.chris.uwa_social.data.mapper

import com.chris.uwa_social.data.local.PostEntity
import com.chris.uwa_social.data.remote.PostDto
import com.chris.uwa_social.domain.model.Post
import com.chris.uwa_social.domain.model.User
import java.time.Instant
import java.time.format.DateTimeParseException

fun PostDto.toEntity(orderIndex: Int = 0): PostEntity {
    return PostEntity(
        id = id,
        userId = user.id,
        userName = user.name,
        profileImageUrl = user.profileImage,
        text = text,
        mediaUrl = media?.url,
        location = location,
        createdAt = createdAt,
        likesCount = likesCount,
        commentsCount = commentsCount,
        likedByCurrentUser = likedByCurrentUser,
        orderIndex = orderIndex
    )
}

fun PostEntity.toDomain(): Post {
    val parsedInstant = try {
        Instant.parse(createdAt)
    } catch (_: DateTimeParseException) {
        Instant.EPOCH
    }

    return Post(
        id = id,
        user = User(
            id = userId,
            name = userName,
            profileImageUrl = profileImageUrl
        ),
        text = text,
        mediaUrl = mediaUrl,
        location = location,
        createdAt = parsedInstant,
        likesCount = likesCount,
        commentsCount = commentsCount,
        isLiked = likedByCurrentUser
    )
}

fun Post.toEntity(orderIndex: Int = 0): PostEntity {
    return PostEntity(
        id = id,
        userId = user.id,
        userName = user.name,
        profileImageUrl = user.profileImageUrl,
        text = text,
        mediaUrl = mediaUrl,
        location = location,
        createdAt = createdAt.toString(),
        likesCount = likesCount,
        commentsCount = commentsCount,
        likedByCurrentUser = isLiked,
        orderIndex = orderIndex
    )
}
