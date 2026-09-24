package com.chris.uwa_social.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remote_keys")
data class RemoteKeysEntity(
    @PrimaryKey val postId: String,
    val prevKey: Int?,
    val nextKey: Int?
)
