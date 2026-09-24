package com.chris.uwa_social

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.chris.uwa_social.di.AppContainer
import com.chris.uwa_social.di.DefaultAppContainer

class SocialFeedApp : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }

    override fun newImageLoader(): ImageLoader {
        return container.imageLoader
    }
}
