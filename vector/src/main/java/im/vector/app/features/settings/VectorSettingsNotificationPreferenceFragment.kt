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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Parcelable
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.pushrules.RuleIds
import org.matrix.android.sdk.api.pushrules.RuleKind
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.preference.VectorPreference
import im.vector.app.core.preference.VectorSwitchPreference
import im.vector.app.core.pushers.PushersManager
import im.vector.app.features.notifications.NotificationUtils
import im.vector.app.push.fcm.FcmHelper
import javax.inject.Inject

// Referenced in vector_settings_preferences_root.xml
class VectorSettingsNotificationPreferenceFragment @Inject constructor(
        private val pushManager: PushersManager,
        private val activeSessionHolder: ActiveSessionHolder,
        private val vectorPreferences: VectorPreferences
) : VectorSettingsBaseFragment() {

    override var titleRes: Int = R.string.settings_notifications
    override val preferenceXmlRes = R.xml.vector_settings_notifications

    private var interactionListener: VectorSettingsFragmentInteractionListener? = null

    override fun bindPref() {
        findPreference<VectorSwitchPreference>(VectorPreferences.SETTINGS_ENABLE_ALL_NOTIF_PREFERENCE_KEY)!!.let { pref ->
            val pushRuleService = session
            val mRuleMaster = pushRuleService.getPushRules().getAllRules()
                    .find { it.ruleId == RuleIds.RULE_ID_DISABLE_ALL }

            if (mRuleMaster == null) {
                // The home server does not support RULE_ID_DISABLE_ALL, so hide the preference
                pref.isVisible = false
                return
            }

            val areNotifEnabledAtAccountLevel = !mRuleMaster.enabled
            (pref as SwitchPreference).isChecked = areNotifEnabledAtAccountLevel
        }

        handleSystemPreference()
    }

    private fun handleSystemPreference() {
        val callNotificationsSystemOptions = findPreference<VectorPreference>(VectorPreferences.SETTINGS_SYSTEM_CALL_NOTIFICATION_PREFERENCE_KEY)!!
        if (NotificationUtils.supportNotificationChannels()) {
            callNotificationsSystemOptions.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                NotificationUtils.openSystemSettingsForCallCategory(this)
                false
            }
        } else {
            callNotificationsSystemOptions.isVisible = false
        }

        val noisyNotificationsSystemOptions = findPreference<VectorPreference>(VectorPreferences.SETTINGS_SYSTEM_NOISY_NOTIFICATION_PREFERENCE_KEY)!!
        if (NotificationUtils.supportNotificationChannels()) {
            noisyNotificationsSystemOptions.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                NotificationUtils.openSystemSettingsForNoisyCategory(this)
                false
            }
        } else {
            noisyNotificationsSystemOptions.isVisible = false
        }

        val silentNotificationsSystemOptions = findPreference<VectorPreference>(VectorPreferences.SETTINGS_SYSTEM_SILENT_NOTIFICATION_PREFERENCE_KEY)!!
        if (NotificationUtils.supportNotificationChannels()) {
            silentNotificationsSystemOptions.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                NotificationUtils.openSystemSettingsForSilentCategory(this)
                false
            }
        } else {
            silentNotificationsSystemOptions.isVisible = false
        }

        // Ringtone
        val ringtonePreference = findPreference<VectorPreference>(VectorPreferences.SETTINGS_NOTIFICATION_RINGTONE_SELECTION_PREFERENCE_KEY)!!

        if (NotificationUtils.supportNotificationChannels()) {
            ringtonePreference.isVisible = false
        } else {
            ringtonePreference.summary = vectorPreferences.getNotificationRingToneName()
            ringtonePreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)

                if (null != vectorPreferences.getNotificationRingTone()) {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, vectorPreferences.getNotificationRingTone())
                }

                startActivityForResult(intent, REQUEST_NOTIFICATION_RINGTONE)
                false
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_NOTIFICATION_RINGTONE -> {
                    vectorPreferences.setNotificationRingTone(data?.getParcelableExtra<Parcelable>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) as Uri?)

                    // test if the selected ring tone can be played
                    val notificationRingToneName = vectorPreferences.getNotificationRingToneName()
                    if (null != notificationRingToneName) {
                        vectorPreferences.setNotificationRingTone(vectorPreferences.getNotificationRingTone())
                        findPreference<VectorPreference>(VectorPreferences.SETTINGS_NOTIFICATION_RINGTONE_SELECTION_PREFERENCE_KEY)!!
                                .summary = notificationRingToneName
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activeSessionHolder.getSafeActiveSession()?.refreshPushers()

        interactionListener?.requestedKeyToHighlight()?.let { key ->
            interactionListener?.requestHighlightPreferenceKeyOnResume(null)
            val preference = findPreference<VectorSwitchPreference>(key)
            preference?.isHighlighted = true
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is VectorSettingsFragmentInteractionListener) {
            interactionListener = context
        }
    }

    override fun onDetach() {
        interactionListener = null
        super.onDetach()
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
                pushManager.registerPusherWithFcmKey(it)
            }
        } else {
            FcmHelper.getFcmToken(requireContext())?.let {
                pushManager.unregisterPusher(it, object : MatrixCallback<Unit> {
                    override fun onSuccess(data: Unit) {
                        session.refreshPushers()
                    }

                    override fun onFailure(failure: Throwable) {
                        if (!isAdded) {
                            return
                        }
                        // revert the check box
                        switchPref.isChecked = !switchPref.isChecked
                        Toast.makeText(activity, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
    }

    private fun updateEnabledForAccount(preference: Preference?) {
        val pushRuleService = session
        val switchPref = preference as SwitchPreference
        pushRuleService.getPushRules().getAllRules()
                .find { it.ruleId == RuleIds.RULE_ID_DISABLE_ALL }
                ?.let {
                    // Trick, we must enable this room to disable notifications
                    pushRuleService.updatePushRuleEnableStatus(RuleKind.OVERRIDE,
                            it,
                            !switchPref.isChecked,
                            object : MatrixCallback<Unit> {
                                override fun onSuccess(data: Unit) {
                                    // Push rules will be updated from the sync
                                }

                                override fun onFailure(failure: Throwable) {
                                    if (!isAdded) {
                                        return
                                    }

                                    // revert the check box
                                    switchPref.isChecked = !switchPref.isChecked
                                    Toast.makeText(activity, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                                }
                            })
                }
    }

    companion object {
        private const val REQUEST_NOTIFICATION_RINGTONE = 888
    }
}
