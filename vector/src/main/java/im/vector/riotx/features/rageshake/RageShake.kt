/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.riotx.features.rageshake

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.preference.PreferenceManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.squareup.seismic.ShakeDetector
import im.vector.riotx.R
import javax.inject.Inject

class RageShake @Inject constructor(private val activity: AppCompatActivity,
                                    private val bugReporter: BugReporter) : ShakeDetector.Listener {

    private var shakeDetector: ShakeDetector? = null

    private var dialogDisplayed = false

    fun start() {
        if (!isEnable(activity)) {
            return
        }


        val sensorManager = activity.getSystemService(AppCompatActivity.SENSOR_SERVICE) as? SensorManager

        if (sensorManager == null) {
            return
        }

        shakeDetector = ShakeDetector(this).apply {
            start(sensorManager)
        }
    }

    fun stop() {
        shakeDetector?.stop()
    }

    /**
     * Enable the feature, and start it
     */
    fun enable() {
        PreferenceManager.getDefaultSharedPreferences(activity).edit {
            putBoolean(SETTINGS_USE_RAGE_SHAKE_KEY, true)
        }

        start()
    }

    /**
     * Disable the feature, and stop it
     */
    fun disable() {
        PreferenceManager.getDefaultSharedPreferences(activity).edit {
            putBoolean(SETTINGS_USE_RAGE_SHAKE_KEY, false)
        }

        stop()
    }

    override fun hearShake() {
        if (dialogDisplayed) {
            // Filtered!
            return
        }

        dialogDisplayed = true

        AlertDialog.Builder(activity)
                .setMessage(R.string.send_bug_report_alert_message)
                .setPositiveButton(R.string.yes) { _, _ -> openBugReportScreen() }
                .setNeutralButton(R.string.disable) { _, _ -> disable() }
                .setOnDismissListener { dialogDisplayed = false }
                .setNegativeButton(R.string.no, null)
                .show()
    }

    private fun openBugReportScreen() {
        bugReporter.openBugReportScreen(activity)
    }

    companion object {
        private const val SETTINGS_USE_RAGE_SHAKE_KEY = "SETTINGS_USE_RAGE_SHAKE_KEY"

        /**
         * Check if the feature is available
         */
        fun isAvailable(context: Context): Boolean {
            return (context.getSystemService(AppCompatActivity.SENSOR_SERVICE) as? SensorManager)
                    ?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
        }

        /**
         * Check if the feature is enable (enabled by default)
         */
        private fun isEnable(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SETTINGS_USE_RAGE_SHAKE_KEY, true)
        }
    }
}