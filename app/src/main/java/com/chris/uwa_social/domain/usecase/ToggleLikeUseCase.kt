package com.chris.uwa_social.domain.usecase

import com.chris.uwa_social.domain.repository.PostRepository

class ToggleLikeUseCase(
    private val repository: PostRepository
) {
    suspend operator fun invoke(postId: String) {
        repository.toggleLike(postId)
    }
}
