/*
 * Copyright (c) 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.call

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import androidx.core.content.getSystemService
import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import javax.inject.Inject

/**
 * Manages the proximity sensor and turns the screen off when the proximity sensor activates.
 */
class CallProximityManager @Inject constructor(
        context: Context,
        private val stringProvider: StringProvider
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
     * Recommending naming convention for WakeLock tags is "app:tag"
     */
    private fun generateWakeLockTag() = "${stringProvider.getString(R.string.app_name)}:$PROXIMITY_WAKE_LOCK_TAG"

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
