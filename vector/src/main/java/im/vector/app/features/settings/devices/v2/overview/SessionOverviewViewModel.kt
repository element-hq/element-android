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

package im.vector.app.features.settings.devices.v2.overview

import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.api.session.Session

class SessionOverviewViewModel @AssistedInject constructor(
        @Assisted val initialState: SessionOverviewViewState,
        session: Session,
        private val getDeviceFullInfoUseCase: GetDeviceFullInfoUseCase,
) : VectorViewModel<SessionOverviewViewState, SessionOverviewAction, EmptyViewEvents>(initialState) {

    companion object : MavericksViewModelFactory<SessionOverviewViewModel, SessionOverviewViewState> by hiltMavericksViewModelFactory()

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<SessionOverviewViewModel, SessionOverviewViewState> {
        override fun create(initialState: SessionOverviewViewState): SessionOverviewViewModel
    }

    init {
        val currentDeviceId = session.sessionParams.deviceId.orEmpty()
        setState {
            copy(isCurrentSession = deviceId.isNotEmpty() && deviceId == currentDeviceId)
        }

        observeSessionInfo(initialState.deviceId)
    }

    private fun observeSessionInfo(deviceId: String) {
        getDeviceFullInfoUseCase.execute(deviceId)
                .mapNotNull { it.getOrNull() }
                .onEach { setState { copy(deviceInfo = Success(it)) } }
                .launchIn(viewModelScope)
    }

    override fun handle(action: SessionOverviewAction) {
        TODO("Implement when adding the first action")
    }
}
