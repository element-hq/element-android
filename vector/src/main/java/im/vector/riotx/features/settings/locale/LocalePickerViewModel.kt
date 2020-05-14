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

package im.vector.riotx.features.settings.locale

import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.riotx.core.extensions.exhaustive
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.features.configuration.VectorConfiguration
import im.vector.riotx.features.settings.VectorLocale

class LocalePickerViewModel @AssistedInject constructor(
        @Assisted initialState: LocalePickerViewState,
        private val vectorConfiguration: VectorConfiguration
) : VectorViewModel<LocalePickerViewState, LocalePickerAction, LocalePickerViewEvents>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: LocalePickerViewState): LocalePickerViewModel
    }

    companion object : MvRxViewModelFactory<LocalePickerViewModel, LocalePickerViewState> {

        override fun initialState(viewModelContext: ViewModelContext): LocalePickerViewState? {
            return LocalePickerViewState(
                    locales = VectorLocale.supportedLocales
            )
        }

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: LocalePickerViewState): LocalePickerViewModel? {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state) ?: error("You should let your activity/fragment implements Factory interface")
        }
    }

    override fun handle(action: LocalePickerAction) {
        when (action) {
            is LocalePickerAction.SelectLocale -> handleSelectLocale(action)
        }.exhaustive
    }

    private fun handleSelectLocale(action: LocalePickerAction.SelectLocale) {
        vectorConfiguration.updateApplicationLocale(action.locale)
        _viewEvents.post(LocalePickerViewEvents.RestartActivity)
    }
}
