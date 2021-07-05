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

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.ViewModelContext
import im.vector.app.AppStateHandler
import im.vector.app.RoomGroupingMethod
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.invite.AutoAcceptInvites
import im.vector.app.features.settings.VectorPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.UpdatableLivePageResult
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.tag.RoomTag
import org.matrix.android.sdk.api.session.room.state.isPublic
import org.matrix.android.sdk.rx.rx
import timber.log.Timber
import javax.inject.Inject

class RoomListViewModel @Inject constructor(
        initialState: RoomListViewState,
        private val session: Session,
        private val stringProvider: StringProvider,
        private val appStateHandler: AppStateHandler,
        private val vectorPreferences: VectorPreferences,
        private val autoAcceptInvites: AutoAcceptInvites
) : VectorViewModel<RoomListViewState, RoomListAction, RoomListViewEvents>(initialState) {

    interface Factory {
        fun create(initialState: RoomListViewState): RoomListViewModel
    }

    private var updatableQuery: UpdatableLivePageResult? = null

    private val suggestedRoomJoiningState: MutableLiveData<Map<String, Async<Unit>>> = MutableLiveData(emptyMap())

    interface ActiveSpaceQueryUpdater {
        fun updateForSpaceId(roomId: String?)
    }

    enum class SpaceFilterStrategy {
        /**
         * Filter the rooms if they are part of the current space (children and grand children).
         * If current space is null, will return orphan rooms only
         */
        ORPHANS_IF_SPACE_NULL,
        /**
         * Special case when we don't want to discriminate rooms when current space is null.
         * In this case return all.
         */
        ALL_IF_SPACE_NULL,
        /** Do not filter based on space*/
        NONE
    }

    init {
        observeMembershipChanges()

        appStateHandler.selectedRoomGroupingObservable
                .distinctUntilChanged()
                .execute {
                    copy(
                            currentRoomGrouping = it.invoke()?.orNull()?.let { Success(it) } ?: Loading()
                    )
                }

        session.rx().liveUser(session.myUserId)
                .map { it.getOrNull()?.getBestName() }
                .distinctUntilChanged()
                .execute {
                    copy(
                            currentUserName = it.invoke() ?: session.myUserId
                    )
                }
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
        if (appStateHandler.getCurrentRoomGroupingMethod() is RoomGroupingMethod.BySpace) {
            SpaceRoomListSectionBuilder(
                    session,
                    stringProvider,
                    appStateHandler,
                    viewModelScope,
                    suggestedRoomJoiningState,
                    autoAcceptInvites,
                    {
                        it.disposeOnClear()
                    },
                    {
                        updatableQuery = it
                    },
                    vectorPreferences.labsSpacesOnlyOrphansInHome()
            ).buildSections(initialState.displayMode)
        } else {
            GroupRoomListSectionBuilder(
                    session,
                    stringProvider,
                    viewModelScope,
                    appStateHandler,
                    autoAcceptInvites,
                    {
                        it.disposeOnClear()
                    },
                    {
                        updatableQuery = it
                    }
            ).buildSections(initialState.displayMode)
        }
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
            is RoomListAction.JoinSuggestedRoom           -> handleJoinSuggestedRoom(action)
            is RoomListAction.ShowRoomDetails             -> handleShowRoomDetails(action)
        }.exhaustive
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
        updatableQuery?.updateQuery {
            it.copy(
                    displayName = QueryStringValue.Contains(action.filter, QueryStringValue.Case.INSENSITIVE)
            )
        }
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

    private fun handleJoinSuggestedRoom(action: RoomListAction.JoinSuggestedRoom) {
        suggestedRoomJoiningState.postValue(suggestedRoomJoiningState.value.orEmpty().toMutableMap().apply {
            this[action.roomId] = Loading()
        }.toMap())

        viewModelScope.launch {
            try {
                session.joinRoom(action.roomId, null, action.viaServers ?: emptyList())

                suggestedRoomJoiningState.postValue(suggestedRoomJoiningState.value.orEmpty().toMutableMap().apply {
                    this[action.roomId] = Success(Unit)
                }.toMap())
            } catch (failure: Throwable) {
                suggestedRoomJoiningState.postValue(suggestedRoomJoiningState.value.orEmpty().toMutableMap().apply {
                    this[action.roomId] = Fail(failure)
                }.toMap())
            }
        }
    }

    private fun handleShowRoomDetails(action: RoomListAction.ShowRoomDetails) {
        session.permalinkService().createRoomPermalink(action.roomId, action.viaServers)?.let {
            _viewEvents.post(RoomListViewEvents.NavigateToMxToBottomSheet(it))
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
            RoomTag.ROOM_TAG_FAVOURITE -> RoomTag.ROOM_TAG_LOW_PRIORITY
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
