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

import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.pushrules.RuleIds
import im.vector.riotredesign.R
import im.vector.riotredesign.core.pushers.PushersManager
import im.vector.riotredesign.push.fcm.FcmHelper
import org.koin.android.ext.android.inject

// Referenced in vector_settings_preferences_root.xml
class VectorSettingsNotificationPreferenceFragment : VectorSettingsBaseFragment() {

    override var titleRes: Int = R.string.settings_notifications
    override val preferenceXmlRes = R.xml.vector_settings_notifications

    val pushManager: PushersManager by inject()

    override fun bindPref() {
        findPreference(PreferencesManager.SETTINGS_ENABLE_ALL_NOTIF_PREFERENCE_KEY).let { pref ->
            val pushRuleService = Matrix.getInstance().currentSession ?: return
            val mRuleMaster = pushRuleService.getPushRules()
                    .find { it.ruleId == RuleIds.RULE_ID_DISABLE_ALL }

            if (mRuleMaster == null) {
                pref.isVisible = false
                return
            }

            val areNotifEnabledAtAccountLevelt = !mRuleMaster.enabled
            (pref as SwitchPreference).isChecked = areNotifEnabledAtAccountLevelt
        }
    }


    override fun onResume() {
        super.onResume()
        Matrix.getInstance().currentSession?.refreshPushers()
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {

        return when (preference?.key) {
            PreferencesManager.SETTINGS_ENABLE_THIS_DEVICE_PREFERENCE_KEY -> {
                updateEnabledForDevice(preference)
                true
            }
            PreferencesManager.SETTINGS_ENABLE_ALL_NOTIF_PREFERENCE_KEY   -> {
                updateEnabledForAccount(preference)
                true
            }
            else                                                          -> {
                return super.onPreferenceTreeClick(preference)
            }
        }

    }

    private fun updateEnabledForDevice(preference: Preference?) {
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
                        Matrix.getInstance().currentSession?.refreshPushers()
                        super.onSuccess(data)
                    }

                    override fun onFailure(failure: Throwable) {
                        Matrix.getInstance().currentSession?.refreshPushers()
                        Toast.makeText(activity, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
    }


    private fun updateEnabledForAccount(preference: Preference?) {
        val pushRuleService = Matrix.getInstance().currentSession ?: return
        val switchPref = preference as SwitchPreference
        pushRuleService.getPushRules()
                .find { it.ruleId == RuleIds.RULE_ID_DISABLE_ALL }
                ?.let {
                    //Trick, we must enable this room to disable notifications
                    pushRuleService.updatePushRuleEnableStatus("override", it, !switchPref.isChecked,
                            object : MatrixCallback<Unit> {

                                override fun onSuccess(data: Unit) {
                                    pushRuleService.fetchPushRules()
                                }

                                override fun onFailure(failure: Throwable) {
                                    //revert the check box
                                    switchPref.isChecked = !switchPref.isChecked
                                    Toast.makeText(activity, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                                }
                            })
                }

    }
}