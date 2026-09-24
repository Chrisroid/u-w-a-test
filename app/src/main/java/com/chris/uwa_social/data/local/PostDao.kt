package com.chris.uwa_social.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PostDao {

    @Query("SELECT * FROM posts ORDER BY orderIndex ASC")
    fun getPagingSource(): PagingSource<Int, PostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<PostEntity>)

    @Query("DELETE FROM posts")
    suspend fun clearAll()

    @Query("SELECT * FROM posts WHERE id = :postId")
    suspend fun getPostById(postId: String): PostEntity?

    @Query("UPDATE posts SET likedByCurrentUser = :isLiked, likesCount = :likesCount WHERE id = :postId")
    suspend fun updateLike(postId: String, isLiked: Boolean, likesCount: Int)

    @Query("UPDATE posts SET commentsCount = :commentsCount WHERE id = :postId")
    suspend fun updateCommentCount(postId: String, commentsCount: Int)

    @Query("SELECT COUNT(*) FROM posts")
    suspend fun getCount(): Int
}
