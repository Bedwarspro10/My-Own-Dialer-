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
    private var transcriptJob: Job? = null

    init {
        // Prepare default data
        viewModelScope.launch {
            repository.prepopulateDefaultDataIfNeeded()
            syncSystemData()
        }
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

    // Initiating Outgoing Call Flow
    fun startCall(number: String, name: String? = null) {
        val finalName = name ?: getContactNameFromNumber(number)
        val finalLabel = getContactLabelFromNumber(number)
        
        // Log the outgoing call
        viewModelScope.launch {
            repository.insertCallLog(
                CallLogEntity(
                    callerName = name,
                    phoneNumber = number,
                    callType = "OUTGOING",
                    timestamp = System.currentTimeMillis(),
                    durationSeconds = 0,
                    label = finalLabel
                )
            )
        }

        // Switch call state
        _callState.value = AppCallState.Ongoing(finalName, number, finalLabel)
        startCallTimer()
        startLiveTranscripts()
    }

    // Simulate Receiving Incoming Call with 3 second delay to showcase UI
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

    // Accept Incoming Call
    fun answerIncomingCall() {
        val currentState = _callState.value
        if (currentState is AppCallState.Incoming) {
            // Update to Ongoing call
            _callState.value = AppCallState.Ongoing(currentState.name, currentState.number, currentState.label)
            startCallTimer()
            startLiveTranscripts()

            // Save incoming call history
            viewModelScope.launch {
                repository.insertCallLog(
                    CallLogEntity(
                        callerName = currentState.name,
                        phoneNumber = currentState.number,
                        callType = "INCOMING",
                        timestamp = System.currentTimeMillis(),
                        durationSeconds = 0,
                        label = currentState.label
                    )
                )
            }
        }
    }

    // Decline / End Call
    fun hangUpCall() {
        val duration = _callTimerSeconds.value
        val state = _callState.value
        
        // If ended an incoming unanswered call, log as MISSED
        if (state is AppCallState.Incoming) {
            viewModelScope.launch {
                repository.insertCallLog(
                    CallLogEntity(
                        callerName = state.name,
                        phoneNumber = state.number,
                        callType = "MISSED",
                        timestamp = System.currentTimeMillis(),
                        durationSeconds = 0,
                        label = state.label
                    )
                )
            }
        } else if (state is AppCallState.Ongoing && duration > 0) {
            // Update last log with correct duration
            viewModelScope.launch {
                val latest = repository.allCallLogs.first().firstOrNull { 
                    it.phoneNumber == state.number && it.durationSeconds == 0 
                }
                if (latest != null) {
                    repository.insertCallLog(latest.copy(durationSeconds = duration))
                }
            }
        }

        stopCallTimer()
        stopLiveTranscripts()
        _callState.value = AppCallState.Idle
        showHeadsUpCallPopup.value = false
        _isCallMuted.value = false
        _isSpeakerOn.value = false
        _isFaceTimeActive.value = false
    }

    // Active Call Custom Controls
    fun toggleMute() {
        _isCallMuted.value = !_isCallMuted.value
    }

    fun toggleSpeaker() {
        _isSpeakerOn.value = !_isSpeakerOn.value
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
        _callTimerSeconds.value = 0
    }

    private fun startLiveTranscripts() {
        _translationLogs.value = listOf(
            "SYSTEM" to "Translation is on for this call"
        )
        val quotes = listOf(
            "Hallo, sind Sie für eine Hochzeit am 6. Dezember verfügbar?" to "Hi, are you available to cater a wedding on December 6?",
            "Ja, an diesem Wochenende sind noch Termine frei." to "Yes, there are still dates available that weekend.",
            "Wie viele Gäste erwarten Sie im Durchschnitt?" to "How many guests are you expecting on average?",
            "Wir planen mit etwa 120 Personen für das Abendessen." to "We are planning for about 120 people for dinner.",
            "Toll! Ich sende Ihnen unsere Menüvorschläge per E-Mail." to "Great! I will email you our menu proposals."
        )

        transcriptJob?.cancel()
        transcriptJob = viewModelScope.launch {
            var counter = 0
            while (counter < quotes.size) {
                delay(8000L)
                val newLogs = _translationLogs.value.toMutableList()
                newLogs.add(quotes[counter])
                _translationLogs.value = newLogs
                counter++
            }
        }
    }

    private fun stopLiveTranscripts() {
        transcriptJob?.cancel()
        _translationLogs.value = emptyList()
    }

    private fun getContactNameFromNumber(number: String): String {
        return contactsList.value.firstOrNull { it.phoneNumber.replace(" ", "") == number.replace(" ", "") }?.name ?: number
    }

    private fun getContactLabelFromNumber(number: String): String {
        return contactsList.value.firstOrNull { it.phoneNumber.replace(" ", "") == number.replace(" ", "") }?.label ?: "mobile"
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
                // Log outgoing
                startCall(number)
            } else {
                // If permission is absent, we simulate premium ongoing glass call directly inside our app!
                startCall(number)
            }
        } catch (e: Exception) {
            // Emulators or devices without sim fallback to in-app simulation
            startCall(number)
        }
    }
}
