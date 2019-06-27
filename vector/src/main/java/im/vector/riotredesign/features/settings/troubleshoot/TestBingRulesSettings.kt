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
package im.vector.riotredesign.features.settings.troubleshoot

import androidx.fragment.app.Fragment
import im.vector.matrix.android.api.pushrules.Action
import im.vector.matrix.android.api.pushrules.RuleIds
import im.vector.matrix.android.api.session.Session
import im.vector.riotredesign.R
import im.vector.riotredesign.features.notifications.NotificationAction

class TestBingRulesSettings(val fragment: Fragment, val session: Session) : TroubleshootTest(R.string.settings_troubleshoot_test_bing_settings_title) {

    private val testedRules =
            listOf(RuleIds.RULE_ID_CONTAIN_DISPLAY_NAME,
                    RuleIds.RULE_ID_CONTAIN_USER_NAME,
                    RuleIds.RULE_ID_ONE_TO_ONE_ROOM,
                    RuleIds.RULE_ID_ALL_OTHER_MESSAGES_ROOMS)


    val ruleSettingsName = arrayOf(R.string.settings_containing_my_display_name,
            R.string.settings_containing_my_user_name,
            R.string.settings_messages_in_one_to_one,
            R.string.settings_messages_in_group_chat)

    override fun perform() {
        val pushRules = session.getPushRules()
        if (pushRules == null) {
            description = fragment.getString(R.string.settings_troubleshoot_test_bing_settings_failed_to_load_rules)
            status = TestStatus.FAILED
        } else {
            var oneOrMoreRuleIsOff = false
            var oneOrMoreRuleAreSilent = false
            for ((index, ruleId) in testedRules.withIndex()) {
                pushRules.find { it.ruleId == ruleId }?.let { rule ->
                    val actions = Action.mapFrom(rule) ?: return@let
                    val notifAction = NotificationAction.extractFrom(actions)
                    if (!rule.enabled || !notifAction.shouldNotify) {
                        //off
                        oneOrMoreRuleIsOff = true
                    } else if (notifAction.soundName == null) {
                        //silent
                        oneOrMoreRuleAreSilent = true
                    } else {
                        //noisy
                    }
                }

            }

            if (oneOrMoreRuleIsOff) {
                description = fragment.getString(R.string.settings_troubleshoot_test_bing_settings_failed)
                //TODO
//                quickFix = object : TroubleshootQuickFix(R.string.settings_troubleshoot_test_bing_settings_quickfix) {
//                    override fun doFix() {
//                        val activity = fragment.activity
//                        if (activity is VectorSettingsFragmentInteractionListener) {
//                            activity.requestHighlightPreferenceKeyOnResume(PreferencesManager.SETTINGS_NOTIFICATION_ADVANCED_PREFERENCE_KEY)
//                        }
//                        activity?.supportFragmentManager?.popBackStack()
//                    }
//                }
                status = TestStatus.FAILED
            } else {
                if (oneOrMoreRuleAreSilent) {
                    description = fragment.getString(R.string.settings_troubleshoot_test_bing_settings_success_with_warn)
                } else {
                    description = null
                }
                status = TestStatus.SUCCESS
            }
        }
    }
}