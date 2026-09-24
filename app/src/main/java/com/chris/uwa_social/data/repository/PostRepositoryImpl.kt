package com.chris.uwa_social.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.chris.uwa_social.data.local.AppDatabase
import androidx.room.withTransaction
import com.chris.uwa_social.data.mapper.toDomain
import com.chris.uwa_social.data.paging.FeedRemoteMediator
import com.chris.uwa_social.data.remote.PostApi
import com.chris.uwa_social.domain.model.Post
import com.chris.uwa_social.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PostRepositoryImpl(
    private val database: AppDatabase,
    private val api: PostApi
) : PostRepository {

    private val postDao = database.postDao()

    @OptIn(ExperimentalPagingApi::class)
    override fun getPosts(): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(
                pageSize = 10,
                prefetchDistance = 5,
                enablePlaceholders = false
            ),
            remoteMediator = FeedRemoteMediator(database, api),
            pagingSourceFactory = { postDao.getPagingSource() }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override suspend fun toggleLike(postId: String) {
        val post = postDao.getPostById(postId) ?: return
        val newIsLiked = !post.likedByCurrentUser
        val newLikesCount = if (newIsLiked) post.likesCount + 1 else (post.likesCount - 1).coerceAtLeast(0)
        postDao.updateLike(postId, newIsLiked, newLikesCount)
    }

    override suspend fun getCachedPostCount(): Int {
        return postDao.getCount()
    }

    override suspend fun incrementCommentCount(postId: String) {
        val post = postDao.getPostById(postId) ?: return
        postDao.updateCommentCount(postId, post.commentsCount + 1)
    }

    override suspend fun clearCache() {
        database.withTransaction {
            database.remoteKeysDao().clearRemoteKeys()
            postDao.clearAll()
        }
    }
}
