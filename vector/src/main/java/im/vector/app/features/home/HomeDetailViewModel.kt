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

package im.vector.app.features.home

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.HasScreenInjector
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.grouplist.SelectedGroupDataSource
import im.vector.app.features.ui.UiStateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.query.RoomCategoryFilter
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.internal.util.awaitCallback
import org.matrix.android.sdk.rx.asObservable
import org.matrix.android.sdk.rx.rx
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * View model used to update the home bottom bar notification counts, observe the sync state and
 * change the selected room list view
 */
class HomeDetailViewModel @AssistedInject constructor(@Assisted initialState: HomeDetailViewState,
                                                      private val session: Session,
                                                      private val uiStateRepository: UiStateRepository,
                                                      private val selectedGroupStore: SelectedGroupDataSource,
                                                      private val stringProvider: StringProvider)
    : VectorViewModel<HomeDetailViewState, HomeDetailAction, EmptyViewEvents>(initialState) {

    @AssistedFactory
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

    override fun handle(action: HomeDetailAction) {
        when (action) {
            is HomeDetailAction.SwitchDisplayMode -> handleSwitchDisplayMode(action)
            HomeDetailAction.MarkAllRoomsRead     -> handleMarkAllRoomsRead()
        }
    }

    private fun handleSwitchDisplayMode(action: HomeDetailAction.SwitchDisplayMode) = withState { state ->
        if (state.displayMode != action.displayMode) {
            setState {
                copy(displayMode = action.displayMode)
            }

            uiStateRepository.storeDisplayMode(action.displayMode)
        }
    }

    // PRIVATE METHODS *****************************************************************************

    private fun handleMarkAllRoomsRead() = withState { _ ->
        // questionable to use viewmodelscope
        viewModelScope.launch(Dispatchers.Default) {
            val roomIds = session.getRoomSummaries(
                    roomSummaryQueryParams {
                        memberships = listOf(Membership.JOIN)
                        roomCategoryFilter = RoomCategoryFilter.ONLY_WITH_NOTIFICATIONS
                    }
            )
                    .map { it.roomId }
            try {
                awaitCallback<Unit> {
                    session.markAllAsRead(roomIds, it)
                }
            } catch (failure: Throwable) {
                Timber.d(failure, "Failed to mark all as read")
            }
        }
    }

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
        session.getPagedRoomSummariesLive(
                roomSummaryQueryParams {
                    memberships = Membership.activeMemberships()
                }
        )
                .asObservable()
                .throttleFirst(300, TimeUnit.MILLISECONDS)
                .subscribe {
                    val dmInvites = session.getRoomSummaries(
                            roomSummaryQueryParams {
                                memberships = listOf(Membership.INVITE)
                                roomCategoryFilter = RoomCategoryFilter.ONLY_DM
                            }
                    ).size

                    val roomsInvite = session.getRoomSummaries(
                            roomSummaryQueryParams {
                                memberships = listOf(Membership.INVITE)
                                roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
                            }
                    ).size

                    val dmRooms = session.getNotificationCountForRooms(
                            roomSummaryQueryParams {
                                memberships = listOf(Membership.JOIN)
                                roomCategoryFilter = RoomCategoryFilter.ONLY_DM
                            }
                    )

                    val otherRooms = session.getNotificationCountForRooms(
                            roomSummaryQueryParams {
                                memberships = listOf(Membership.JOIN)
                                roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
                            }
                    )

                    setState {
                        copy(
                                notificationCountCatchup = dmRooms.totalCount + otherRooms.totalCount + roomsInvite + dmInvites,
                                notificationHighlightCatchup = dmRooms.isHighlight || otherRooms.isHighlight,
                                notificationCountPeople = dmRooms.totalCount + dmInvites,
                                notificationHighlightPeople = dmRooms.isHighlight || dmInvites > 0,
                                notificationCountRooms = otherRooms.totalCount + roomsInvite,
                                notificationHighlightRooms = otherRooms.isHighlight || roomsInvite > 0,
                                hasUnreadMessages = dmRooms.totalCount + otherRooms.totalCount > 0
                        )
                    }
                }
                .disposeOnClear()
    }
}
