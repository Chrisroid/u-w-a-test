package com.chris.uwa_social.di

import android.content.Context
import androidx.room.Room
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.chris.uwa_social.data.connectivity.ConnectivityNetworkMonitor
import com.chris.uwa_social.data.connectivity.NetworkMonitor
import com.chris.uwa_social.data.local.AppDatabase
import com.chris.uwa_social.data.remote.MockPostApi
import com.chris.uwa_social.data.repository.PostRepositoryImpl
import com.chris.uwa_social.domain.repository.PostRepository
import com.chris.uwa_social.domain.usecase.GetFeedUseCase
import com.chris.uwa_social.domain.usecase.ToggleLikeUseCase

interface AppContainer {
    val database: AppDatabase
    val mockPostApi: MockPostApi
    val postRepository: PostRepository
    val networkMonitor: NetworkMonitor
    val getFeedUseCase: GetFeedUseCase
    val toggleLikeUseCase: ToggleLikeUseCase
    val imageLoader: ImageLoader
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "social_feed.db"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }

    override val mockPostApi: MockPostApi by lazy {
        MockPostApi(simulatedDelayMs = 400L)
    }

    override val postRepository: PostRepository by lazy {
        PostRepositoryImpl(database, mockPostApi)
    }

    override val networkMonitor: NetworkMonitor by lazy {
        ConnectivityNetworkMonitor(context.applicationContext)
    }

    override val getFeedUseCase: GetFeedUseCase by lazy {
        GetFeedUseCase(postRepository)
    }

    override val toggleLikeUseCase: ToggleLikeUseCase by lazy {
        ToggleLikeUseCase(postRepository)
    }

    override val imageLoader: ImageLoader by lazy {
        ImageLoader.Builder(context.applicationContext)
            .memoryCache {
                MemoryCache.Builder(context.applicationContext)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.applicationContext.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024L * 1024L) // 50MB per AGENTS.md §12
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
