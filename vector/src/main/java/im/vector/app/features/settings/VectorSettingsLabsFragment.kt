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
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.preference.VectorSwitchPreference
import im.vector.app.features.MainActivity
import im.vector.app.features.MainActivityArgs
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.home.room.threads.ThreadsManager
import org.matrix.android.sdk.api.settings.LightweightSettingsStorage
import javax.inject.Inject

@AndroidEntryPoint
class VectorSettingsLabsFragment :
        VectorSettingsBaseFragment() {

    @Inject lateinit var vectorPreferences: VectorPreferences
    @Inject lateinit var lightweightSettingsStorage: LightweightSettingsStorage
    @Inject lateinit var threadsManager: ThreadsManager

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
        findPreference<VectorSwitchPreference>(VectorPreferences.SETTINGS_LABS_ENABLE_THREAD_MESSAGES)?.let { vectorPref ->
            vectorPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                onThreadsPreferenceClickedInterceptor(vectorPref)
                false
            }
        }

        findPreference<SwitchPreference>(VectorPreferences.SETTINGS_LABS_MSC3061_SHARE_KEYS_HISTORY)?.let { pref ->
            // ensure correct default
            pref.isChecked = session.cryptoService().isShareKeysOnInviteEnabled()

            pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                session.cryptoService().enableShareKeyOnInvite(pref.isChecked)
                MainActivity.restartApp(requireActivity(), MainActivityArgs(clearCache = true))
                true
            }
        }
    }

    /**
     * Intercept the click to display a user friendly dialog when their homeserver do not support threads.
     */
    private fun onThreadsPreferenceClickedInterceptor(vectorSwitchPreference: VectorSwitchPreference) {
        val userEnabledThreads = vectorPreferences.areThreadMessagesEnabled()
        if (!session.homeServerCapabilitiesService().getHomeServerCapabilities().canUseThreading && userEnabledThreads) {
            activity?.let {
                MaterialAlertDialogBuilder(it)
                        .setTitle(R.string.threads_labs_enable_notice_title)
                        .setMessage(threadsManager.getLabsEnableThreadsMessage())
                        .setCancelable(true)
                        .setNegativeButton(R.string.action_not_now) { _, _ ->
                            vectorSwitchPreference.isChecked = false
                        }
                        .setPositiveButton(R.string.action_try_it_out) { _, _ ->
                            onThreadsPreferenceClicked()
                        }
                        .show()
                        ?.findViewById<TextView>(android.R.id.message)
                        ?.apply {
                            linksClickable = true
                            movementMethod = LinkMovementMethod.getInstance()
                        }
            }
        } else {
            onThreadsPreferenceClicked()
        }
    }

    /**
     * Action when threads preference switch is actually clicked.
     */
    private fun onThreadsPreferenceClicked() {
        // We should migrate threads only if threads are disabled
        vectorPreferences.setShouldMigrateThreads(!vectorPreferences.areThreadMessagesEnabled())
        lightweightSettingsStorage.setThreadMessagesEnabled(vectorPreferences.areThreadMessagesEnabled())
        displayLoadingView()
        MainActivity.restartApp(requireActivity(), MainActivityArgs(clearCache = true))
    }
}
