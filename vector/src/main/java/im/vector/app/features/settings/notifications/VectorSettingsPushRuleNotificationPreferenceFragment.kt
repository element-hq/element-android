/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.notifications

import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import im.vector.app.core.preference.VectorCheckboxPreference
import im.vector.app.features.settings.VectorSettingsBaseFragment
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.pushrules.RuleKind
import org.matrix.android.sdk.api.session.pushrules.rest.PushRuleAndKind

abstract class VectorSettingsPushRuleNotificationPreferenceFragment :
        VectorSettingsBaseFragment() {

    abstract val prefKeyToPushRuleId: Map<String, String>

    override fun bindPref() {
        for (preferenceKey in prefKeyToPushRuleId.keys) {
            val preference = findPreference<VectorCheckboxPreference>(preferenceKey)!!
            preference.isIconSpaceReserved = false
            val ruleAndKind: PushRuleAndKind? = session.pushRuleService().getPushRules().findDefaultRule(prefKeyToPushRuleId[preferenceKey])
            if (ruleAndKind == null) {
                // The rule is not defined, hide the preference
                preference.isVisible = false
            } else {
                preference.isVisible = true
                val initialIndex = ruleAndKind.pushRule.notificationIndex
                preference.isChecked = initialIndex != NotificationIndex.OFF
                preference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    updatePushRule(ruleAndKind.pushRule.ruleId, ruleAndKind.kind, newValue as Boolean, preference)
                    false
                }
            }
        }
    }

    fun updatePushRule(ruleId: String, kind: RuleKind, checked: Boolean, preference: VectorCheckboxPreference) {
        val newIndex = if (checked) NotificationIndex.NOISY else NotificationIndex.OFF
        val standardAction = getStandardAction(ruleId, newIndex) ?: return
        val enabled = standardAction != StandardActions.Disabled
        val newActions = standardAction.actions
        displayLoadingView()

        lifecycleScope.launch {
            val result = runCatching {
                session.pushRuleService().updatePushRuleActions(
                        kind,
                        ruleId,
                        enabled,
                        newActions
                )
            }
            hideLoadingView()
            if (!isAdded) {
                return@launch
            }
            result.onSuccess {
                preference.isChecked = checked
            }
            result.onFailure { failure ->
                // Restore the previous value
                refreshDisplay()
                displayErrorDialog(failure)
            }
        }
    }

    fun refreshDisplay() {
        listView?.adapter?.notifyDataSetChanged()
    }
}
