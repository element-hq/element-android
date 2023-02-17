/*
 * Copyright (c) 2023 New Vector Ltd
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

import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.preference.VectorCheckboxPreference
import im.vector.app.features.settings.VectorSettingsBaseFragment
import im.vector.app.features.themes.ThemeUtils

abstract class VectorSettingsPushRuleNotificationFragment :
        VectorSettingsBaseFragment() {

    protected val viewModel: VectorSettingsPushRuleNotificationViewModel by fragmentViewModel()

    abstract val prefKeyToPushRuleId: Map<String, String>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewEvents()
        viewModel.onEach(VectorSettingsPushRuleNotificationViewState::isLoading) { updateLoadingView(it) }
        viewModel.onEach(VectorSettingsPushRuleNotificationViewState::rulesOnError) { refreshErrors(it) }
    }

    private fun observeViewEvents() {
        viewModel.observeViewEvents {
            when (it) {
                is VectorSettingsPushRuleNotificationViewEvent.Failure -> onFailure(it.ruleId)
                is VectorSettingsPushRuleNotificationViewEvent.PushRuleUpdated -> {
                    updatePreference(it.ruleId, it.checked)
                    if (it.failure != null) {
                        onFailure(it.ruleId)
                    }
                }
            }
        }
    }

    override fun bindPref() {
        for (preferenceKey in prefKeyToPushRuleId.keys) {
            val preference = findPreference<VectorCheckboxPreference>(preferenceKey)!!
            preference.isIconSpaceReserved = false
            val ruleAndKind = prefKeyToPushRuleId[preferenceKey]?.let { viewModel.getPushRuleAndKind(it) }
            if (ruleAndKind == null) {
                // The rule is not defined, hide the preference
                preference.isVisible = false
            } else {
                preference.isVisible = true
                updatePreference(ruleAndKind.pushRule.ruleId, viewModel.isPushRuleChecked(ruleAndKind.pushRule.ruleId))
                preference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    viewModel.handle(VectorSettingsPushRuleNotificationViewAction.UpdatePushRule(ruleAndKind, newValue as Boolean))
                    false
                }
            }
        }
    }

    private fun updateLoadingView(isLoading: Boolean) {
        if (isLoading) {
            displayLoadingView()
        } else {
            hideLoadingView()
        }
    }

    private fun refreshErrors(rulesWithError: Set<String>) {
        if (withState(viewModel, VectorSettingsPushRuleNotificationViewState::isLoading)) return
        prefKeyToPushRuleId.forEach { (preferenceKey, ruleId) ->
            val preference = findPreference<VectorCheckboxPreference>(preferenceKey)!!
            if (ruleId in rulesWithError) {
                preference.summaryTextColor = context?.let { ThemeUtils.getColor(it, R.attr.colorError) }
                preference.setSummary(R.string.settings_notification_error_on_update)
            } else {
                preference.summaryTextColor = null
                preference.summary = null
            }
        }
    }

    protected fun refreshDisplay() {
        listView?.adapter?.notifyDataSetChanged()
    }

    private fun updatePreference(ruleId: String, checked: Boolean) {
        val preferenceKey = prefKeyToPushRuleId.entries.find { it.value == ruleId }?.key ?: return
        val preference = findPreference<VectorCheckboxPreference>(preferenceKey) ?: return
        preference.isChecked = checked
    }

    protected open fun onFailure(ruleId: String) {
        refreshDisplay()
    }
}
