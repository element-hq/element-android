/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.labs

import android.os.Build
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.SwitchPreference
import com.airbnb.mvrx.fragmentViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.preference.VectorSwitchPreference
import im.vector.app.features.MainActivity
import im.vector.app.features.MainActivityArgs
import im.vector.app.features.VectorFeatures
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.home.room.threads.ThreadsManager
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.features.settings.VectorSettingsBaseFragment
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.settings.LightweightSettingsStorage
import javax.inject.Inject

@AndroidEntryPoint
class VectorSettingsLabsFragment :
        VectorSettingsBaseFragment() {

    private val viewModel: VectorSettingsLabsViewModel by fragmentViewModel()

    @Inject lateinit var vectorPreferences: VectorPreferences
    @Inject lateinit var lightweightSettingsStorage: LightweightSettingsStorage
    @Inject lateinit var threadsManager: ThreadsManager
    @Inject lateinit var vectorFeatures: VectorFeatures

    override var titleRes = CommonStrings.room_settings_labs_pref_title
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
            if (session.cryptoService().supportsShareKeysOnInvite()) {
                // ensure correct default
                pref.isChecked = session.cryptoService().isShareKeysOnInviteEnabled()

                pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    session.cryptoService().enableShareKeyOnInvite(pref.isChecked)
                    MainActivity.restartApp(requireActivity(), MainActivityArgs(clearCache = true))
                    true
                }
            } else {
                pref.isEnabled = false
                pref.isChecked = false
            }
        }

        findPreference<VectorSwitchPreference>(VectorPreferences.SETTINGS_LABS_NEW_APP_LAYOUT_KEY)?.let { pref ->
            pref.isVisible = vectorFeatures.isNewAppLayoutFeatureEnabled()

            pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                onNewLayoutPreferenceClicked()
                true
            }
        }

        findPreference<VectorSwitchPreference>(VectorPreferences.SETTINGS_LABS_VOICE_BROADCAST_KEY)?.let { pref ->
            // Voice Broadcast recording is not available on Android < 10
            pref.isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && vectorFeatures.isVoiceBroadcastEnabled()
        }

        configureUnreadNotificationsAsTabPreference()
        configureEnableClientInfoRecordingPreference()
    }

    private fun configureUnreadNotificationsAsTabPreference() {
        findPreference<VectorSwitchPreference>(VectorPreferences.SETTINGS_LABS_UNREAD_NOTIFICATIONS_AS_TAB)?.let { pref ->
            pref.isVisible = !vectorFeatures.isNewAppLayoutFeatureEnabled()
            pref.isEnabled = !vectorPreferences.isNewAppLayoutEnabled()
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
                        .setTitle(CommonStrings.threads_labs_enable_notice_title)
                        .setMessage(threadsManager.getLabsEnableThreadsMessage())
                        .setCancelable(true)
                        .setNegativeButton(CommonStrings.action_not_now) { _, _ ->
                            vectorSwitchPreference.isChecked = false
                        }
                        .setPositiveButton(CommonStrings.action_try_it_out) { _, _ ->
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
        vectorPreferences.setThreadFlagChangedManually()
        vectorPreferences.setShouldMigrateThreads(!vectorPreferences.areThreadMessagesEnabled())
        lightweightSettingsStorage.setThreadMessagesEnabled(vectorPreferences.areThreadMessagesEnabled())
        displayLoadingView()
        MainActivity.restartApp(requireActivity(), MainActivityArgs(clearCache = true))
    }

    /**
     * Action when new layout preference switch is actually clicked.
     */
    private fun onNewLayoutPreferenceClicked() {
        configureUnreadNotificationsAsTabPreference()
    }

    private fun configureEnableClientInfoRecordingPreference() {
        findPreference<VectorSwitchPreference>(VectorPreferences.SETTINGS_LABS_CLIENT_INFO_RECORDING_KEY)?.onPreferenceChangeListener =
                OnPreferenceChangeListener { _, newValue ->
                    when (newValue as? Boolean) {
                        false -> viewModel.handle(VectorSettingsLabsAction.DeleteRecordedClientInfo)
                        true -> viewModel.handle(VectorSettingsLabsAction.UpdateClientInfo)
                        else -> Unit
                    }
                    true
                }
    }
}
