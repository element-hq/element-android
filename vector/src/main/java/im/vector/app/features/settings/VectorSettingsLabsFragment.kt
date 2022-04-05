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

package im.vector.app.features.settings

import android.os.Bundle
import androidx.preference.Preference
import im.vector.app.R
import im.vector.app.core.preference.VectorSwitchPreference
import im.vector.app.features.MainActivity
import im.vector.app.features.MainActivityArgs
import im.vector.app.features.analytics.plan.MobileScreen
import org.matrix.android.sdk.internal.database.lightweight.LightweightSettingsStorage
import javax.inject.Inject

class VectorSettingsLabsFragment @Inject constructor(
        private val vectorPreferences: VectorPreferences,
        private val lightweightSettingsStorage: LightweightSettingsStorage
) : VectorSettingsBaseFragment() {

    override var titleRes = R.string.room_settings_labs_pref_title
    override val preferenceXmlRes = R.xml.vector_settings_labs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analyticsScreenName = MobileScreen.ScreenName.SettingsLabs
    }

    override fun bindPref() {
        findPreference<VectorSwitchPreference>(VectorPreferences.SETTINGS_LABS_AUTO_REPORT_UISI)?.let { pref ->
            // ensure correct default
            pref.isChecked = vectorPreferences.labsAutoReportUISI()
        }

        // clear cache
        findPreference<VectorSwitchPreference>(VectorPreferences.SETTINGS_LABS_ENABLE_THREAD_MESSAGES)?.let {
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                // We should migrate threads only if threads are disabled
                vectorPreferences.setShouldMigrateThreads(!vectorPreferences.areThreadMessagesEnabled())
                lightweightSettingsStorage.setThreadMessagesEnabled(vectorPreferences.areThreadMessagesEnabled())
                displayLoadingView()
                MainActivity.restartApp(requireActivity(), MainActivityArgs(clearCache = true))
                false
            }
        }
    }
}
