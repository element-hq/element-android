/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.home.room.list.home.layout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetHomeLayoutSettingsBinding
import im.vector.app.features.home.room.list.home.HomeLayoutPreferences
import javax.inject.Inject

@AndroidEntryPoint
class HomeLayoutSettingBottomDialogFragment : VectorBaseBottomSheetDialogFragment<BottomSheetHomeLayoutSettingsBinding>() {

    @Inject lateinit var preferences: HomeLayoutPreferences

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetHomeLayoutSettingsBinding {
        return BottomSheetHomeLayoutSettingsBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.homeLayoutSettingsRecents.isChecked = preferences.areRecentsEnabled()
        views.homeLayoutSettingsFilters.isChecked = preferences.areFiltersEnabled()

        views.homeLayoutSettingsRecents.setOnCheckedChangeListener { _, isChecked ->
            preferences.setRecentsEnabled(isChecked)
        }

        views.homeLayoutSettingsFilters.setOnCheckedChangeListener { _, isChecked ->
            preferences.setFiltersEnabled(isChecked)
        }

        views.homeLayoutSettingsDone.setOnClickListener {
            dismiss()
        }
    }
}
