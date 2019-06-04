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
import im.vector.riotredesign.core.platform.VectorViewModel
import org.koin.android.ext.android.get

/**
 * View model used to update the home bottom bar notification counts
 */
class HomeDetailViewModel(initialState: HomeDetailViewState,
                          private val homeRoomListStore: HomeRoomListObservableStore)
    : VectorViewModel<HomeDetailViewState>(initialState) {

    companion object : MvRxViewModelFactory<HomeDetailViewModel, HomeDetailViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: HomeDetailViewState): HomeDetailViewModel? {
            val homeRoomListStore = viewModelContext.activity.get<HomeRoomListObservableStore>()
            return HomeDetailViewModel(state, homeRoomListStore)
        }
    }

    init {
        observeRoomSummaries()
    }

    // PRIVATE METHODS *****************************************************************************

    private fun observeRoomSummaries() {
        homeRoomListStore
                .observe()
                .subscribe { list ->
                    list.let { summaries ->
                        val peopleNotifications = summaries
                                .filter { it.isDirect }
                                .map { it.notificationCount }
                                .takeIf { it.isNotEmpty() }
                                ?.reduce { acc, i -> acc + i }
                                ?: 0
                        val peopleHasHighlight = summaries
                                .filter { it.isDirect }
                                .any { it.highlightCount > 0 }

                        val roomsNotifications = summaries
                                .filter { !it.isDirect }
                                .map { it.notificationCount }
                                .takeIf { it.isNotEmpty() }
                                ?.reduce { acc, i -> acc + i }
                                ?: 0
                        val roomsHasHighlight = summaries
                                .filter { !it.isDirect }
                                .any { it.highlightCount > 0 }

                        setState {
                            copy(
                                    notificationCountCatchup = peopleNotifications + roomsNotifications,
                                    notificationHighlightCatchup = peopleHasHighlight || roomsHasHighlight,
                                    notificationCountPeople = peopleNotifications,
                                    notificationHighlightPeople = peopleHasHighlight,
                                    notificationCountRooms = roomsNotifications,
                                    notificationHighlightRooms = roomsHasHighlight
                            )
                        }
                    }
                }
                .disposeOnClear()
    }

}