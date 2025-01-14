/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.settings.troubleshoot

import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.notifications.toNotificationAction
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.pushrules.RuleIds
import org.matrix.android.sdk.api.session.pushrules.getActions
import javax.inject.Inject

class TestPushRulesSettings @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
        private val stringProvider: StringProvider
) :
        TroubleshootTest(CommonStrings.settings_troubleshoot_test_bing_settings_title) {

    private val testedRules =
            listOf(
                    RuleIds.RULE_ID_CONTAIN_DISPLAY_NAME,
                    RuleIds.RULE_ID_CONTAIN_USER_NAME,
                    RuleIds.RULE_ID_ONE_TO_ONE_ROOM,
                    RuleIds.RULE_ID_ALL_OTHER_MESSAGES_ROOMS
            )

    override fun perform(testParameters: TestParameters) {
        val session = activeSessionHolder.getSafeActiveSession() ?: return
        val pushRules = session.pushRuleService().getPushRules().getAllRules()
        var oneOrMoreRuleIsOff = false
        var oneOrMoreRuleAreSilent = false
        testedRules.forEach { ruleId ->
            pushRules.find { it.ruleId == ruleId }?.let { rule ->
                val actions = rule.getActions()
                val notifAction = actions.toNotificationAction()
                if (!rule.enabled || !notifAction.shouldNotify) {
                    // off
                    oneOrMoreRuleIsOff = true
                } else if (notifAction.soundName == null) {
                    // silent
                    oneOrMoreRuleAreSilent = true
                } else {
                    // noisy
                }
            }
        }

        if (oneOrMoreRuleIsOff) {
            description = stringProvider.getString(CommonStrings.settings_troubleshoot_test_bing_settings_failed)
            // TODO
//                quickFix = object : TroubleshootQuickFix(CommonStrings.settings_troubleshoot_test_bing_settings_quickfix) {
//                    override fun doFix() {
//                        val activity = fragment.activity
//                        if (activity is VectorSettingsFragmentInteractionListener) {
//                            activity.requestHighlightPreferenceKeyOnResume(VectorPreferences.SETTINGS_NOTIFICATION_ADVANCED_PREFERENCE_KEY)
//                        }
//                        activity?.supportFragmentManager?.popBackStack()
//                    }
//                }
            status = TestStatus.FAILED
        } else {
            description = if (oneOrMoreRuleAreSilent) {
                stringProvider.getString(CommonStrings.settings_troubleshoot_test_bing_settings_success_with_warn)
            } else {
                null
            }
            status = TestStatus.SUCCESS
        }
    }
}
