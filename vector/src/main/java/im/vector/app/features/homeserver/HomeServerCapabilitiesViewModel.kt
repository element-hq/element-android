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

package im.vector.app.features.homeserver

import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import im.vector.app.core.di.HasScreenInjector
import im.vector.app.core.platform.EmptyAction
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilities

class HomeServerCapabilitiesViewModel(initialState: HomeServerCapabilitiesViewState)
    : VectorViewModel<HomeServerCapabilitiesViewState, EmptyAction, EmptyViewEvents>(initialState) {

    companion object : MvRxViewModelFactory<HomeServerCapabilitiesViewModel, HomeServerCapabilitiesViewState> {

        override fun initialState(viewModelContext: ViewModelContext): HomeServerCapabilitiesViewState? {
            val session = (viewModelContext.activity as HasScreenInjector).injector().activeSessionHolder().getSafeActiveSession()
            return HomeServerCapabilitiesViewState(
                    capabilities = session?.getHomeServerCapabilities() ?: HomeServerCapabilities()
            )
        }
    }

    override fun handle(action: EmptyAction) {}
}
