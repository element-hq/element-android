/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.settings.notifications.advanced

import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.preference.PushRulePreference
import im.vector.app.core.preference.VectorPreference
import im.vector.app.core.utils.toast
import im.vector.app.features.settings.VectorSettingsBaseFragment
import im.vector.app.features.settings.notifications.NotificationIndex
import im.vector.app.features.settings.notifications.StandardActions
import im.vector.app.features.settings.notifications.getStandardAction
import im.vector.app.features.settings.notifications.notificationIndex
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.pushrules.RuleIds
import org.matrix.android.sdk.api.session.pushrules.rest.PushRuleAndKind

// TODO This fragment seems not used anymore, we can probably delete it
@AndroidEntryPoint
class VectorSettingsAdvancedNotificationPreferenceFragment :
        VectorSettingsBaseFragment() {

    override var titleRes: Int = CommonStrings.settings_notification_advanced

    override val preferenceXmlRes = R.xml.vector_settings_notification_advanced_preferences

    override fun bindPref() {
        for (preferenceKey in prefKeyToPushRuleId.keys) {
            val preference = findPreference<VectorPreference>(preferenceKey)
            if (preference is PushRulePreference) {
                val ruleAndKind: PushRuleAndKind? = prefKeyToPushRuleId[preferenceKey]?.let { session.pushRuleService().getPushRules().findDefaultRule(it) }

                if (ruleAndKind == null) {
                    // The rule is not defined, hide the preference
                    preference.isVisible = false
                } else {
                    preference.isVisible = true
                    val initialIndex = ruleAndKind.pushRule.notificationIndex
                    preference.setIndex(initialIndex)
                    preference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                        val newIndex = newValue as NotificationIndex
                        val standardAction = getStandardAction(ruleAndKind.pushRule.ruleId, newIndex)
                        if (standardAction != null) {
                            val enabled = standardAction != StandardActions.Disabled
                            val newActions = standardAction.actions
                            displayLoadingView()

                            lifecycleScope.launch {
                                val result = runCatching {
                                    session.pushRuleService().updatePushRuleActions(
                                            ruleAndKind.kind,
                                            ruleAndKind.pushRule.ruleId,
                                            enabled,
                                            newActions
                                    )
                                }
                                if (!isAdded) {
                                    return@launch
                                }
                                hideLoadingView()
                                result.onSuccess {
                                    preference.setIndex(newIndex)
                                }
                                result.onFailure { failure ->
                                    // Restore the previous value
                                    refreshDisplay()
                                    activity?.toast(errorFormatter.toHumanReadable(failure))
                                }
                            }
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

    /* ==========================================================================================
     * Companion
     * ========================================================================================== */

    companion object {
        //  preference name <-> rule Id
        private val prefKeyToPushRuleId = mapOf(
                "SETTINGS_PUSH_RULE_CONTAINING_MY_DISPLAY_NAME_PREFERENCE_KEY" to RuleIds.RULE_ID_CONTAIN_DISPLAY_NAME,
                "SETTINGS_PUSH_RULE_CONTAINING_MY_USER_NAME_PREFERENCE_KEY" to RuleIds.RULE_ID_CONTAIN_USER_NAME,
                "SETTINGS_PUSH_RULE_MESSAGES_IN_ONE_TO_ONE_PREFERENCE_KEY" to RuleIds.RULE_ID_ONE_TO_ONE_ROOM,
                "SETTINGS_PUSH_RULE_MESSAGES_IN_GROUP_CHAT_PREFERENCE_KEY" to RuleIds.RULE_ID_ALL_OTHER_MESSAGES_ROOMS,
                "SETTINGS_PUSH_RULE_INVITED_TO_ROOM_PREFERENCE_KEY" to RuleIds.RULE_ID_INVITE_ME,
                "SETTINGS_PUSH_RULE_CALL_INVITATIONS_PREFERENCE_KEY" to RuleIds.RULE_ID_CALL,
                "SETTINGS_PUSH_RULE_MESSAGES_SENT_BY_BOT_PREFERENCE_KEY" to RuleIds.RULE_ID_SUPPRESS_BOTS_NOTIFICATIONS,
                "SETTINGS_PUSH_RULE_MESSAGES_CONTAINING_AT_ROOM_PREFERENCE_KEY" to RuleIds.RULE_ID_ROOM_NOTIF,
                "SETTINGS_PUSH_RULE_MESSAGES_IN_E2E_ONE_ONE_CHAT_PREFERENCE_KEY" to RuleIds.RULE_ID_ONE_TO_ONE_ENCRYPTED_ROOM,
                "SETTINGS_PUSH_RULE_MESSAGES_IN_E2E_GROUP_CHAT_PREFERENCE_KEY" to RuleIds.RULE_ID_ENCRYPTED,
                "SETTINGS_PUSH_RULE_ROOMS_UPGRADED_KEY" to RuleIds.RULE_ID_TOMBSTONE
        )
    }
}
