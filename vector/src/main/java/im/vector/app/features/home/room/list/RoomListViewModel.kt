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

package im.vector.app.features.home.room.list

import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import im.vector.app.R
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.RoomListDisplayMode
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.query.RoomCategoryFilter
import org.matrix.android.sdk.api.query.RoomTagQueryFilter
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.RoomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.UpdatableFilterLivePageResult
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.tag.RoomTag
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.state.isPublic
import org.matrix.android.sdk.rx.asObservable
import org.matrix.android.sdk.rx.rx
import timber.log.Timber
import javax.inject.Inject

class RoomListViewModel @Inject constructor(
        initialState: RoomListViewState,
        private val session: Session,
        private val stringProvider: StringProvider
) : VectorViewModel<RoomListViewState, RoomListAction, RoomListViewEvents>(initialState) {

    interface Factory {
        fun create(initialState: RoomListViewState): RoomListViewModel
    }

    private var updatableQuery: UpdatableFilterLivePageResult? = null

    init {
        observeMembershipChanges()
    }

    private fun observeMembershipChanges() {
        session.rx()
                .liveRoomChangeMembershipState()
                .subscribe {
                    setState { copy(roomMembershipChanges = it) }
                }
                .disposeOnClear()
    }

    companion object : MvRxViewModelFactory<RoomListViewModel, RoomListViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: RoomListViewState): RoomListViewModel? {
            val fragment: RoomListFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.roomListViewModelFactory.create(state)
        }
    }

    val sections: List<RoomsSection> by lazy {
        val sections = mutableListOf<RoomsSection>()
        if (initialState.displayMode == RoomListDisplayMode.PEOPLE) {
            addSection(sections, R.string.invitations_header, true) {
                it.memberships = listOf(Membership.INVITE)
                it.roomCategoryFilter = RoomCategoryFilter.ONLY_DM
            }

            addSection(sections, R.string.bottom_action_favourites) {
                it.memberships = listOf(Membership.JOIN)
                it.roomCategoryFilter = RoomCategoryFilter.ONLY_DM
                it.roomTagQueryFilter = RoomTagQueryFilter(true, null, null)
            }

            addSection(sections, R.string.bottom_action_people_x) {
                it.memberships = listOf(Membership.JOIN)
                it.roomCategoryFilter = RoomCategoryFilter.ONLY_DM
            }
        } else if (initialState.displayMode == RoomListDisplayMode.ROOMS) {
            addSection(sections, R.string.invitations_header, true) {
                it.memberships = listOf(Membership.INVITE)
                it.roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
            }

            addSection(sections, R.string.bottom_action_favourites) {
                it.memberships = listOf(Membership.JOIN)
                it.roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
                it.roomTagQueryFilter = RoomTagQueryFilter(true, null, null)
            }

            addSection(sections, R.string.bottom_action_rooms) {
                it.memberships = listOf(Membership.JOIN)
                it.roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
                it.roomTagQueryFilter = RoomTagQueryFilter(false, false, false)
            }

            addSection(sections, R.string.low_priority_header) {
                it.memberships = listOf(Membership.JOIN)
                it.roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
                it.roomTagQueryFilter = RoomTagQueryFilter(null, true, null)
            }

            addSection(sections, R.string.system_alerts_header) {
                it.memberships = listOf(Membership.JOIN)
                it.roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
                it.roomTagQueryFilter = RoomTagQueryFilter(null, null, true)
            }
        } else if (initialState.displayMode == RoomListDisplayMode.FILTERED) {
            withQueryParams(
                    {
                        it.memberships = Membership.activeMemberships()
                    },
                    { qpm ->
                        val name = stringProvider.getString(R.string.bottom_action_rooms)
                        session.getFilteredPagedRoomSummariesLive(qpm)
                                .let { updatableFilterLivePageResult ->
                                    updatableQuery = updatableFilterLivePageResult
                                    sections.add(RoomsSection(name, updatableFilterLivePageResult.livePagedList))
                                }
                    }
            )
        } else if (initialState.displayMode == RoomListDisplayMode.NOTIFICATIONS) {
            addSection(sections, R.string.invitations_header, true) {
                it.memberships = listOf(Membership.INVITE)
                it.roomCategoryFilter = RoomCategoryFilter.ALL
            }

            addSection(sections, R.string.bottom_action_rooms, true) {
                it.memberships = listOf(Membership.JOIN)
                it.roomCategoryFilter = RoomCategoryFilter.ONLY_WITH_NOTIFICATIONS
            }
        }

        sections
    }

    override fun handle(action: RoomListAction) {
        when (action) {
            is RoomListAction.SelectRoom                  -> handleSelectRoom(action)
            is RoomListAction.AcceptInvitation            -> handleAcceptInvitation(action)
            is RoomListAction.RejectInvitation            -> handleRejectInvitation(action)
            is RoomListAction.FilterWith                  -> handleFilter(action)
            is RoomListAction.LeaveRoom                   -> handleLeaveRoom(action)
            is RoomListAction.ChangeRoomNotificationState -> handleChangeNotificationMode(action)
            is RoomListAction.ToggleTag                   -> handleToggleTag(action)
            is RoomListAction.ToggleSection               -> handleToggleSection(action.section)
        }.exhaustive
    }

    private fun addSection(sections: MutableList<RoomsSection>,
                           @StringRes nameRes: Int,
                           notifyOfLocalEcho: Boolean = false,
                           query: (RoomSummaryQueryParams.Builder) -> Unit) {
        withQueryParams(
                { query.invoke(it) },
                { roomQueryParams ->

                    val name = stringProvider.getString(nameRes)
                    session.getPagedRoomSummariesLive(roomQueryParams)
                            .let { livePagedList ->

                                // use it also as a source to update count
                                livePagedList.asObservable()
                                        .observeOn(Schedulers.computation())
                                        .subscribe {
                                            sections.find { it.sectionName == name }
                                                    ?.notificationCount
                                                    ?.postValue(session.getNotificationCountForRooms(roomQueryParams))
                                        }
                                        .disposeOnClear()

                                sections.add(
                                        RoomsSection(
                                                sectionName = name,
                                                livePages = livePagedList,
                                                notifyOfLocalEcho = notifyOfLocalEcho
                                        )
                                )
                            }
                }
        )
    }

    private fun withQueryParams(builder: (RoomSummaryQueryParams.Builder) -> Unit, block: (RoomSummaryQueryParams) -> Unit) {
        RoomSummaryQueryParams.Builder()
                .apply { builder.invoke(this) }
                .build()
                .let { block(it) }
    }

    fun isPublicRoom(roomId: String): Boolean {
        return session.getRoom(roomId)?.isPublic().orFalse()
    }

    // PRIVATE METHODS *****************************************************************************

    private fun handleSelectRoom(action: RoomListAction.SelectRoom) = withState {
        _viewEvents.post(RoomListViewEvents.SelectRoom(action.roomSummary))
    }

    private fun handleToggleSection(roomSection: RoomsSection) {
        roomSection.isExpanded.postValue(!roomSection.isExpanded.value.orFalse())
        /* TODO Cleanup if it is working
        sections.find { it.sectionName == roomSection.sectionName }
                ?.let { section ->
                    section.isExpanded.postValue(!section.isExpanded.value.orFalse())
                }
         */
    }

    private fun handleFilter(action: RoomListAction.FilterWith) {
        setState {
            copy(
                    roomFilter = action.filter
            )
        }
        updatableQuery?.updateQuery(
                roomSummaryQueryParams {
                    memberships = Membership.activeMemberships()
                    displayName = QueryStringValue.Contains(action.filter, QueryStringValue.Case.INSENSITIVE)
                }
        )
    }

    private fun handleAcceptInvitation(action: RoomListAction.AcceptInvitation) = withState { state ->
        val roomId = action.roomSummary.roomId
        val roomMembershipChange = state.roomMembershipChanges[roomId]
        if (roomMembershipChange?.isInProgress().orFalse()) {
            // Request already sent, should not happen
            Timber.w("Try to join an already joining room. Should not happen")
            return@withState
        }

        // quick echo
        setState {
            copy(
                    roomMembershipChanges = roomMembershipChanges.mapValues {
                        if (it.key == roomId) {
                            ChangeMembershipState.Joining
                        } else {
                            it.value
                        }
                    }
            )
        }

        val room = session.getRoom(roomId) ?: return@withState
        viewModelScope.launch {
            try {
                room.join()
                // We do not update the joiningRoomsIds here, because, the room is not joined yet regarding the sync data.
                // Instead, we wait for the room to be joined
            } catch (failure: Throwable) {
                // Notify the user
                _viewEvents.post(RoomListViewEvents.Failure(failure))
            }
        }
    }

    private fun handleRejectInvitation(action: RoomListAction.RejectInvitation) = withState { state ->
        val roomId = action.roomSummary.roomId
        val roomMembershipChange = state.roomMembershipChanges[roomId]
        if (roomMembershipChange?.isInProgress().orFalse()) {
            // Request already sent, should not happen
            Timber.w("Try to left an already leaving or joining room. Should not happen")
            return@withState
        }

        val room = session.getRoom(roomId) ?: return@withState
        viewModelScope.launch {
            try {
                room.leave(null)
                // We do not update the rejectingRoomsIds here, because, the room is not rejected yet regarding the sync data.
                // Instead, we wait for the room to be rejected
                // Known bug: if the user is invited again (after rejecting the first invitation), the loading will be displayed instead of the buttons.
                // If we update the state, the button will be displayed again, so it's not ideal...
            } catch (failure: Throwable) {
                // Notify the user
                _viewEvents.post(RoomListViewEvents.Failure(failure))
            }
        }
    }

    private fun handleChangeNotificationMode(action: RoomListAction.ChangeRoomNotificationState) {
        val room = session.getRoom(action.roomId)
        if (room != null) {
            viewModelScope.launch {
                try {
                    room.setRoomNotificationState(action.notificationState)
                } catch (failure: Exception) {
                    _viewEvents.post(RoomListViewEvents.Failure(failure))
                }
            }
        }
    }

    private fun handleToggleTag(action: RoomListAction.ToggleTag) {
        session.getRoom(action.roomId)?.let { room ->
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    if (room.roomSummary()?.hasTag(action.tag) == false) {
                        // Favorite and low priority tags are exclusive, so maybe delete the other tag first
                        action.tag.otherTag()
                                ?.takeIf { room.roomSummary()?.hasTag(it).orFalse() }
                                ?.let { tagToRemove ->
                                    room.deleteTag(tagToRemove)
                                }

                        // Set the tag. We do not handle the order for the moment
                        room.addTag(action.tag, 0.5)
                    } else {
                        room.deleteTag(action.tag)
                    }
                } catch (failure: Throwable) {
                    _viewEvents.post(RoomListViewEvents.Failure(failure))
                }
            }
        }
    }

    private fun String.otherTag(): String? {
        return when (this) {
            RoomTag.ROOM_TAG_FAVOURITE    -> RoomTag.ROOM_TAG_LOW_PRIORITY
            RoomTag.ROOM_TAG_LOW_PRIORITY -> RoomTag.ROOM_TAG_FAVOURITE
            else                          -> null
        }
    }

    private fun handleLeaveRoom(action: RoomListAction.LeaveRoom) {
        _viewEvents.post(RoomListViewEvents.Loading(null))
        val room = session.getRoom(action.roomId) ?: return
        viewModelScope.launch {
            val value = runCatching { room.leave(null) }
                    .fold({ RoomListViewEvents.Done }, { RoomListViewEvents.Failure(it) })
            _viewEvents.post(value)
        }
    }
}
