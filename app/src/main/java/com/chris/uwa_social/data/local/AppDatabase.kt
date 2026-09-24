package com.chris.uwa_social.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PostEntity::class, RemoteKeysEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun remoteKeysDao(): RemoteKeysDao
}
