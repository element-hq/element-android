/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.settings.locale

import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.configuration.VectorConfiguration
import im.vector.app.features.settings.VectorLocale
import kotlinx.coroutines.launch

class LocalePickerViewModel @AssistedInject constructor(
        @Assisted initialState: LocalePickerViewState,
        private val vectorConfiguration: VectorConfiguration
) : VectorViewModel<LocalePickerViewState, LocalePickerAction, LocalePickerViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<LocalePickerViewModel, LocalePickerViewState> {
        override fun create(initialState: LocalePickerViewState): LocalePickerViewModel
    }

    init {
        viewModelScope.launch {
            val result = VectorLocale.getSupportedLocales()

            setState {
                copy(
                        locales = Success(result)
                )
            }
        }
    }

    companion object : MavericksViewModelFactory<LocalePickerViewModel, LocalePickerViewState> by hiltMavericksViewModelFactory()

    override fun handle(action: LocalePickerAction) {
        when (action) {
            is LocalePickerAction.SelectLocale -> handleSelectLocale(action)
        }
    }

    private fun handleSelectLocale(action: LocalePickerAction.SelectLocale) {
        VectorLocale.saveApplicationLocale(action.locale)
        vectorConfiguration.applyToApplicationContext()
        _viewEvents.post(LocalePickerViewEvents.RestartActivity)
    }
}
