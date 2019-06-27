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

package im.vector.riotredesign.features.settings

import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreference
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.MatrixCallback
import im.vector.riotredesign.R
import im.vector.riotredesign.core.preference.BingRule
import im.vector.riotredesign.core.pushers.PushersManager
import im.vector.riotredesign.push.fcm.FcmHelper
import org.koin.android.ext.android.inject

// Referenced in vector_settings_preferences_root.xml
class VectorSettingsNotificationPreferenceFragment : VectorSettingsBaseFragment() {

    override var titleRes: Int = R.string.settings_notifications
    override val preferenceXmlRes = R.xml.vector_settings_notifications

    val pushManager: PushersManager by inject()

    // background sync category
    private val mSyncRequestTimeoutPreference by lazy {
        // ? Cause it can be removed
        findPreference(PreferencesManager.SETTINGS_SET_SYNC_TIMEOUT_PREFERENCE_KEY) as EditTextPreference?
    }
    private val mSyncRequestDelayPreference by lazy {
        // ? Cause it can be removed
        findPreference(PreferencesManager.SETTINGS_SET_SYNC_DELAY_PREFERENCE_KEY) as EditTextPreference?
    }

    private val backgroundSyncCategory by lazy {
        findPreference(PreferencesManager.SETTINGS_BACKGROUND_SYNC_PREFERENCE_KEY)
    }
    private val backgroundSyncDivider by lazy {
        findPreference(PreferencesManager.SETTINGS_BACKGROUND_SYNC_DIVIDER_PREFERENCE_KEY)
    }
    private val backgroundSyncPreference by lazy {
        findPreference(PreferencesManager.SETTINGS_ENABLE_BACKGROUND_SYNC_PREFERENCE_KEY) as SwitchPreference
    }

    private val notificationsSettingsCategory by lazy {
        findPreference(PreferencesManager.SETTINGS_NOTIFICATIONS_KEY) as PreferenceCategory
    }

    override fun bindPref() {
        for (preferenceKey in mPrefKeyToBingRuleId.keys) {
            val preference = findPreference(preferenceKey)

            if (null != preference) {
                if (preference is SwitchPreference) {
                    preference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValueAsVoid ->
                        // on some old android APIs,
                        // the callback is called even if there is no user interaction
                        // so the value will be checked to ensure there is really no update.
                        // TODO onPushRuleClick(preference.key, newValueAsVoid as Boolean)
                        true
                    }
                }
            }
        }
    }


    override fun onResume() {
        super.onResume()
        Matrix.getInstance().currentSession?.refreshPushers()
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        if (preference?.key == PreferencesManager.SETTINGS_ENABLE_THIS_DEVICE_PREFERENCE_KEY) {
            val switchPref = preference as SwitchPreference
            if (switchPref.isChecked) {
                FcmHelper.getFcmToken(requireContext())?.let {
                    if (PreferencesManager.areNotificationEnabledForDevice(requireContext())) {
                        pushManager.registerPusherWithFcmKey(it)
                    }
                }
            } else {
                FcmHelper.getFcmToken(requireContext())?.let {
                    pushManager.unregisterPusher(it, object : MatrixCallback<Unit> {
                        override fun onSuccess(data: Unit) {
                            super.onSuccess(data)
                        }

                        override fun onFailure(failure: Throwable) {
                            super.onFailure(failure)
                        }
                    })
                }
            }
        }
        return super.onPreferenceTreeClick(preference)
    }

    /**
     * Update a push rule.
     */
    private fun onPushRuleClick(preferenceKey: String, newValue: Boolean) {
        notImplemented()
        /* TODO
        val matrixInstance = Matrix.getInstance(context)
        val pushManager = matrixInstance.pushManager

        Timber.v("onPushRuleClick $preferenceKey : set to $newValue")

        when (preferenceKey) {

            PreferencesManager.SETTINGS_TURN_SCREEN_ON_PREFERENCE_KEY -> {
                if (pushManager.isScreenTurnedOn != newValue) {
                    pushManager.isScreenTurnedOn = newValue
                }
            }

            PreferencesManager.SETTINGS_ENABLE_THIS_DEVICE_PREFERENCE_KEY -> {
                val isConnected = matrixInstance.isConnected
                val isAllowed = pushManager.areDeviceNotificationsAllowed()

                // avoid useless update
                if (isAllowed == newValue) {
                    return
                }

                pushManager.setDeviceNotificationsAllowed(!isAllowed)

                // when using FCM
                // need to register on servers
                if (isConnected && pushManager.useFcm() && (pushManager.isServerRegistered || pushManager.isServerUnRegistered)) {
                    val listener = object : MatrixCallback<Unit> {

                        private fun onDone() {
                            activity?.runOnUiThread {
                                hideLoadingView(true)
                                refreshPushersList()
                            }
                        }

                        override fun onSuccess(info: Void?) {
                            onDone()
                        }

                        override fun onMatrixError(e: MatrixError?) {
                            // Set again the previous state
                            pushManager.setDeviceNotificationsAllowed(isAllowed)
                            onDone()
                        }

                        override fun onNetworkError(e: java.lang.Exception?) {
                            // Set again the previous state
                            pushManager.setDeviceNotificationsAllowed(isAllowed)
                            onDone()
                        }

                        override fun onUnexpectedError(e: java.lang.Exception?) {
                            // Set again the previous state
                            pushManager.setDeviceNotificationsAllowed(isAllowed)
                            onDone()
                        }
                    }

                    displayLoadingView()
                    if (pushManager.isServerRegistered) {
                        pushManager.unregister(listener)
                    } else {
                        pushManager.register(listener)
                    }
                }
            }

            // check if there is an update

            // on some old android APIs,
            // the callback is called even if there is no user interaction
            // so the value will be checked to ensure there is really no update.
            else -> {

                val ruleId = mPrefKeyToBingRuleId[preferenceKey]
                val rule = session.dataHandler.pushRules()?.findDefaultRule(ruleId)

                // check if there is an update
                var curValue = null != rule && rule.isEnabled

                if (TextUtils.equals(ruleId, BingRule.RULE_ID_DISABLE_ALL) || TextUtils.equals(ruleId, BingRule.RULE_ID_SUPPRESS_BOTS_NOTIFICATIONS)) {
                    curValue = !curValue
                }

                // on some old android APIs,
                // the callback is called even if there is no user interaction
                // so the value will be checked to ensure there is really no update.
                if (newValue == curValue) {
                    return
                }

                if (null != rule) {
                    displayLoadingView()
                    session.dataHandler.bingRulesManager.updateEnableRuleStatus(rule, !rule.isEnabled, object : BingRulesManager.onBingRuleUpdateListener {
                        private fun onDone() {
                            refreshDisplay()
                            hideLoadingView()
                        }

                        override fun onBingRuleUpdateSuccess() {
                            onDone()
                        }

                        override fun onBingRuleUpdateFailure(errorMessage: String) {
                            activity?.toast(errorMessage)
                            onDone()
                        }
                    })
                }
            }
        }
        */
    }

    /**
     * Refresh the background sync preference
     */
    private fun refreshBackgroundSyncPrefs() {
        /* TODO
        activity?.let { activity ->
            val pushManager = Matrix.getInstance(activity).pushManager

            val timeout = pushManager.backgroundSyncTimeOut / 1000
            val delay = pushManager.backgroundSyncDelay / 1000

            // update the settings
            PreferenceManager.getDefaultSharedPreferences(activity).edit {
                putString(PreferencesManager.SETTINGS_SET_SYNC_TIMEOUT_PREFERENCE_KEY, timeout.toString() + "")
                putString(PreferencesManager.SETTINGS_SET_SYNC_DELAY_PREFERENCE_KEY, delay.toString() + "")
            }

            mSyncRequestTimeoutPreference?.let {
                it.summary = secondsToText(timeout)
                it.text = timeout.toString() + ""

                it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    var newTimeOut = timeout

                    try {
                        newTimeOut = Integer.parseInt(newValue as String)
                    } catch (e: Exception) {
                        Timber.e(e, "## refreshBackgroundSyncPrefs : parseInt failed " + e.message)
                    }

                    if (newTimeOut != timeout) {
                        pushManager.backgroundSyncTimeOut = newTimeOut * 1000

                        activity.runOnUiThread { refreshBackgroundSyncPrefs() }
                    }

                    false
                }
            }

            mSyncRequestDelayPreference?.let {
                it.summary = secondsToText(delay)
                it.text = delay.toString() + ""

                it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    var newDelay = delay

                    try {
                        newDelay = Integer.parseInt(newValue as String)
                    } catch (e: Exception) {
                        Timber.e(e, "## refreshBackgroundSyncPrefs : parseInt failed " + e.message)
                    }

                    if (newDelay != delay) {
                        pushManager.backgroundSyncDelay = newDelay * 1000

                        activity.runOnUiThread { refreshBackgroundSyncPrefs() }
                    }

                    false
                }
            }
        }
        */
    }

    companion object {
        private const val DUMMY_RULE = "DUMMY_RULE"

        // preference name <-> rule Id
        private var mPrefKeyToBingRuleId = mapOf(
                PreferencesManager.SETTINGS_ENABLE_ALL_NOTIF_PREFERENCE_KEY to BingRule.RULE_ID_DISABLE_ALL,
                PreferencesManager.SETTINGS_ENABLE_THIS_DEVICE_PREFERENCE_KEY to DUMMY_RULE,
                PreferencesManager.SETTINGS_TURN_SCREEN_ON_PREFERENCE_KEY to DUMMY_RULE
        )
    }

}