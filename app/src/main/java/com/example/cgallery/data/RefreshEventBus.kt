package com.example.cgallery.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object RefreshEventBus {
    private val _refreshRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val refreshRequests = _refreshRequests.asSharedFlow()

    fun requestRefresh() {
        _refreshRequests.tryEmit(Unit)
    }
}
