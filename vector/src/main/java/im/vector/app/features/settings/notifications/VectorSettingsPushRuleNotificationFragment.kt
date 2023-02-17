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
import im.vector.app.core.preference.VectorCheckboxPreference
import im.vector.app.features.settings.VectorSettingsBaseFragment

abstract class VectorSettingsPushRuleNotificationFragment :
        VectorSettingsBaseFragment() {

    private val viewModel: VectorSettingsPushRuleNotificationViewModel by fragmentViewModel()

    abstract val prefKeyToPushRuleId: Map<String, String>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewEvents()
        viewModel.onEach(VectorSettingsPushRuleNotificationViewState::isLoading) { isLoading ->
            if (isLoading) {
                displayLoadingView()
            } else {
                hideLoadingView()
            }
        }
    }

    private fun observeViewEvents() {
        viewModel.observeViewEvents {
            when (it) {
                is VectorSettingsPushRuleNotificationViewEvent.Failure -> refreshDisplay()
                is VectorSettingsPushRuleNotificationViewEvent.PushRuleUpdated -> updatePreference(it.ruleId, it.checked)
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

    override fun invalidate() = withState(viewModel) { state ->
        if (state.isLoading) {
            displayLoadingView()
        } else {
            hideLoadingView()
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
}
