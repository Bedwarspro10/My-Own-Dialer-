package com.example.ui.viewmodel

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

sealed interface HolderCallEvent {
    data class Incoming(val number: String) : HolderCallEvent
    data class Ongoing(val number: String) : HolderCallEvent
    object Idle : HolderCallEvent
}

object CallStateHolder {
    private val _callEventFlow = MutableSharedFlow<HolderCallEvent>(extraBufferCapacity = 10)
    val callEventFlow: SharedFlow<HolderCallEvent> = _callEventFlow

    fun triggerIncomingCall(number: String) {
        _callEventFlow.tryEmit(HolderCallEvent.Incoming(number))
    }

    fun triggerOngoingCall(number: String) {
        _callEventFlow.tryEmit(HolderCallEvent.Ongoing(number))
    }

    fun triggerIdleCall() {
        _callEventFlow.tryEmit(HolderCallEvent.Idle)
    }
}
