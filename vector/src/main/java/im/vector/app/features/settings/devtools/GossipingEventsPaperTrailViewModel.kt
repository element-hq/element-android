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

import androidx.lifecycle.asFlow
import androidx.paging.PagedList
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyAction
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.Event

data class GossipingEventsPaperTrailState(
        val events: Async<PagedList<Event>> = Uninitialized
) : MavericksState

class GossipingEventsPaperTrailViewModel @AssistedInject constructor(@Assisted initialState: GossipingEventsPaperTrailState,
                                                                     private val session: Session) :
    VectorViewModel<GossipingEventsPaperTrailState, EmptyAction, EmptyViewEvents>(initialState) {

    init {
        refresh()
    }

    fun refresh() {
        setState {
            copy(events = Loading())
        }
        session.cryptoService().getGossipingEventsTrail()
                .asFlow()
                .execute {
                    copy(events = it)
                }
    }

    override fun handle(action: EmptyAction) {}

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<GossipingEventsPaperTrailViewModel, GossipingEventsPaperTrailState> {
        override fun create(initialState: GossipingEventsPaperTrailState): GossipingEventsPaperTrailViewModel
    }

    companion object : MavericksViewModelFactory<GossipingEventsPaperTrailViewModel, GossipingEventsPaperTrailState> by hiltMavericksViewModelFactory()
}
