/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.notifications

import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.core.preference.VectorCheckboxPreference
import im.vector.app.features.settings.VectorSettingsBaseFragment
import im.vector.app.features.themes.ThemeUtils
import im.vector.lib.strings.CommonStrings

abstract class VectorSettingsPushRuleNotificationFragment :
        VectorSettingsBaseFragment() {

    protected val viewModel: VectorSettingsPushRuleNotificationViewModel by fragmentViewModel()

    abstract val prefKeyToPushRuleId: Map<String, String>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewEvents()
        viewModel.onEach(VectorSettingsPushRuleNotificationViewState::allRules) { refreshPreferences() }
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
        prefKeyToPushRuleId.forEach { (preferenceKey, ruleId) ->
            findPreference<VectorCheckboxPreference>(preferenceKey)?.apply {
                isIconSpaceReserved = false
                onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    viewModel.handle(VectorSettingsPushRuleNotificationViewAction.UpdatePushRule(ruleId, newValue as Boolean))
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

    private fun refreshPreferences() {
        prefKeyToPushRuleId.values.forEach { ruleId -> updatePreference(ruleId, viewModel.isPushRuleChecked(ruleId)) }
    }

    private fun refreshErrors(rulesWithError: Set<String>) {
        if (withState(viewModel, VectorSettingsPushRuleNotificationViewState::isLoading)) return
        prefKeyToPushRuleId.forEach { (preferenceKey, ruleId) ->
            findPreference<VectorCheckboxPreference>(preferenceKey)?.apply {
                if (ruleId in rulesWithError) {
                    summaryTextColor = ThemeUtils.getColor(context, com.google.android.material.R.attr.colorError)
                    setSummary(CommonStrings.settings_notification_error_on_update)
                } else {
                    summaryTextColor = null
                    summary = null
                }
            }
        }
    }

    protected fun refreshDisplay() {
        listView?.adapter?.notifyDataSetChanged()
    }

    private fun updatePreference(ruleId: String, checked: Boolean) {
        val preferenceKey = prefKeyToPushRuleId.entries.find { it.value == ruleId }?.key ?: return
        val preference = findPreference<VectorCheckboxPreference>(preferenceKey) ?: return
        val ruleIds = withState(viewModel) { state -> state.allRules.map { it.ruleId } }
        preference.isVisible = ruleId in ruleIds
        preference.isChecked = checked
    }

    protected open fun onFailure(ruleId: String) {
        refreshDisplay()
    }
}
