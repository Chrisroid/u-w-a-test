package com.chris.uwa_social.data.remote

import com.google.gson.annotations.SerializedName

data class PostApiResponse(
    @SerializedName("posts") val posts: List<PostDto>,
    @SerializedName("page") val page: Int,
    @SerializedName("pageSize") val pageSize: Int,
    @SerializedName("hasNextPage") val hasNextPage: Boolean
)

data class PostDto(
    @SerializedName("id") val id: String,
    @SerializedName("user") val user: UserDto,
    @SerializedName("text") val text: String,
    @SerializedName("media") val media: MediaDto? = null,
    @SerializedName("location") val location: String? = null,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("likesCount") val likesCount: Int,
    @SerializedName("commentsCount") val commentsCount: Int,
    @SerializedName("likedByCurrentUser") val likedByCurrentUser: Boolean = false
)

data class UserDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("profileImage") val profileImage: String? = null
)

data class MediaDto(
    @SerializedName("type") val type: String = "image",
    @SerializedName("url") val url: String
)
