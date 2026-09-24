package com.chris.uwa_social.domain.usecase

import androidx.paging.PagingData
import com.chris.uwa_social.domain.model.Post
import com.chris.uwa_social.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow

class GetFeedUseCase(
    private val repository: PostRepository
) {
    operator fun invoke(): Flow<PagingData<Post>> {
        return repository.getPosts()
    }
}
