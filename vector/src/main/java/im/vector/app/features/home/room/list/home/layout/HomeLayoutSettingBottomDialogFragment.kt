/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list.home.layout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetHomeLayoutSettingsBinding
import im.vector.app.features.analytics.plan.Interaction
import im.vector.app.features.home.room.list.home.HomeLayoutPreferencesStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeLayoutSettingBottomDialogFragment : VectorBaseBottomSheetDialogFragment<BottomSheetHomeLayoutSettingsBinding>() {

    @Inject lateinit var preferencesStore: HomeLayoutPreferencesStore

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetHomeLayoutSettingsBinding {
        return BottomSheetHomeLayoutSettingsBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            views.homeLayoutSettingsRecents.isChecked = preferencesStore.areRecentsEnabledFlow.first()
            views.homeLayoutSettingsFilters.isChecked = preferencesStore.areFiltersEnabledFlow.first()

            if (preferencesStore.isAZOrderingEnabledFlow.first()) {
                views.homeLayoutSettingsSortName.isChecked = true
            } else {
                views.homeLayoutSettingsSortActivity.isChecked = true
            }
        }

        views.homeLayoutSettingsRecents.setOnCheckedChangeListener { _, isChecked ->
            trackRecentsStateEvent(isChecked)
            setRecentsEnabled(isChecked)
        }
        views.homeLayoutSettingsFilters.setOnCheckedChangeListener { _, isChecked ->
            trackFiltersStateEvent(isChecked)
            setFiltersEnabled(isChecked)
        }
        views.homeLayoutSettingsSortGroup.setOnCheckedChangeListener { _, checkedId ->
            setAzOrderingEnabled(checkedId == R.id.home_layout_settings_sort_name)
        }
    }

    private fun trackRecentsStateEvent(areEnabled: Boolean) {
        val interactionName = if (areEnabled) {
            Interaction.Name.MobileAllChatsRecentsEnabled
        } else {
            Interaction.Name.MobileAllChatsRecentsDisabled
        }
        analyticsTracker.capture(
                Interaction(
                        index = null,
                        interactionType = null,
                        name = interactionName
                )
        )
    }

    private fun setRecentsEnabled(isEnabled: Boolean) = lifecycleScope.launch {
        preferencesStore.setRecentsEnabled(isEnabled)
    }

    private fun trackFiltersStateEvent(areEnabled: Boolean) {
        val interactionName = if (areEnabled) {
            Interaction.Name.MobileAllChatsFiltersEnabled
        } else {
            Interaction.Name.MobileAllChatsFiltersDisabled
        }
        analyticsTracker.capture(
                Interaction(
                        index = null,
                        interactionType = null,
                        name = interactionName
                )
        )
    }

    private fun setFiltersEnabled(isEnabled: Boolean) = lifecycleScope.launch {
        preferencesStore.setFiltersEnabled(isEnabled)
    }

    private fun setAzOrderingEnabled(isEnabled: Boolean) = lifecycleScope.launch {
        preferencesStore.setAZOrderingEnabled(isEnabled)
    }
}
