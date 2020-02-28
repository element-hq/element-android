/*
 * Copyright 2018 New Vector Ltd
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

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Parcelable
import androidx.core.content.edit
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.pushrules.rest.PushRule
import im.vector.matrix.android.api.pushrules.rest.PushRuleAndKind
import im.vector.riotx.R
import im.vector.riotx.core.preference.BingRulePreference
import im.vector.riotx.core.preference.VectorPreference
import im.vector.riotx.core.utils.toast
import im.vector.riotx.features.notifications.NotificationUtils
import javax.inject.Inject

class VectorSettingsAdvancedNotificationPreferenceFragment @Inject constructor(
        private val vectorPreferences: VectorPreferences
) : VectorSettingsBaseFragment() {

    // events listener
    /* TODO
    private val mEventsListener = object : MXEventListener() {
        override fun onBingRulesUpdate() {
            refreshPreferences()
            refreshDisplay()
        }
    }    */

    override var titleRes: Int = R.string.settings_notification_advanced

    override val preferenceXmlRes = R.xml.vector_settings_notification_advanced_preferences

    override fun bindPref() {
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

        for (preferenceKey in mPrefKeyToBingRuleId.keys) {
            val preference = findPreference<VectorPreference>(preferenceKey)
            if (preference is BingRulePreference) {
                // preference.isEnabled = null != rules && isConnected && pushManager.areDeviceNotificationsAllowed()
                val ruleAndKind: PushRuleAndKind? = session.getPushRules().findDefaultRule(mPrefKeyToBingRuleId[preferenceKey])

                if (ruleAndKind == null) {
                    // The rule is not defined, hide the preference
                    preference.isVisible = false
                } else {
                    preference.isVisible = true
                    preference.setPushRule(ruleAndKind)
                    preference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                        val newRule = preference.createNewRule(newValue as Int)
                        if (newRule != null) {
                            displayLoadingView()

                            session.updatePushRuleActions(
                                    ruleAndKind.kind,
                                    preference.ruleAndKind?.pushRule ?: ruleAndKind.pushRule,
                                    newRule,
                                    object : MatrixCallback<Unit> {
                                        override fun onSuccess(data: Unit) {
                                            if (!isAdded) {
                                                return
                                            }
                                            preference.setPushRule(ruleAndKind.copy(pushRule = newRule))
                                            hideLoadingView()
                                        }

                                        override fun onFailure(failure: Throwable) {
                                            if (!isAdded) {
                                                return
                                            }
                                            hideLoadingView()
                                            activity?.toast(errorFormatter.toHumanReadable(failure))
                                        }
                                    })
                        }
                        false
                    }
                }
            }
        }
    }

    private fun refreshDisplay() {
        listView?.adapter?.notifyDataSetChanged()
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

    /**
     * Refresh the known information about the account
     */
    private fun refreshPreferences() {
        PreferenceManager.getDefaultSharedPreferences(activity).edit {
            /* TODO
            session.dataHandler.pushRules()?.let {
                for (prefKey in mPrefKeyToBingRuleId.keys) {
                    val preference = findPreference(prefKey)

                    if (null != preference && preference is SwitchPreference) {
                        val ruleId = mPrefKeyToBingRuleId[prefKey]

                        val rule = it.findDefaultRule(ruleId)
                        var isEnabled = null != rule && rule.isEnabled

                        if (TextUtils.equals(ruleId, PushRule.RULE_ID_DISABLE_ALL) || TextUtils.equals(ruleId, PushRule.RULE_ID_SUPPRESS_BOTS_NOTIFICATIONS)) {
                            isEnabled = !isEnabled
                        } else if (isEnabled) {
                            val domainActions = rule!!.domainActions

                            // no action -> noting will be done
                            if (null == domainActions || domainActions.isEmpty()) {
                                isEnabled = false
                            } else if (1 == domainActions.size) {
                                try {
                                    isEnabled = !TextUtils.equals(domainActions[0] as String, PushRule.ACTION_DONT_NOTIFY)
                                } catch (e: Exception) {
                                    Timber.e(e, "## refreshPreferences failed")
                                }

                            }
                        }// check if the rule is only defined by don't notify

                        putBoolean(prefKey, isEnabled)
                    }
                }
            }
            */
        }
    }

    /* ==========================================================================================
     * Companion
     * ========================================================================================== */

    companion object {
        private const val REQUEST_NOTIFICATION_RINGTONE = 888

        //  preference name <-> rule Id
        private val mPrefKeyToBingRuleId = mapOf(
                VectorPreferences.SETTINGS_CONTAINING_MY_DISPLAY_NAME_PREFERENCE_KEY to PushRule.RULE_ID_CONTAIN_DISPLAY_NAME,
                VectorPreferences.SETTINGS_CONTAINING_MY_USER_NAME_PREFERENCE_KEY to PushRule.RULE_ID_CONTAIN_USER_NAME,
                VectorPreferences.SETTINGS_MESSAGES_IN_ONE_TO_ONE_PREFERENCE_KEY to PushRule.RULE_ID_ONE_TO_ONE_ROOM,
                VectorPreferences.SETTINGS_MESSAGES_IN_GROUP_CHAT_PREFERENCE_KEY to PushRule.RULE_ID_ALL_OTHER_MESSAGES_ROOMS,
                VectorPreferences.SETTINGS_INVITED_TO_ROOM_PREFERENCE_KEY to PushRule.RULE_ID_INVITE_ME,
                VectorPreferences.SETTINGS_CALL_INVITATIONS_PREFERENCE_KEY to PushRule.RULE_ID_CALL,
                VectorPreferences.SETTINGS_MESSAGES_SENT_BY_BOT_PREFERENCE_KEY to PushRule.RULE_ID_SUPPRESS_BOTS_NOTIFICATIONS,
                VectorPreferences.SETTINGS_MESSAGES_CONTAINING_AT_ROOM_PREFERENCE_KEY to PushRule.RULE_ID_AT_ROOMS,
                VectorPreferences.SETTINGS_MESSAGES_IN_E2E_ONE_ONE_CHAT_PREFERENCE_KEY to PushRule.RULE_ID_E2E_ONE_TO_ONE_ROOM,
                VectorPreferences.SETTINGS_MESSAGES_IN_E2E_GROUP_CHAT_PREFERENCE_KEY to PushRule.RULE_ID_E2E_GROUP,
                VectorPreferences.SETTINGS_ROOMS_UPGRADED_KEY to PushRule.RULE_ID_TOMBSTONE
        )
    }
}
