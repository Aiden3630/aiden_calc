package com.aiden.calculator

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class EmergencyLockPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("privacy", Context.MODE_PRIVATE)

    var enabled by mutableStateOf(preferences.getBoolean(KEY_ENABLED, true))
        private set

    fun updateEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply()
        this.enabled = enabled
    }

    private companion object {
        const val KEY_ENABLED = "emergencyLock"
    }
}

internal class FaceDownDetector(
    private val debounceMs: Long = 400,
    private val now: () -> Long,
) {
    private var faceDownSince: Long? = null
    private var triggered = false

    fun update(x: Float, y: Float, z: Float): Boolean {
        val magnitudeSquared = x * x + y * y + z * z
        val stableFaceDown = z < -7.5f && magnitudeSquared in 70f..125f
        if (!stableFaceDown) {
            faceDownSince = null
            triggered = false
            return false
        }
        val started = faceDownSince ?: now().also { faceDownSince = it }
        if (!triggered && now() - started >= debounceMs) {
            triggered = true
            return true
        }
        return false
    }
}

class FaceDownLockController(
    context: Context,
    private val lock: () -> Unit,
) : SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val detector = FaceDownDetector(now = SystemClock::elapsedRealtime)
    private var listening = false

    fun isSupported() = accelerometer != null

    fun updateListening(active: Boolean) {
        if (active == listening || accelerometer == null) return
        listening = active
        if (active) sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        else sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER &&
            detector.update(event.values[0], event.values[1], event.values[2])
        ) lock()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
