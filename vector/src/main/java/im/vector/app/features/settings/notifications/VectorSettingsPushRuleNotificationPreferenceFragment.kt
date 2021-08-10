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

package im.vector.app.features.settings.notifications

import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import im.vector.app.core.preference.VectorCheckboxPreference
import im.vector.app.core.utils.toast
import im.vector.app.features.settings.VectorSettingsBaseFragment
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.pushrules.rest.PushRuleAndKind

abstract class VectorSettingsPushRuleNotificationPreferenceFragment
    : VectorSettingsBaseFragment() {

    abstract val prefKeyToPushRuleId: Map<String, String>

    override fun bindPref() {
        for (preferenceKey in prefKeyToPushRuleId.keys) {
            val preference = findPreference<VectorCheckboxPreference>(preferenceKey)!!
            val ruleAndKind: PushRuleAndKind? = session.getPushRules().findDefaultRule(prefKeyToPushRuleId[preferenceKey])
            if (ruleAndKind == null) {
                // The rule is not defined, hide the preference
                preference.isVisible = false
            } else {
                preference.isVisible = true
                val initialIndex = ruleAndKind.pushRule.notificationIndex
                preference.isChecked = initialIndex != NotificationIndex.OFF
                preference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    val newIndex = if (newValue as Boolean) NotificationIndex.NOISY else NotificationIndex.OFF
                    val standardAction = getStandardAction(ruleAndKind.pushRule.ruleId, newIndex) ?: return@OnPreferenceChangeListener false
                    val enabled = standardAction != StandardActions.Disabled
                    val newActions = standardAction.actions
                    displayLoadingView()

                    lifecycleScope.launch {
                        val result = runCatching {
                            session.updatePushRuleActions(ruleAndKind.kind,
                                    ruleAndKind.pushRule.ruleId,
                                    enabled,
                                    newActions)
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

                    false
                }
            }
        }
    }

    private fun refreshDisplay() {
        listView?.adapter?.notifyDataSetChanged()
    }
}
