package com.safemesh.ai.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

enum class FallPhase {
    IDLE,
    IMPACT_DETECTED,
    STILLNESS_CHECK,
    CONFIRMED
}

class FallDetectionManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onFallDetected: (reason: String) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private val _liveMagnitude = MutableStateFlow(9.8f)
    val liveMagnitude: StateFlow<Float> = _liveMagnitude.asStateFlow()

    private val _liveGyroscopeMagnitude = MutableStateFlow(0f)
    val liveGyroscopeMagnitude: StateFlow<Float> = _liveGyroscopeMagnitude.asStateFlow()

    private val _hasAccelerometer = MutableStateFlow(accelerometer != null)
    val hasAccelerometer: StateFlow<Boolean> = _hasAccelerometer.asStateFlow()

    private val _hasGyroscope = MutableStateFlow(gyroscope != null)
    val hasGyroscope: StateFlow<Boolean> = _hasGyroscope.asStateFlow()

    private val _fallPhase = MutableStateFlow(FallPhase.IDLE)
    val fallPhase: StateFlow<FallPhase> = _fallPhase.asStateFlow()

    // Heuristics:
    // 1. High impact spike threshold (> 25 m/s^2, ~2.5g)
    private val impactThreshold = 25.0f
    // 2. Post-impact resting stillness range (8.0 to 11.0 m/s^2 near 1g gravity)
    private val stillnessMin = 8.0f
    private val stillnessMax = 11.0f
    // 3. Stillness duration required (2000 ms)
    private val stillnessDurationMs = 2000L
    // 4. Cooldown window after a detected event to prevent duplicate triggers (15000 ms)
    private val cooldownDurationMs = 15000L

    private var currentPhase = FallPhase.IDLE
    private var verificationJob: Job? = null
    private var isInCooldown = false

    fun startMonitoring() {
        if (_isMonitoring.value || sensorManager == null) return

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        _isMonitoring.value = true
    }

    fun stopMonitoring() {
        if (!_isMonitoring.value) return

        sensorManager?.unregisterListener(this)
        verificationJob?.cancel()
        currentPhase = FallPhase.IDLE
        _fallPhase.value = FallPhase.IDLE
        _isMonitoring.value = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val ax = event.values[0]
                val ay = event.values[1]
                val az = event.values[2]
                val mag = sqrt(ax * ax + ay * ay + az * az)

                if (!mag.isNaN() && mag > 0) {
                    _liveMagnitude.value = mag
                    processMotionVector(mag)
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                val gx = event.values[0]
                val gy = event.values[1]
                val gz = event.values[2]
                val gyroMag = sqrt(gx * gx + gy * gy + gz * gz)
                if (!gyroMag.isNaN()) {
                    _liveGyroscopeMagnitude.value = gyroMag
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op for motion sensors
    }

    private fun processMotionVector(magnitude: Float) {
        if (isInCooldown) return

        when (currentPhase) {
            FallPhase.IDLE -> {
                // Phase 1: Sudden high-impact acceleration spike
                if (magnitude >= impactThreshold) {
                    currentPhase = FallPhase.IMPACT_DETECTED
                    _fallPhase.value = FallPhase.IMPACT_DETECTED

                    verificationJob?.cancel()
                    verificationJob = scope.launch(Dispatchers.Default) {
                        // Allow 1 second for body / phone settling after impact
                        delay(1000)
                        if (currentPhase == FallPhase.IMPACT_DETECTED) {
                            currentPhase = FallPhase.STILLNESS_CHECK
                            _fallPhase.value = FallPhase.STILLNESS_CHECK
                            checkStillnessWindow()
                        }
                    }
                }
            }
            FallPhase.STILLNESS_CHECK -> {
                // If intense movement continues outside stillness range, cancel verification
                if (magnitude < stillnessMin || magnitude > stillnessMax) {
                    resetToIdle()
                }
            }
            FallPhase.IMPACT_DETECTED -> {
                // Transient phase waiting for 1s settling window
            }
            FallPhase.CONFIRMED -> {
                // Handled in cooldown
            }
        }
    }

    private fun checkStillnessWindow() {
        verificationJob?.cancel()
        verificationJob = scope.launch(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < stillnessDurationMs) {
                delay(100)
                val currentMag = _liveMagnitude.value
                if (currentMag < stillnessMin || currentMag > stillnessMax) {
                    resetToIdle()
                    return@launch
                }
            }

            // Sustained stillness verified
            if (currentPhase == FallPhase.STILLNESS_CHECK && !isInCooldown) {
                currentPhase = FallPhase.CONFIRMED
                _fallPhase.value = FallPhase.CONFIRMED
                isInCooldown = true

                onFallDetected("Possible Fall Detected (High impact + stillness)")

                // Enter cooldown to prevent multiple repetitive countdown triggers
                delay(cooldownDurationMs)
                isInCooldown = false
                resetToIdle()
            }
        }
    }

    private fun resetToIdle() {
        verificationJob?.cancel()
        currentPhase = FallPhase.IDLE
        _fallPhase.value = FallPhase.IDLE
    }
}
