package com.safemesh.ai.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.safemesh.ai.data.model.EmergencyContact
import com.safemesh.ai.data.model.EmergencyResponse
import com.safemesh.ai.data.model.LocationData
import com.safemesh.ai.data.repository.EmergencyRepository
import com.safemesh.ai.data.sensor.FallDetectionManager
import com.safemesh.ai.data.sensor.FallPhase
import com.safemesh.ai.data.sensor.VoiceDistressManager
import com.safemesh.ai.data.sensor.VoiceState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class SosUiState {
    object Idle : SosUiState()
    data class CountingDown(val secondsRemaining: Int, val reason: String) : SosUiState()
    data class Dispatching(val reason: String, val locationStatus: String) : SosUiState()
    data class Delivered(val response: EmergencyResponse, val locationData: LocationData?) : SosUiState()
    data class Error(val message: String) : SosUiState()
}

class EmergencyViewModel(
    application: Application,
    private val repository: EmergencyRepository
) : AndroidViewModel(application) {

    val contacts: StateFlow<List<EmergencyContact>> = repository.contactsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userName: StateFlow<String> = repository.userNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Shubham Chaubey")

    val backendUrl: StateFlow<String> = repository.backendUrlFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "http://10.0.2.2:3001/")

    // Safety Setting: Simulation-Only mode is enabled by default to prevent real dispatches during dev/testing
    private val _isSimulationOnly = MutableStateFlow(true)
    val isSimulationOnly: StateFlow<Boolean> = _isSimulationOnly.asStateFlow()

    private val _sosState = MutableStateFlow<SosUiState>(SosUiState.Idle)
    val sosState: StateFlow<SosUiState> = _sosState.asStateFlow()

    // Hardware Sensor Manager for Fall Detection
    val fallDetectionManager = FallDetectionManager(
        context = application.applicationContext,
        scope = viewModelScope,
        onFallDetected = { reason ->
            // Only trigger countdown if not already active
            if (_sosState.value is SosUiState.Idle) {
                startSosCountdown(reason)
            }
        }
    )

    // Voice Distress Recognition Manager
    val voiceDistressManager = VoiceDistressManager(
        context = application.applicationContext,
        onDistressDetected = { reason ->
            // Only trigger countdown if not already active
            if (_sosState.value is SosUiState.Idle) {
                startSosCountdown(reason)
            }
        }
    )

    val isSensorMonitoring: StateFlow<Boolean> = fallDetectionManager.isMonitoring
    val liveMagnitude: StateFlow<Float> = fallDetectionManager.liveMagnitude
    val liveGyroscopeMagnitude: StateFlow<Float> = fallDetectionManager.liveGyroscopeMagnitude
    val hasAccelerometer: StateFlow<Boolean> = fallDetectionManager.hasAccelerometer
    val hasGyroscope: StateFlow<Boolean> = fallDetectionManager.hasGyroscope
    val fallPhase: StateFlow<FallPhase> = fallDetectionManager.fallPhase

    // Voice State Flows
    val voiceState: StateFlow<VoiceState> = voiceDistressManager.voiceState
    val isVoiceSupported: StateFlow<Boolean> = voiceDistressManager.isVoiceSupported
    val lastDetectedPhrase: StateFlow<String> = voiceDistressManager.lastDetectedPhrase

    private var countdownJob: Job? = null
    private var lastCapturedLocation: LocationData? = null

    fun setSimulationOnlyMode(enabled: Boolean) {
        _isSimulationOnly.value = enabled
    }

    fun toggleSensorMonitoring(enable: Boolean) {
        if (enable) {
            fallDetectionManager.startMonitoring()
        } else {
            fallDetectionManager.stopMonitoring()
        }
    }

    fun toggleVoiceListening(enable: Boolean) {
        if (enable) {
            voiceDistressManager.startListening()
        } else {
            voiceDistressManager.stopListening()
        }
    }

    fun startSosCountdown(reason: String = "Manual SOS Trigger") {
        countdownJob?.cancel()
        _sosState.value = SosUiState.CountingDown(secondsRemaining = 10, reason = reason)

        countdownJob = viewModelScope.launch {
            for (sec in 9 downTo 0) {
                delay(1000)
                val current = _sosState.value
                if (current is SosUiState.CountingDown) {
                    if (sec > 0) {
                        _sosState.value = current.copy(secondsRemaining = sec)
                    } else {
                        // Countdown completed -> Trigger Dispatch (or Simulated dispatch if simulation mode)
                        executeDispatch(reason)
                        break
                    }
                } else {
                    break
                }
            }
        }
    }

    fun cancelSos() {
        countdownJob?.cancel()
        _sosState.value = SosUiState.Idle
    }

    fun sendNow(reason: String) {
        countdownJob?.cancel()
        executeDispatch(reason)
    }

    private fun executeDispatch(reason: String) {
        viewModelScope.launch {
            _sosState.value = SosUiState.Dispatching(reason = reason, locationStatus = "Acquiring GPS location...")

            // 1. Get GPS Location
            val locationResult = repository.getLocation()
            lastCapturedLocation = locationResult.getOrNull()

            // Check if in Simulation-Only Mode
            if (_isSimulationOnly.value) {
                _sosState.value = SosUiState.Dispatching(
                    reason = reason,
                    locationStatus = "[Simulation-Only Mode] GPS acquired. Simulating dispatch..."
                )
                delay(1200)

                // Simulated response - NO network request, NO real calls/SMS sent
                val simulatedResponse = EmergencyResponse(
                    success = true,
                    simulated = true,
                    smsSent = contacts.value.size,
                    callsSent = contacts.value.size,
                    location = lastCapturedLocation != null,
                    locationUrl = lastCapturedLocation?.let { "https://www.google.com/maps?q=${it.latitude},${it.longitude}" },
                    results = emptyList()
                )

                _sosState.value = SosUiState.Delivered(
                    response = simulatedResponse,
                    locationData = lastCapturedLocation
                )
                return@launch
            }

            // Real dispatch mode (only when user explicitly disabled Simulation-Only mode)
            _sosState.value = SosUiState.Dispatching(
                reason = reason,
                locationStatus = if (lastCapturedLocation != null) "Location acquired. Contacting server..." else "Location unavailable. Contacting server..."
            )

            val dispatchResult = repository.dispatchEmergency(
                reason = reason,
                location = lastCapturedLocation
            )

            dispatchResult.fold(
                onSuccess = { response ->
                    _sosState.value = SosUiState.Delivered(
                        response = response,
                        locationData = lastCapturedLocation
                    )
                },
                onFailure = { error ->
                    _sosState.value = SosUiState.Error(
                        message = error.localizedMessage ?: "Emergency dispatch failed"
                    )
                }
            )
        }
    }

    fun resetSosState() {
        countdownJob?.cancel()
        _sosState.value = SosUiState.Idle
    }

    fun addContact(name: String, phone: String, relation: String) {
        viewModelScope.launch {
            val newContact = EmergencyContact(
                id = System.currentTimeMillis().toString(),
                name = name,
                phone = phone,
                relation = relation.ifBlank { "Contact" }
            )
            repository.addContact(newContact)
        }
    }

    fun removeContact(id: String) {
        viewModelScope.launch {
            repository.removeContact(id)
        }
    }

    fun updateUserName(name: String) {
        viewModelScope.launch {
            repository.updateUserName(name)
        }
    }

    fun updateBackendUrl(url: String) {
        viewModelScope.launch {
            repository.updateBackendUrl(url)
        }
    }

    override fun onCleared() {
        super.onCleared()
        fallDetectionManager.stopMonitoring()
        voiceDistressManager.stopListening()
    }
}

class EmergencyViewModelFactory(
    private val application: Application,
    private val repository: EmergencyRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EmergencyViewModel::class.java)) {
            return EmergencyViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
