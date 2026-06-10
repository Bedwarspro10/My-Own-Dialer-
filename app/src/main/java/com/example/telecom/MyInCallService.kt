package com.example.telecom

import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log
import com.example.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object TelecomCallManager {
    private val _currentCall = MutableStateFlow<Call?>(null)
    val currentCall: StateFlow<Call?> = _currentCall

    private val _callState = MutableStateFlow(Call.STATE_DISCONNECTED)
    val callState: StateFlow<Int> = _callState

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber

    private val _audioState = MutableStateFlow<CallAudioState?>(null)
    val audioState: StateFlow<CallAudioState?> = _audioState

    // Global weak/static reference to the active InCallService instance to route speaker/mute
    var activeInCallService: InCallService? = null

    fun updateCall(call: Call?) {
        _currentCall.value = call
        if (call != null) {
            _callState.value = call.state
            val uri = call.details?.handle
            _phoneNumber.value = uri?.schemeSpecificPart ?: ""
        } else {
            _callState.value = Call.STATE_DISCONNECTED
            _phoneNumber.value = ""
        }
    }

    fun updateState(state: Int) {
        _callState.value = state
    }

    fun updateAudioState(audioState: CallAudioState?) {
        _audioState.value = audioState
    }

    // Call Actions
    fun answerCall() {
        _currentCall.value?.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY)
    }

    fun disconnectCall() {
        _currentCall.value?.disconnect()
    }

    fun toggleMute(mute: Boolean) {
        activeInCallService?.setMuted(mute)
    }

    fun toggleSpeaker(speakerOn: Boolean) {
        val route = if (speakerOn) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE
        activeInCallService?.setAudioRoute(route)
    }

    fun holdCall(hold: Boolean) {
        if (hold) {
            _currentCall.value?.hold()
        } else {
            _currentCall.value?.unhold()
        }
    }

    fun playDtmf(digit: Char) {
        _currentCall.value?.playDtmfTone(digit)
        _currentCall.value?.stopDtmfTone()
    }
}

class MyInCallService : InCallService() {

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            Log.d("MyInCallService", "Call state changed: $state")
            TelecomCallManager.updateCall(call)
            if (state == Call.STATE_DISCONNECTED) {
                TelecomCallManager.updateCall(null)
            }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.d("MyInCallService", "Call added: $call")
        TelecomCallManager.activeInCallService = this
        call.registerCallback(callCallback)
        TelecomCallManager.updateCall(call)

        // Force launch our active call screen / MainActivity
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        startActivity(launchIntent)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d("MyInCallService", "Call removed: $call")
        call.unregisterCallback(callCallback)
        TelecomCallManager.updateCall(null)
        if (TelecomCallManager.activeInCallService == this) {
            TelecomCallManager.activeInCallService = null
        }
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        Log.d("MyInCallService", "Audio state changed: $audioState")
        TelecomCallManager.updateAudioState(audioState)
    }
}
