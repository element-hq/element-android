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

package im.vector.riotx.features.home

import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.rx.rx
import im.vector.riotx.core.di.HasScreenInjector
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.features.home.group.SelectedGroupStore
import im.vector.riotx.features.home.room.list.RoomListFragment
import im.vector.riotx.features.ui.UiStateRepository
import io.reactivex.schedulers.Schedulers

/**
 * View model used to update the home bottom bar notification counts, observe the sync state and
 * change the selected room list view
 */
class HomeDetailViewModel @AssistedInject constructor(@Assisted initialState: HomeDetailViewState,
                                                      private val session: Session,
                                                      private val uiStateRepository: UiStateRepository,
                                                      private val selectedGroupStore: SelectedGroupStore,
                                                      private val homeRoomListStore: HomeRoomListObservableStore)
    : VectorViewModel<HomeDetailViewState>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: HomeDetailViewState): HomeDetailViewModel
    }

    companion object : MvRxViewModelFactory<HomeDetailViewModel, HomeDetailViewState> {

        override fun initialState(viewModelContext: ViewModelContext): HomeDetailViewState? {
            val uiStateRepository = (viewModelContext.activity as HasScreenInjector).injector().uiStateRepository()
            return HomeDetailViewState(
                    displayMode = uiStateRepository.getDisplayMode()
            )
        }

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: HomeDetailViewState): HomeDetailViewModel? {
            val fragment: HomeDetailFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.homeDetailViewModelFactory.create(state)
        }
    }

    init {
        observeSyncState()
        observeSelectedGroupStore()
        observeRoomSummaries()
    }

    fun switchDisplayMode(displayMode: RoomListFragment.DisplayMode) = withState { state ->
        if (state.displayMode != displayMode) {
            setState {
                copy(displayMode = displayMode)
            }

            uiStateRepository.storeDisplayMode(displayMode)
        }
    }

    // PRIVATE METHODS *****************************************************************************

    private fun observeSyncState() {
        session.rx()
                .liveSyncState()
                .subscribe { syncState ->
                    setState {
                        copy(syncState = syncState)
                    }
                }
                .disposeOnClear()
    }

    private fun observeSelectedGroupStore() {
        selectedGroupStore
                .observe()
                .subscribe {
                    setState {
                        copy(groupSummary = it)
                    }
                }
                .disposeOnClear()
    }

    private fun observeRoomSummaries() {
        homeRoomListStore
                .observe()
                .observeOn(Schedulers.computation())
                .subscribe { list ->
                    list.let { summaries ->
                        val peopleNotifications = summaries
                                .filter { it.isDirect }
                                .map { it.notificationCount }
                                .takeIf { it.isNotEmpty() }
                                ?.sumBy { i -> i }
                                ?: 0
                        val peopleHasHighlight = summaries
                                .filter { it.isDirect }
                                .any { it.highlightCount > 0 }

                        val roomsNotifications = summaries
                                .filter { !it.isDirect }
                                .map { it.notificationCount }
                                .takeIf { it.isNotEmpty() }
                                ?.sumBy { i -> i }
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