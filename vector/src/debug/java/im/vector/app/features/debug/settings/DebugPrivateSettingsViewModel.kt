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

package im.vector.app.features.debug.settings

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.settings.VectorDataStore
import kotlinx.coroutines.launch

class DebugPrivateSettingsViewModel @AssistedInject constructor(
        @Assisted initialState: DebugPrivateSettingsViewState,
        private val vectorDataStore: VectorDataStore
) : VectorViewModel<DebugPrivateSettingsViewState, DebugPrivateSettingsViewActions, EmptyViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<DebugPrivateSettingsViewModel, DebugPrivateSettingsViewState> {
        override fun create(initialState: DebugPrivateSettingsViewState): DebugPrivateSettingsViewModel
    }

    companion object : MavericksViewModelFactory<DebugPrivateSettingsViewModel, DebugPrivateSettingsViewState> by hiltMavericksViewModelFactory()

    init {
        observeVectorDataStore()
    }

    private fun observeVectorDataStore() {
        vectorDataStore.forceDialPadDisplayFlow.setOnEach {
            copy(
                    dialPadVisible = it
            )
        }
    }

    override fun handle(action: DebugPrivateSettingsViewActions) {
        when (action) {
            is DebugPrivateSettingsViewActions.SetDialPadVisibility -> handleSetDialPadVisibility(action)
        }
    }

    private fun handleSetDialPadVisibility(action: DebugPrivateSettingsViewActions.SetDialPadVisibility) {
        viewModelScope.launch {
            vectorDataStore.setForceDialPadDisplay(action.force)
        }
    }
}
