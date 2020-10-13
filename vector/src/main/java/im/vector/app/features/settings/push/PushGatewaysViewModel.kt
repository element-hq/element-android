/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.settings.push

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.pushers.Pusher
import org.matrix.android.sdk.rx.RxSession

data class PushGatewayViewState(
        val pushGateways: Async<List<Pusher>> = Uninitialized
) : MvRxState

class PushGatewaysViewModel @AssistedInject constructor(@Assisted initialState: PushGatewayViewState,
                                                        private val session: Session)
    : VectorViewModel<PushGatewayViewState, PushGatewayAction, EmptyViewEvents>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: PushGatewayViewState): PushGatewaysViewModel
    }

    companion object : MvRxViewModelFactory<PushGatewaysViewModel, PushGatewayViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: PushGatewayViewState): PushGatewaysViewModel? {
            val fragment: PushGatewaysFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.pushGatewaysViewModelFactory.create(state)
        }
    }

    init {
        observePushers()
        // Force a refresh
        session.refreshPushers()
    }

    private fun observePushers() {
        RxSession(session)
                .livePushers()
                .execute {
                    copy(pushGateways = it)
                }
    }

    override fun handle(action: PushGatewayAction) {
        when (action) {
            is PushGatewayAction.Refresh -> handleRefresh()
        }.exhaustive
    }

    private fun handleRefresh() {
        session.refreshPushers()
    }
}
