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

package im.vector.app.features.settings.devices.v2.rename

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.settings.devices.v2.overview.GetDeviceFullInfoUseCase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class RenameSessionViewModel @AssistedInject constructor(
        @Assisted val initialState: RenameSessionViewState,
        private val getDeviceFullInfoUseCase: GetDeviceFullInfoUseCase,
        private val renameSessionUseCase: RenameSessionUseCase,
) : VectorViewModel<RenameSessionViewState, RenameSessionAction, RenameSessionViewEvent>(initialState) {

    companion object : MavericksViewModelFactory<RenameSessionViewModel, RenameSessionViewState> by hiltMavericksViewModelFactory()

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<RenameSessionViewModel, RenameSessionViewState> {
        override fun create(initialState: RenameSessionViewState): RenameSessionViewModel
    }

    init {
        observeSessionInfo(initialState.deviceId)
    }

    private fun observeSessionInfo(deviceId: String) {
        getDeviceFullInfoUseCase.execute(deviceId)
                .onEach { setState { copy(editedDeviceName = it.deviceInfo.displayName.orEmpty()) } }
                .launchIn(viewModelScope)
    }

    override fun handle(action: RenameSessionAction) {
        when (action) {
            is RenameSessionAction.EditLocally -> handleEditLocally(action.editedName)
            is RenameSessionAction.SaveModifications -> handleSaveModifications()
        }
    }

    private fun handleEditLocally(editedName: String) {
        setState { copy(editedDeviceName = editedName) }
    }

    private fun handleSaveModifications() = withState { viewState ->
        viewModelScope.launch {
            val result = renameSessionUseCase.execute(
                    deviceId = viewState.deviceId,
                    newName = viewState.editedDeviceName,
            )
            val viewEvent = if (result.isSuccess) {
                RenameSessionViewEvent.SessionRenamed
            } else {
                RenameSessionViewEvent.Failure(result.exceptionOrNull() ?: Exception())
            }
            _viewEvents.post(viewEvent)
        }
    }
}
