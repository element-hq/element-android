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

package im.vector.app.features.settings.devtools

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.app.core.platform.EmptyAction
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.Event

data class GossipingEventsPaperTrailState(
        val events: Async<List<Event>> = Uninitialized
) : MvRxState

class GossipingEventsPaperTrailViewModel @AssistedInject constructor(@Assisted initialState: GossipingEventsPaperTrailState,
                                                                     private val session: Session)
    : VectorViewModel<GossipingEventsPaperTrailState, EmptyAction, EmptyViewEvents>(initialState) {

    init {
        refresh()
    }

    fun refresh() {
        setState {
            copy(events = Loading())
        }
        viewModelScope.launch {
            session.cryptoService().getGossipingEventsTrail().let {
                val sorted = it.sortedByDescending { it.ageLocalTs }
                setState {
                    copy(events = Success(sorted))
                }
            }
        }
    }

    override fun handle(action: EmptyAction) {}

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: GossipingEventsPaperTrailState): GossipingEventsPaperTrailViewModel
    }

    companion object : MvRxViewModelFactory<GossipingEventsPaperTrailViewModel, GossipingEventsPaperTrailState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: GossipingEventsPaperTrailState): GossipingEventsPaperTrailViewModel? {
            val fragment: GossipingEventsPaperTrailFragment = (viewModelContext as FragmentViewModelContext).fragment()

            return fragment.viewModelFactory.create(state)
        }
    }
}
