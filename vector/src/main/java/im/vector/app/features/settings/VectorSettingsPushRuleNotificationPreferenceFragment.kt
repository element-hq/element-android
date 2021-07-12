/*
 * Copyright (c) 2021 New Vector Ltd
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

import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import im.vector.app.core.preference.VectorCheckboxPreference
import im.vector.app.core.utils.toast
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.pushrules.Action
import org.matrix.android.sdk.api.pushrules.RuleIds
import org.matrix.android.sdk.api.pushrules.RuleSetKey
import org.matrix.android.sdk.api.pushrules.rest.PushRule
import org.matrix.android.sdk.api.pushrules.rest.PushRuleAndKind

abstract class VectorSettingsPushRuleNotificationPreferenceFragment
    : VectorSettingsBaseFragment() {


    fun indexFromBooleanPreference(pushRuleOn: Boolean): Int {
        return if (pushRuleOn) {
            NOTIFICATION_NOISY_INDEX
        } else {
            NOTIFICATION_OFF_INDEX
        }
    }

    fun booleanPreferenceFromIndex(index: Int): Boolean {
        return index != NOTIFICATION_OFF_INDEX
    }

    /**
     * @return the bing rule status index
     */
    fun ruleStatusIndexFor(ruleAndKind: PushRuleAndKind? ): Int {
            val safeRule = ruleAndKind?.pushRule ?: return NOTIFICATION_OFF_INDEX

            if (safeRule.ruleId == RuleIds.RULE_ID_SUPPRESS_BOTS_NOTIFICATIONS) {
                if (safeRule.shouldNotNotify()) {
                    return if (safeRule.enabled) {
                        NOTIFICATION_OFF_INDEX
                    } else {
                        NOTIFICATION_SILENT_INDEX
                    }
                } else if (safeRule.shouldNotify()) {
                    return NOTIFICATION_NOISY_INDEX
                }
            }

            if (safeRule.enabled) {
                return if (safeRule.shouldNotNotify()) {
                    NOTIFICATION_OFF_INDEX
                } else if (safeRule.getNotificationSound() != null) {
                    NOTIFICATION_NOISY_INDEX
                } else {
                    NOTIFICATION_SILENT_INDEX
                }
            }

            return NOTIFICATION_OFF_INDEX
        }

    /**
     * Create a push rule with the updated required at index.
     *
     * @param index index
     * @return a push rule with the updated flags / null if there is no update
     */
    fun createNewRule(ruleAndKind: PushRuleAndKind?, index: Int): PushRule? {
        val safeRule = ruleAndKind?.pushRule ?: return null
        val safeKind = ruleAndKind.kind
        val ruleStatusIndex = ruleStatusIndexFor(ruleAndKind)

        return if (index != ruleStatusIndex) {
            if (safeRule.ruleId == RuleIds.RULE_ID_SUPPRESS_BOTS_NOTIFICATIONS) {
                when (index) {
                    NOTIFICATION_OFF_INDEX    -> {
                        safeRule.copy(enabled = true)
                                .setNotify(false)
                                .removeNotificationSound()
                    }
                    NOTIFICATION_SILENT_INDEX -> {
                        safeRule.copy(enabled = false)
                                .setNotify(false)
                    }
                    NOTIFICATION_NOISY_INDEX  -> {
                        safeRule.copy(enabled = true)
                                .setNotify(true)
                                .setNotificationSound()
                    }
                    else                                         -> safeRule
                }
            } else {
                if (NOTIFICATION_OFF_INDEX == index) {
                    if (safeKind == RuleSetKey.UNDERRIDE || safeRule.ruleId == RuleIds.RULE_ID_SUPPRESS_BOTS_NOTIFICATIONS) {
                        safeRule.setNotify(false)
                    } else {
                        safeRule.copy(enabled = false)
                    }
                } else {
                    val newRule = safeRule.copy(enabled = true)
                            .setNotify(true)
                            .setHighlight(safeKind != RuleSetKey.UNDERRIDE
                                    && safeRule.ruleId != RuleIds.RULE_ID_INVITE_ME
                                    && NOTIFICATION_NOISY_INDEX == index)

                    if (NOTIFICATION_NOISY_INDEX == index) {
                        newRule.setNotificationSound(
                                if (safeRule.ruleId == RuleIds.RULE_ID_CALL) {
                                    Action.ACTION_OBJECT_VALUE_VALUE_RING
                                } else {
                                    Action.ACTION_OBJECT_VALUE_VALUE_DEFAULT
                                }
                        )
                    } else {
                        newRule.removeNotificationSound()
                    }
                }
            }
        } else {
            safeRule
        }
    }


    override fun bindPref() {
        for (preferenceKey in prefKeyToPushRuleId.keys) {
            val preference = findPreference<VectorCheckboxPreference>(preferenceKey)!!
            // preference.isEnabled = null != rules && isConnected && pushManager.areDeviceNotificationsAllowed()
            val ruleAndKind: PushRuleAndKind? = session.getPushRules().findDefaultRule(prefKeyToPushRuleId[preferenceKey])

            if (ruleAndKind == null) {
                // The rule is not defined, hide the preference
                preference.isVisible = false
            } else {
                preference.isVisible = true

                val index = ruleStatusIndexFor(ruleAndKind)
                val boolPreference = booleanPreferenceFromIndex(index)
                preference.isChecked = boolPreference
                preference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    val newIndex = indexFromBooleanPreference(newValue as Boolean)
                    val newRule = createNewRule(ruleAndKind, newIndex)

                    if (newRule != null) {
                        displayLoadingView()

                        lifecycleScope.launch {
                            val result = runCatching {
                                session.updatePushRuleActions(
                                        ruleAndKind.kind,
                                        ruleAndKind.pushRule,
                                        newRule
                                )
                            }
                            if (!isAdded) {
                                return@launch
                            }
                            hideLoadingView()
                            result.onSuccess {
                                preference.isChecked = newValue
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

    private fun refreshDisplay() {
        listView?.adapter?.notifyDataSetChanged()
    }

    /* ==========================================================================================
     * Companion
     * ========================================================================================== */

    abstract val prefKeyToPushRuleId: Map<String, String>

    companion object {

        // index in mRuleStatuses
        private const val NOTIFICATION_OFF_INDEX = 0
        private const val NOTIFICATION_SILENT_INDEX = 1
        private const val NOTIFICATION_NOISY_INDEX = 2
    }
}
