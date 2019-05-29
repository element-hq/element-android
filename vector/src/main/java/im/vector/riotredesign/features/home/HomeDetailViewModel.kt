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

package im.vector.riotredesign.features.home

import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.rx.rx
import im.vector.riotredesign.core.platform.VectorViewModel
import org.koin.android.ext.android.get

class HomeDetailViewModel(initialState: HomeDetailViewState,
                          private val session: Session)
    : VectorViewModel<HomeDetailViewState>(initialState) {

    companion object : MvRxViewModelFactory<HomeDetailViewModel, HomeDetailViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: HomeDetailViewState): HomeDetailViewModel? {
            val currentSession = viewModelContext.activity.get<Session>()
            return HomeDetailViewModel(state, currentSession)
        }
    }

    init {
        observeRoomSummaries()
    }

    // PRIVATE METHODS *****************************************************************************

    // TODO Filter with selected group
    private fun observeRoomSummaries() {
        session
                .rx()
                .liveRoomSummaries()
                .execute { state ->
                    state.invoke()?.let { summaries ->
                        val peopleNotifications = summaries
                                .filter { it.isDirect }
                                .map { it.notificationCount }
                                .reduce { acc, i -> acc + i }
                        val peopleHasHighlight = summaries
                                .filter { it.isDirect }
                                .any { it.highlightCount > 0 }

                        val roomsNotifications = summaries
                                .filter { !it.isDirect }
                                .map { it.notificationCount }
                                .reduce { acc, i -> acc + i }
                        val roomsHasHighlight = summaries
                                .filter { !it.isDirect }
                                .any { it.highlightCount > 0 }

                        copy(
                                notificationCountCatchup = peopleNotifications + roomsNotifications,
                                notificationHighlightCatchup = peopleHasHighlight || roomsHasHighlight,
                                notificationCountPeople = peopleNotifications,
                                notificationHighlightPeople = peopleHasHighlight,
                                notificationCountRooms = roomsNotifications,
                                notificationHighlightRooms = roomsHasHighlight
                        )
                    } ?: run {
                        // No change
                        copy()
                    }
                }
    }

}