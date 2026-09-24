package com.chris.uwa_social.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface PostApi {
    @GET("posts")
    suspend fun getPosts(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): PostApiResponse
}
