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

package im.vector.riotx.features.settings

import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.pushrules.RuleIds
import im.vector.matrix.android.api.pushrules.RuleKind
import im.vector.riotx.R
import im.vector.riotx.core.di.ActiveSessionHolder
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.preference.VectorSwitchPreference
import im.vector.riotx.core.pushers.PushersManager
import im.vector.riotx.push.fcm.FcmHelper
import javax.inject.Inject

// Referenced in vector_settings_preferences_root.xml
class VectorSettingsNotificationPreferenceFragment : VectorSettingsBaseFragment() {

    override var titleRes: Int = R.string.settings_notifications
    override val preferenceXmlRes = R.xml.vector_settings_notifications

    @Inject lateinit var pushManager: PushersManager
    @Inject lateinit var activeSessionHolder: ActiveSessionHolder
    @Inject lateinit var vectorPreferences: VectorPreferences

    override fun bindPref() {
        findPreference<VectorSwitchPreference>(VectorPreferences.SETTINGS_ENABLE_ALL_NOTIF_PREFERENCE_KEY)!!.let { pref ->
            val pushRuleService = session
            val mRuleMaster = pushRuleService.getPushRules()
                    .find { it.ruleId == RuleIds.RULE_ID_DISABLE_ALL }

            if (mRuleMaster == null) {
                // The home server does not support RULE_ID_DISABLE_ALL, so hide the preference
                pref.isVisible = false
                return
            }

            val areNotifEnabledAtAccountLevel = !mRuleMaster.enabled
            (pref as SwitchPreference).isChecked = areNotifEnabledAtAccountLevel
        }
    }

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun onResume() {
        super.onResume()
        activeSessionHolder.getSafeActiveSession()?.refreshPushers()
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {

        return when (preference?.key) {
            VectorPreferences.SETTINGS_ENABLE_THIS_DEVICE_PREFERENCE_KEY -> {
                updateEnabledForDevice(preference)
                true
            }
            VectorPreferences.SETTINGS_ENABLE_ALL_NOTIF_PREFERENCE_KEY   -> {
                updateEnabledForAccount(preference)
                true
            }
            else                                                         -> {
                return super.onPreferenceTreeClick(preference)
            }
        }

    }

    private fun updateEnabledForDevice(preference: Preference?) {
        val switchPref = preference as SwitchPreference
        if (switchPref.isChecked) {
            FcmHelper.getFcmToken(requireContext())?.let {
                if (vectorPreferences.areNotificationEnabledForDevice()) {
                    pushManager.registerPusherWithFcmKey(it)
                }
            }
        } else {
            FcmHelper.getFcmToken(requireContext())?.let {
                pushManager.unregisterPusher(it, object : MatrixCallback<Unit> {
                    override fun onSuccess(data: Unit) {
                        session.refreshPushers()
                        super.onSuccess(data)
                    }

                    override fun onFailure(failure: Throwable) {
                        session.refreshPushers()
                        Toast.makeText(activity, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
    }


    private fun updateEnabledForAccount(preference: Preference?) {
        val pushRuleService = session
        val switchPref = preference as SwitchPreference
        pushRuleService.getPushRules()
                .find { it.ruleId == RuleIds.RULE_ID_DISABLE_ALL }
                ?.let {
                    //Trick, we must enable this room to disable notifications
                    pushRuleService.updatePushRuleEnableStatus(RuleKind.OVERRIDE,
                            it,
                            !switchPref.isChecked,
                            object : MatrixCallback<Unit> {

                                override fun onSuccess(data: Unit) {
                                    // Push rules will be updated form the sync
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