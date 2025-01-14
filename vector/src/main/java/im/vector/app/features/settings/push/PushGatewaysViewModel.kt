/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.push

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.pushers.Pusher
import org.matrix.android.sdk.flow.flow

data class PushGatewayViewState(
        val pushGateways: Async<List<Pusher>> = Uninitialized
) : MavericksState

class PushGatewaysViewModel @AssistedInject constructor(
        @Assisted initialState: PushGatewayViewState,
        private val session: Session
) :
        VectorViewModel<PushGatewayViewState, PushGatewayAction, PushGatewayViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<PushGatewaysViewModel, PushGatewayViewState> {
        override fun create(initialState: PushGatewayViewState): PushGatewaysViewModel
    }

    companion object : MavericksViewModelFactory<PushGatewaysViewModel, PushGatewayViewState> by hiltMavericksViewModelFactory()

    init {
        observePushers()
        // Force a refresh
        session.pushersService().refreshPushers()
    }

    private fun observePushers() {
        session.flow()
                .livePushers()
                .execute {
                    copy(pushGateways = it)
                }
    }

    override fun handle(action: PushGatewayAction) {
        when (action) {
            is PushGatewayAction.Refresh -> handleRefresh()
            is PushGatewayAction.RemovePusher -> removePusher(action.pusher)
        }
    }

    private fun removePusher(pusher: Pusher) {
        viewModelScope.launch {
            kotlin.runCatching {
                session.pushersService().removePusher(pusher)
            }.onFailure {
                _viewEvents.post(PushGatewayViewEvents.RemovePusherFailed(it))
            }
        }
    }

    private fun handleRefresh() {
        session.pushersService().refreshPushers()
    }
}
