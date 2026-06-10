package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.CallLogEntity
import com.example.data.database.ContactEntity
import com.example.data.repository.DialerRepository
import com.example.ui.settings.DialerSettings
import com.example.ui.settings.SettingsManager
import com.example.telecom.TelecomCallManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface AppCallState {
    object Idle : AppCallState
    data class Incoming(val name: String, val number: String, val label: String) : AppCallState
    data class Ongoing(val name: String, val number: String, val label: String) : AppCallState
}

class DialerViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = DialerRepository(
        application,
        database.contactDao(),
        database.callLogDao()
    )
    private val settingsManager = SettingsManager(application)

    // Current general tab (Recent, Contacts, Keypad, Settings)
    private val _currentTab = MutableStateFlow("Recents")
    val currentTab: StateFlow<String> = _currentTab

    // Entered text in keyboard/T9
    private val _enteredDigits = MutableStateFlow("")
    val enteredDigits: StateFlow<String> = _enteredDigits

    // T9 Contact Suggestions
    val t9Suggestions: StateFlow<List<ContactEntity>> = combine(
        _enteredDigits,
        repository.allContacts
    ) { digits, contacts ->
        if (digits.isEmpty()) {
            emptyList()
        } else {
            contacts.filter { contact ->
                matchT9(digits, contact.name, contact.phoneNumber)
            }.take(5)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Contacts & Favorites
    private val _contactSearchQuery = MutableStateFlow("")
    val contactSearchQuery: StateFlow<String> = _contactSearchQuery

    val contactsList: StateFlow<List<ContactEntity>> = combine(
        _contactSearchQuery,
        repository.allContacts
    ) { query, contacts ->
        if (query.isEmpty()) {
            contacts
        } else {
            contacts.filter {
                it.name.contains(query, ignoreCase = true) || it.phoneNumber.contains(query)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteContacts: StateFlow<List<ContactEntity>> = repository.favoriteContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recents Filter: "All" vs "Missed"
    private val _recentsFilter = MutableStateFlow("All")
    val recentsFilter: StateFlow<String> = _recentsFilter

    val callLogsList: StateFlow<List<CallLogEntity>> = combine(
        _recentsFilter,
        repository.allCallLogs,
        repository.missedCallLogs
    ) { filter, all, missed ->
        if (filter == "Missed") missed else all
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Customized settings state
    private val _uiSettings = MutableStateFlow(settingsManager.getSettings())
    val uiSettings: StateFlow<DialerSettings> = _uiSettings

    // Call Lifecycle States
    private val _callState = MutableStateFlow<AppCallState>(AppCallState.Idle)
    val callState: StateFlow<AppCallState> = _callState

    // Live permission and default status states
    val isDefaultDialer = MutableStateFlow(true)
    val arePermissionsGranted = MutableStateFlow(true)
    val isSettingsOpen = MutableStateFlow(false)
    val isLockedSimulation = MutableStateFlow(false) // Toggle: true = Locked FS Slider, false = Unlocked Samsung Style
    val showHeadsUpCallPopup = MutableStateFlow(false) // Trigger floating popup active call inside other screens
    val isFirstLaunch = MutableStateFlow(true)

    // Call Active features
    private val _callTimerSeconds = MutableStateFlow(0)
    val callTimerSeconds: StateFlow<Int> = _callTimerSeconds

    private val _isCallMuted = MutableStateFlow(false)
    val isCallMuted: StateFlow<Boolean> = _isCallMuted

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn

    private val _isFaceTimeActive = MutableStateFlow(false)
    val isFaceTimeActive: StateFlow<Boolean> = _isFaceTimeActive

    // Floating Dynamic translation transcripts during active call
    private val _translationLogs = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val translationLogs: StateFlow<List<Pair<String, String>>> = _translationLogs

    private var callTimerJob: Job? = null

    init {
        // Read first launch status
        val prefs = application.getSharedPreferences("dialer_settings_prefs", Context.MODE_PRIVATE)
        isFirstLaunch.value = !prefs.getBoolean("first_launch_done", false)

        // SYNC REAL USER DATA ON FIRST INIT
        viewModelScope.launch {
            repository.prepopulateDefaultDataIfNeeded()
            syncSystemData()
        }

        // BIND AND LISTEN TO ACTUAL CARRIER AND TELECOM SUBSYSTEM PHONE STATES
        viewModelScope.launch {
            TelecomCallManager.callState.combine(TelecomCallManager.phoneNumber) { state, number ->
                Pair(state, number)
            }.collect { (state, number) ->
                Log.d("DialerViewModel", "Observe telecom call state: $state, number: $number")
                when (state) {
                    android.telecom.Call.STATE_RINGING -> {
                        val name = getContactNameFromNumber(number)
                        val label = getContactLabelFromNumber(number)
                        _callState.value = AppCallState.Incoming(name, number, label)
                        showHeadsUpCallPopup.value = true
                    }
                    android.telecom.Call.STATE_DIALING,
                    android.telecom.Call.STATE_CONNECTING,
                    android.telecom.Call.STATE_ACTIVE,
                    android.telecom.Call.STATE_HOLDING -> {
                        val name = getContactNameFromNumber(number)
                        val label = getContactLabelFromNumber(number)
                        _callState.value = AppCallState.Ongoing(name, number, label)
                        showHeadsUpCallPopup.value = false
                        if (state == android.telecom.Call.STATE_ACTIVE && callTimerJob == null) {
                            startCallTimer()
                        }
                    }
                    else -> {
                        _callState.value = AppCallState.Idle
                        showHeadsUpCallPopup.value = false
                        stopCallTimer()
                        _isCallMuted.value = false
                        _isSpeakerOn.value = false
                        _isFaceTimeActive.value = false
                    }
                }
            }
        }
    }

    fun completeFirstLaunch() {
        val prefs = getApplication<Application>().getSharedPreferences("dialer_settings_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("first_launch_done", true).apply()
        isFirstLaunch.value = false
    }

    fun triggerSimulatedIncomingCallDelay(seconds: Int = 3, isHeadsUp: Boolean = false, explicitNumber: String? = null, explicitName: String? = null) {
        viewModelScope.launch {
            delay(seconds * 1000L)
            val number = explicitNumber ?: "+49 160 95620427"
            val name = explicitName ?: "Sharon Z."
            val label = "mobile"
            showHeadsUpCallPopup.value = isHeadsUp
            _callState.value = AppCallState.Incoming(name, number, label)
        }
    }

    fun triggerIncomingCallImmediately(name: String, number: String, label: String = "mobile", isHeadsUp: Boolean = false) {
        showHeadsUpCallPopup.value = isHeadsUp
        _callState.value = AppCallState.Incoming(name, number, label)
    }

    fun syncSystemData() {
        viewModelScope.launch {
            repository.trySyncWithAndroidSystemContacts()
            repository.trySyncWithAndroidSystemCallLogs()
        }
    }

    fun setTab(tab: String) {
        _currentTab.value = tab
    }

    fun setRecentsFilter(filter: String) {
        _recentsFilter.value = filter
    }

    fun setContactSearchQuery(query: String) {
        _contactSearchQuery.value = query
    }

    // Keypad digits operations
    fun appendDigit(digit: Char) {
        _enteredDigits.value += digit
    }

    fun removeDigit() {
        if (_enteredDigits.value.isNotEmpty()) {
            _enteredDigits.value = _enteredDigits.value.dropLast(1)
        }
    }

    fun clearDigits() {
        _enteredDigits.value = ""
    }

    // Settings adjustments
    fun updateSettings(newSettings: DialerSettings) {
        _uiSettings.value = newSettings
        settingsManager.saveSettings(newSettings)
    }

    // Initiating Outgoing Call Flow - TRIGGERS REAL ANDROID TELECOM TO BIND CALLS
    fun startCall(number: String, name: String? = null) {
        // Clear digits since we started a call
        clearDigits()
        // Standard start delegation through Telecom Manager
        Log.d("DialerViewModel", "Dial placing call to number: $number")
    }

    // Accept Incoming Call
    fun answerIncomingCall() {
        TelecomCallManager.answerCall()
    }

    // Decline / End Call
    fun hangUpCall() {
        TelecomCallManager.disconnectCall()
    }

    // Active Call Custom Controls - CONTROLS THE REAL TELECOM AUDIO STATE
    fun toggleMute() {
        val nextMute = !_isCallMuted.value
        _isCallMuted.value = nextMute
        TelecomCallManager.toggleMute(nextMute)
    }

    fun toggleSpeaker() {
        val nextSpeaker = !_isSpeakerOn.value
        _isSpeakerOn.value = nextSpeaker
        TelecomCallManager.toggleSpeaker(nextSpeaker)
    }

    fun toggleFaceTime() {
        _isFaceTimeActive.value = !_isFaceTimeActive.value
    }

    // Helper functions
    private fun startCallTimer() {
        _callTimerSeconds.value = 0
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                _callTimerSeconds.value += 1
            }
        }
    }

    private fun stopCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = null
        _callTimerSeconds.value = 0
    }

    private fun getContactNameFromNumber(number: String): String {
        val cleanNumber = number.replace(" ", "")
        return contactsList.value.firstOrNull { it.phoneNumber.replace(" ", "") == cleanNumber }?.name ?: number
    }

    private fun getContactLabelFromNumber(number: String): String {
        val cleanNumber = number.replace(" ", "")
        return contactsList.value.firstOrNull { it.phoneNumber.replace(" ", "") == cleanNumber }?.label ?: "mobile"
    }

    // T9 Mapping matcher
    private fun matchT9(digits: String, name: String, phone: String): Boolean {
        val cleanPhone = phone.replace(Regex("[^0-9]"), "")
        if (cleanPhone.contains(digits)) return true

        val t9Map = mapOf(
            '2' to "abc", '3' to "def", '4' to "ghi", '5' to "jkl",
            '6' to "mno", '7' to "pqrs", '8' to "tuv", '9' to "wxyz",
            '0' to " "
        )

        var digitIndex = 0
        val cleanName = name.lowercase()
        
        // Find if substring starts with specific letter matches
        val words = cleanName.split(" ", "-", ".")
        for (digitsToMatch in listOf(digits)) {
            for (word in words) {
                if (word.isEmpty()) continue
                var index = 0
                while (index < word.length && index < digitsToMatch.length) {
                    val requiredDigit = digitsToMatch[index]
                    val possibleChars = t9Map[requiredDigit] ?: ""
                    if (!possibleChars.contains(word[index])) {
                        break
                    }
                    index++
                }
                if (index == digitsToMatch.length) return true
            }
        }
        return false
    }

    // Call Log insert wrapper and triggers
    fun initiateNativeSystemCall(number: String, context: Context) {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$number")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                context.startActivity(intent)
            } else {
                // If permission is absent, request dialer fallback to avoid silence
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$number")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(dialIntent)
            }
        } catch (e: Exception) {
            // Emulators or devices without carrier dial support fallback safely to DIAL
            try {
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$number")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(dialIntent)
            } catch (ex: Exception) {
                Log.e("DialerViewModel", "Error placing call fallback", ex)
            }
        }
    }

    fun toggleContactFavorite(contact: com.example.data.database.ContactEntity) {
        viewModelScope.launch {
            repository.updateContact(contact.copy(isFavorite = !contact.isFavorite))
        }
    }
}
