/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import androidx.core.content.getSystemService
import im.vector.app.core.resources.BuildMeta
import javax.inject.Inject

/**
 * Manages the proximity sensor and turns the screen off when the proximity sensor activates.
 */
class CallProximityManager @Inject constructor(
        context: Context,
        private val buildMeta: BuildMeta,
) : SensorEventListener {

    companion object {
        private const val PROXIMITY_WAKE_LOCK_TAG = "PROXIMITY_WAKE_LOCK_TAG"

        // 1 hour
        private const val WAKE_LOCK_TIMEOUT_MILLIS = 3_600_000L
    }

    private val powerManager = context.getSystemService<PowerManager>()!!
    private val sensorManager = context.getSystemService<SensorManager>()!!

    private var wakeLock: PowerManager.WakeLock? = null
    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private val isSupported = sensor != null && powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)

    /**
     * Start listening the proximity sensor. [stop] function should be called to release the sensor and the WakeLock.
     */
    fun start() {
        if (isSupported) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    /**
     * Stop listening proximity sensor changes and release the WakeLock.
     */
    fun stop() {
        if (isSupported) {
            sensorManager.unregisterListener(this)
            wakeLock
                    ?.takeIf { it.isHeld }
                    ?.release()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // NOOP
    }

    override fun onSensorChanged(event: SensorEvent) {
        val distanceInCentimeters = event.values[0]
        if (distanceInCentimeters < sensor?.maximumRange ?: 20f) {
            onProximityNear()
        } else {
            onProximityFar()
        }
    }

    /**
     * Recommending naming convention for WakeLock tags is "app:tag".
     */
    private fun generateWakeLockTag() = "${buildMeta.applicationName}:$PROXIMITY_WAKE_LOCK_TAG"

    private fun onProximityNear() {
        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, generateWakeLockTag())
        }
        wakeLock
                ?.takeIf { !it.isHeld }
                ?.acquire(WAKE_LOCK_TIMEOUT_MILLIS)
    }

    private fun onProximityFar() {
        wakeLock
                ?.takeIf { it.isHeld }
                ?.release()
    }
}
