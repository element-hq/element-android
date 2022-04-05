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

package im.vector.app.features.rageshake

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.core.content.getSystemService
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.seismic.ShakeDetector
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.hardware.vibrate
import im.vector.app.features.navigation.Navigator
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.features.settings.VectorSettingsActivity
import javax.inject.Inject

class RageShake @Inject constructor(private val activity: FragmentActivity,
                                    private val bugReporter: BugReporter,
                                    private val navigator: Navigator,
                                    private val sessionHolder: ActiveSessionHolder,
                                    private val vectorPreferences: VectorPreferences) : ShakeDetector.Listener {

    private var shakeDetector: ShakeDetector? = null

    private var dialogDisplayed = false

    var interceptor: (() -> Unit)? = null

    fun start() {
        val sensorManager = activity.getSystemService<SensorManager>() ?: return

        shakeDetector = ShakeDetector(this).apply {
            setSensitivity(vectorPreferences.getRageshakeSensitivity())
            start(sensorManager, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        shakeDetector?.stop()
    }

    fun setSensitivity(sensitivity: Int) {
        shakeDetector?.setSensitivity(sensitivity)
    }

    override fun hearShake() {
        val i = interceptor
        if (i != null) {
            vibrate(activity)
            i.invoke()
        } else {
            if (dialogDisplayed) {
                // Filtered!
                return
            }

            vibrate(activity)
            dialogDisplayed = true

            MaterialAlertDialogBuilder(activity)
                    .setMessage(R.string.send_bug_report_alert_message)
                    .setPositiveButton(R.string.yes) { _, _ -> openBugReportScreen() }
                    .also {
                        if (sessionHolder.hasActiveSession()) {
                            it.setNeutralButton(R.string.settings) { _, _ -> openSettings() }
                        } else {
                            it.setNeutralButton(R.string.action_disable) { _, _ -> disableRageShake() }
                        }
                    }
                    .setOnDismissListener { dialogDisplayed = false }
                    .setNegativeButton(R.string.no, null)
                    .show()
        }
    }

    private fun openBugReportScreen() {
        bugReporter.openBugReportScreen(activity)
    }

    private fun openSettings() {
        navigator.openSettings(activity, VectorSettingsActivity.EXTRA_DIRECT_ACCESS_ADVANCED_SETTINGS)
    }

    private fun disableRageShake() {
        vectorPreferences.setRageshakeEnabled(false)
        stop()
    }

    companion object {
        /**
         * Check if the feature is available
         */
        fun isAvailable(context: Context): Boolean {
            return context.getSystemService<SensorManager>()?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
        }
    }
}
