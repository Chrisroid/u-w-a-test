package com.chris.uwa_social.domain.repository

import androidx.paging.PagingData
import com.chris.uwa_social.domain.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun getPosts(): Flow<PagingData<Post>>
    suspend fun toggleLike(postId: String)
    suspend fun getCachedPostCount(): Int
    suspend fun incrementCommentCount(postId: String) {}
    suspend fun clearCache() {}
}
