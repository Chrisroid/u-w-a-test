package com.chris.uwa_social.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val profileImageUrl: String?,
    val text: String,
    val mediaUrl: String?,
    val location: String?,
    val createdAt: String,
    val likesCount: Int,
    val commentsCount: Int,
    val likedByCurrentUser: Boolean,
    val orderIndex: Int = 0
)
