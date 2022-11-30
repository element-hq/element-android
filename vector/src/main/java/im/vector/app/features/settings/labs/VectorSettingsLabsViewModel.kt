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

package im.vector.app.features.settings.labs

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.session.clientinfo.DeleteMatrixClientInfoUseCase
import im.vector.app.core.session.clientinfo.UpdateMatrixClientInfoUseCase
import kotlinx.coroutines.launch

class VectorSettingsLabsViewModel @AssistedInject constructor(
        @Assisted initialState: VectorSettingsLabsViewState,
        private val activeSessionHolder: ActiveSessionHolder,
        private val updateMatrixClientInfoUseCase: UpdateMatrixClientInfoUseCase,
        private val deleteMatrixClientInfoUseCase: DeleteMatrixClientInfoUseCase,
) : VectorViewModel<VectorSettingsLabsViewState, VectorSettingsLabsAction, EmptyViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<VectorSettingsLabsViewModel, VectorSettingsLabsViewState> {
        override fun create(initialState: VectorSettingsLabsViewState): VectorSettingsLabsViewModel
    }

    companion object : MavericksViewModelFactory<VectorSettingsLabsViewModel, VectorSettingsLabsViewState> by hiltMavericksViewModelFactory()

    override fun handle(action: VectorSettingsLabsAction) {
        when (action) {
            VectorSettingsLabsAction.UpdateClientInfo -> handleUpdateClientInfo()
            VectorSettingsLabsAction.DeleteRecordedClientInfo -> handleDeleteRecordedClientInfo()
        }
    }

    private fun handleUpdateClientInfo() {
        viewModelScope.launch {
            activeSessionHolder.getSafeActiveSession()
                    ?.let { session ->
                        updateMatrixClientInfoUseCase.execute(session)
                    }
        }
    }

    private fun handleDeleteRecordedClientInfo() {
        viewModelScope.launch {
            deleteMatrixClientInfoUseCase.execute()
        }
    }
}
