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

package im.vector.app.features.settings.font

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.configuration.VectorConfiguration
import im.vector.app.features.settings.FontScalePreferences

class FontScaleSettingViewModel @AssistedInject constructor(
        @Assisted initialState: FontScaleSettingViewState,
        private val vectorConfiguration: VectorConfiguration,
        private val fontScalePreferences: FontScalePreferences,
) : VectorViewModel<FontScaleSettingViewState, FontScaleSettingAction, FontScaleSettingViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<FontScaleSettingViewModel, FontScaleSettingViewState> {
        override fun create(initialState: FontScaleSettingViewState): FontScaleSettingViewModel
    }

    companion object : MavericksViewModelFactory<FontScaleSettingViewModel, FontScaleSettingViewState> by hiltMavericksViewModelFactory()

    init {
        setState {
            copy(
                    availableScaleOptions = fontScalePreferences.getAvailableScales(),
                    useSystemSettings = fontScalePreferences.getUseSystemScale(),
                    persistedSettingIndex = fontScalePreferences.getAppFontScaleValue().index
            )
        }
    }

    override fun handle(action: FontScaleSettingAction) {
        when (action) {
            is FontScaleSettingAction.UseSystemSettingChangedAction -> handleUseSystemScale(action)
            is FontScaleSettingAction.FontScaleChangedAction -> handleFontScaleChange(action)
        }
    }

    private fun handleFontScaleChange(action: FontScaleSettingAction.FontScaleChangedAction) {
        setState {
            copy(persistedSettingIndex = fontScalePreferences.getAvailableScales().indexOf(action.fontScale))
        }

        fontScalePreferences.setFontScaleValue(action.fontScale)
        vectorConfiguration.applyToApplicationContext()

        _viewEvents.post(FontScaleSettingViewEvents.RestartActivity)
    }

    private fun handleUseSystemScale(action: FontScaleSettingAction.UseSystemSettingChangedAction) {
        setState {
            copy(useSystemSettings = action.useSystemSettings)
        }

        val oldScale = fontScalePreferences.getResolvedFontScaleValue()
        fontScalePreferences.setUseSystemScale(action.useSystemSettings)
        val newScale = fontScalePreferences.getResolvedFontScaleValue()

        if (oldScale != newScale) {
            vectorConfiguration.applyToApplicationContext()
            _viewEvents.post(FontScaleSettingViewEvents.RestartActivity)
        }
    }
}
