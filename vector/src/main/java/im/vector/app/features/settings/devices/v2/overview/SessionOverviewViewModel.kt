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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.flow.flow

class SessionOverviewViewModel @AssistedInject constructor(
        @Assisted val initialState: SessionOverviewViewState,
        private val session: Session,
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
        observePushers(initialState.deviceId)
    }

    private fun observeSessionInfo(deviceId: String) {
        getDeviceFullInfoUseCase.execute(deviceId)
                .onEach { setState { copy(deviceInfo = Success(it)) } }
                .launchIn(viewModelScope)
    }

    private fun observePushers(deviceId: String) {
        session.flow()
                .livePushers()
                .map { it.filter { pusher -> pusher.deviceId == deviceId }}
                .execute { copy(pushers = it) }
    }

    override fun handle(action: SessionOverviewAction) {
        when (action) {
            is SessionOverviewAction.TogglePushNotifications -> handleTogglePusherAction(action)
        }
    }

    private fun handleTogglePusherAction(action: SessionOverviewAction.TogglePushNotifications) {
        viewModelScope.launch {
            val devicePushers = awaitState().pushers.invoke()?.filter { it.deviceId == action.deviceId }
            devicePushers?.forEach { pusher ->
                session.pushersService().togglePusher(pusher, action.enabled)
            }
        }
    }
}
