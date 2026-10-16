package com.safemesh.ai.data.sensor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoiceState {
    OFF,
    LISTENING,
    PERMISSION_REQUIRED,
    UNAVAILABLE,
    ERROR_RETRYING
}

class VoiceDistressManager(
    private val context: Context,
    private val onDistressDetected: (keyword: String) -> Unit
) : RecognitionListener {

    private val _voiceState = MutableStateFlow(VoiceState.OFF)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _isVoiceSupported = MutableStateFlow(SpeechRecognizer.isRecognitionAvailable(context))
    val isVoiceSupported: StateFlow<Boolean> = _isVoiceSupported.asStateFlow()

    private val _lastDetectedPhrase = MutableStateFlow("")
    val lastDetectedPhrase: StateFlow<String> = _lastDetectedPhrase.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var isRequestedActive = false
    private var isTriggerCooldown = false
    private var restartAttempts = 0
    private val maxConsecutiveErrors = 4

    private val mainHandler = Handler(Looper.getMainLooper())

    // Distress keywords in English and Hindi / Hinglish
    private val distressKeywords = setOf(
        "help",
        "help me",
        "emergency",
        "save me",
        "save",
        "bachao",
        "bachaao",
        "madad",
        "madat",
        "danger",
        "khatra",
        "police",
        "बचाओ",
        "मदद"
    )

    fun startListening() {
        if (!_isVoiceSupported.value) {
            _voiceState.value = VoiceState.UNAVAILABLE
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _voiceState.value = VoiceState.PERMISSION_REQUIRED
            return
        }

        isRequestedActive = true
        restartAttempts = 0
        mainHandler.post {
            initiateRecognition()
        }
    }

    fun stopListening() {
        isRequestedActive = false
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                // Ignore cleanup errors
            } finally {
                speechRecognizer = null
                _voiceState.value = VoiceState.OFF
            }
        }
    }

    private fun initiateRecognition() {
        if (!isRequestedActive) return

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@VoiceDistressManager)
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            speechRecognizer?.startListening(intent)
            _voiceState.value = VoiceState.LISTENING
        } catch (e: Exception) {
            _voiceState.value = VoiceState.UNAVAILABLE
        }
    }

    private fun scheduleRestart(delayMs: Long = 1000L) {
        if (!isRequestedActive) return

        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (isRequestedActive) {
                initiateRecognition()
            }
        }, delayMs)
    }

    override fun onResults(results: Bundle?) {
        processSpeechBundle(results)
        // Automatically restart speech recognizer session safely
        if (isRequestedActive) {
            scheduleRestart(500L)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        processSpeechBundle(partialResults)
    }

    private fun processSpeechBundle(bundle: Bundle?) {
        if (bundle == null || isTriggerCooldown) return

        val matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
        for (rawText in matches) {
            val normalized = normalizeText(rawText)
            for (keyword in distressKeywords) {
                // Check if spoken phrase contains distress keyword as whole word or phrase
                if (normalized.contains(keyword) || normalized.split("\\s+".toRegex()).contains(keyword)) {
                    triggerDistress(keyword)
                    return
                }
            }
        }
    }

    private fun normalizeText(input: String): String {
        return input.lowercase(Locale.ROOT)
            .replace("[^a-zA-Z0-9\\s\u0900-\u097F]".toRegex(), " ")
            .trim()
    }

    private fun triggerDistress(keyword: String) {
        if (isTriggerCooldown) return
        isTriggerCooldown = true
        _lastDetectedPhrase.value = keyword

        onDistressDetected("Voice Distress Detected (Heard \"$keyword\")")

        // 15-second cooldown to avoid duplicate SOS countdown loops
        mainHandler.postDelayed({
            isTriggerCooldown = false
        }, 15000L)
    }

    override fun onError(error: Int) {
        if (!isRequestedActive) return

        when (error) {
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                // Normal pauses in speech: restart after a brief pause
                restartAttempts = 0
                scheduleRestart(500L)
            }
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                _voiceState.value = VoiceState.PERMISSION_REQUIRED
                stopListening()
            }
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                scheduleRestart(1000L)
            }
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                restartAttempts++
                if (restartAttempts > maxConsecutiveErrors) {
                    _voiceState.value = VoiceState.ERROR_RETRYING
                    // Back off with longer delay on continuous network errors to prevent tight loops
                    scheduleRestart(5000L)
                } else {
                    scheduleRestart(1500L)
                }
            }
            else -> {
                restartAttempts++
                if (restartAttempts > maxConsecutiveErrors) {
                    _voiceState.value = VoiceState.ERROR_RETRYING
                    scheduleRestart(4000L)
                } else {
                    scheduleRestart(1000L)
                }
            }
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {
        if (isRequestedActive) {
            _voiceState.value = VoiceState.LISTENING
            restartAttempts = 0
        }
    }

    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onEvent(eventType: Int, params: Bundle?) {}
}
