package com.tabletaide.ide.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkspaceMutationBus @Inject constructor() {
    private val _writes = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val writes: SharedFlow<String> = _writes

    fun notifyFileWritten(relativePath: String) {
        _writes.tryEmit(relativePath)
    }
}
