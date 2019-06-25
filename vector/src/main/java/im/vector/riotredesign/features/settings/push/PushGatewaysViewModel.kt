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

package im.vector.riotredesign.features.settings.push

import androidx.lifecycle.Observer
import com.airbnb.mvrx.*
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.pushers.Pusher
import im.vector.riotredesign.core.platform.VectorViewModel
import org.koin.android.ext.android.get


data class PushGatewayViewState(
        val pushGateways: Async<List<Pusher>> = Uninitialized
) : MvRxState

class PushGatewaysViewModel(initialState: PushGatewayViewState) : VectorViewModel<PushGatewayViewState>(initialState) {

    companion object : MvRxViewModelFactory<PushGatewaysViewModel, PushGatewayViewState> {

        override fun create(viewModelContext: ViewModelContext, state: PushGatewayViewState): PushGatewaysViewModel? {
            val session = viewModelContext.activity.get<Session>()
            val fragment = (viewModelContext as FragmentViewModelContext).fragment

            val livePushers = session.livePushers()

            val viewModel = PushGatewaysViewModel(state)

            livePushers.observe(fragment, Observer {
                viewModel.setState {
                    PushGatewayViewState(Success(it))
                }
            })
            return viewModel
        }

    }

}