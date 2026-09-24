package com.chris.uwa_social.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.chris.uwa_social.data.local.AppDatabase
import com.chris.uwa_social.data.local.PostEntity
import com.chris.uwa_social.data.local.RemoteKeysEntity
import com.chris.uwa_social.data.mapper.toEntity
import com.chris.uwa_social.data.remote.PostApi
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class FeedRemoteMediator(
    private val database: AppDatabase,
    private val api: PostApi
) : RemoteMediator<Int, PostEntity>() {

    private val postDao = database.postDao()
    private val remoteKeysDao = database.remoteKeysDao()

    override suspend fun initialize(): InitializeAction {
        // Launch refresh if network is available; Room cache persists
        return InitializeAction.LAUNCH_INITIAL_REFRESH
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>
    ): MediatorResult {
        return try {
            val page = when (loadType) {
                LoadType.REFRESH -> {
                    val remoteKeys = getRemoteKeyClosestToCurrentPosition(state)
                    remoteKeys?.nextKey?.minus(1) ?: 1
                }
                LoadType.PREPEND -> {
                    return MediatorResult.Success(endOfPaginationReached = true)
                }
                LoadType.APPEND -> {
                    val remoteKeys = getRemoteKeyForLastItem(state)
                    val nextKey = remoteKeys?.nextKey
                    if (nextKey == null) {
                        return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
                    }
                    nextKey
                }
            }

            val pageSize = state.config.pageSize
            val response = api.getPosts(page = page, pageSize = pageSize)
            val posts = response.posts
            val endOfPaginationReached = !response.hasNextPage || posts.isEmpty()

            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    remoteKeysDao.clearRemoteKeys()
                    postDao.clearAll()
                }

                val prevKey = if (page == 1) null else page - 1
                val nextKey = if (endOfPaginationReached) null else page + 1

                val startingOrderIndex = (page - 1) * pageSize
                val entities = posts.mapIndexed { index, postDto ->
                    postDto.toEntity(orderIndex = startingOrderIndex + index)
                }
                val keys = posts.map { postDto ->
                    RemoteKeysEntity(
                        postId = postDto.id,
                        prevKey = prevKey,
                        nextKey = nextKey
                    )
                }

                remoteKeysDao.insertAll(keys)
                postDao.insertAll(entities)
            }

            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }

    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, PostEntity>): RemoteKeysEntity? {
        return state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()?.let { post ->
            remoteKeysDao.getRemoteKeyByPostId(post.id)
        }
    }

    private suspend fun getRemoteKeyClosestToCurrentPosition(state: PagingState<Int, PostEntity>): RemoteKeysEntity? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.id?.let { id ->
                remoteKeysDao.getRemoteKeyByPostId(id)
            }
        }
    }
}
