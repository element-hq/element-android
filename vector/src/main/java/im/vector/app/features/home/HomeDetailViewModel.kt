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
import fr.gouv.tchap.features.home.TchapHomeViewEvents
import im.vector.app.AppStateHandler
import im.vector.app.RoomGroupingMethod
import im.vector.app.core.di.HasScreenInjector
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.ui.UiStateRepository
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.query.ActiveSpaceFilter
import org.matrix.android.sdk.api.query.RoomCategoryFilter
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.room.RoomSortOrder
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.api.util.toMatrixItem
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
                                                      private val matrix: Matrix,
                                                      private val rawService: RawService,
                                                      private val uiStateRepository: UiStateRepository,
                                                      private val appStateHandler: AppStateHandler)
    : VectorViewModel<HomeDetailViewState, HomeDetailAction, TchapHomeViewEvents>(initialState) {

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
        observeRoomGroupingMethod()
        observeRoomSummaries()

        session.rx().liveUser(session.myUserId).execute {
            copy(
                    myMatrixItem = it.invoke()?.getOrNull()?.toMatrixItem()
            )
        }
    }

    override fun handle(action: HomeDetailAction) {
        when (action) {
            is HomeDetailAction.SwitchDisplayMode -> handleSwitchDisplayMode(action)
            HomeDetailAction.MarkAllRoomsRead     -> handleMarkAllRoomsRead()
            is HomeDetailAction.InviteByEmail     -> handleIndividualInviteByEmail(action)
            is HomeDetailAction.CreateDiscussion -> handleCreateDiscussion(action)
            HomeDetailAction.UnauthorizedEmail   -> handleUnauthorizedEmail()
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
                session.markAllAsRead(roomIds)
            } catch (failure: Throwable) {
                Timber.d(failure, "Failed to mark all as read")
            }
        }
    }

    private fun handleIndividualInviteByEmail(action: HomeDetailAction.InviteByEmail) {
        val existingRoom = session.getExistingDirectRoomWithUser(action.email)

        setState {
            copy(
                    inviteEmail = action.email,
                    existingRoom = existingRoom
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            // Start the invite process by checking whether a Tchap account has been created for this email.
            val data = tryOrNull { session.identityService().lookUp(listOf(ThreePid.Email(action.email))) }

            if (data.isNullOrEmpty()) {
                _viewEvents.post(TchapHomeViewEvents.GetPlatform(action.email))
            } else {
                _viewEvents.post(TchapHomeViewEvents.InviteIgnoredForDiscoveredUser(action.email))
            }
        }
    }

    private fun handleUnauthorizedEmail() = withState {
        it.inviteEmail ?: return@withState

        if (it.existingRoom.isNullOrEmpty()) {
            _viewEvents.post(TchapHomeViewEvents.InviteIgnoredForUnauthorizedEmail(it.inviteEmail))
        } else {
            // Ignore the error, notify the user that the invite has been already sent
            _viewEvents.post(TchapHomeViewEvents.InviteIgnoredForExistingRoom(it.inviteEmail))
        }
    }

    private fun handleCreateDiscussion(action: HomeDetailAction.CreateDiscussion) = withState {
        it.inviteEmail ?: return@withState

        if (it.existingRoom.isNullOrEmpty()) {
            // Send the invite if the email is authorized
            viewModelScope.launch {
                createDiscussion(it.inviteEmail)
            }
        } else {
            // There is already a discussion with this email
            // We do not re-invite the NoTchapUser except if
            // the email is bound to the external instance (for which the invites may expire).
            if (action.isExternalEmail) {
                // Revoke the pending invite and leave this empty discussion, we will invite again this email.
                // We don't have a way for the moment to check if the invite expired or not...
                viewModelScope.launch {
                    try {
                        revokePendingInviteAndLeave(it.existingRoom)
                        createDiscussion(it.inviteEmail)
                    } catch (failure: Throwable) {
                        // Ignore the error, notify the user that the invite has been already sent
                        _viewEvents.post(TchapHomeViewEvents.InviteIgnoredForExistingRoom(it.inviteEmail))
                    }
                }
            } else {
                // Notify the user that the invite has been already sent
                _viewEvents.post(TchapHomeViewEvents.InviteIgnoredForExistingRoom(it.inviteEmail))
            }
        }
    }

    private suspend fun createDiscussion(email: String) {
        val roomParams = CreateRoomParams()
                .apply {
                    invite3pids.add(ThreePid.Email(email))
                    setDirectMessage()
                }

        runCatching { session.createRoom(roomParams) }.fold(
                { _ -> _viewEvents.post(TchapHomeViewEvents.InviteNoTchapUserByEmail) },
                { failure -> _viewEvents.post(TchapHomeViewEvents.Failure(failure)) }
        )
    }

    private suspend fun revokePendingInviteAndLeave(roomId: String) = withState {
        val room = session.getRoom(roomId) ?: return@withState
        val event = room.getStateEvent(EventType.STATE_ROOM_THIRD_PARTY_INVITE) ?: return@withState
        val token = event.stateKey

        viewModelScope.launch {
            if (!token.isNullOrEmpty()) {
                try {
                    room.sendStateEvent(
                            eventType = EventType.STATE_ROOM_THIRD_PARTY_INVITE,
                            stateKey = token,
                            body = emptyMap()
                    )

                    room.leave()
                } catch (failure: Throwable) {
                    throw failure
                }
            } else {
                Timber.d("unable to revoke invite (no pending invite)")
                room.leave()
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

    private fun observeRoomGroupingMethod() {
        appStateHandler.selectedRoomGroupingObservable
                .subscribe {
                    setState {
                        copy(
                                roomGroupingMethod = it.orNull() ?: RoomGroupingMethod.BySpace(null)
                        )
                    }
                }
                .disposeOnClear()
    }

    private fun observeRoomSummaries() {
        appStateHandler.selectedRoomGroupingObservable.distinctUntilChanged().switchMap {
            // we use it as a trigger to all changes in room, but do not really load
            // the actual models
            session.getPagedRoomSummariesLive(
                    roomSummaryQueryParams {
                        memberships = Membership.activeMemberships()
                    },
                    sortOrder = RoomSortOrder.NONE
            ).asObservable()
        }
                .observeOn(Schedulers.computation())
                .throttleFirst(300, TimeUnit.MILLISECONDS)
                .subscribe {
                    when (val groupingMethod = appStateHandler.getCurrentRoomGroupingMethod()) {
                        is RoomGroupingMethod.ByLegacyGroup -> {
                            // TODO!!
                        }
                        is RoomGroupingMethod.BySpace       -> {
                            val activeSpaceRoomId = groupingMethod.spaceSummary?.roomId
                            val dmInvites = session.getRoomSummaries(
                                    roomSummaryQueryParams {
                                        memberships = listOf(Membership.INVITE)
                                        roomCategoryFilter = RoomCategoryFilter.ONLY_DM
                                        activeSpaceFilter = activeSpaceRoomId?.let { ActiveSpaceFilter.ActiveSpace(it) } ?: ActiveSpaceFilter.None
                                    }
                            ).size

                            val roomsInvite = session.getRoomSummaries(
                                    roomSummaryQueryParams {
                                        memberships = listOf(Membership.INVITE)
                                        roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
                                        activeSpaceFilter = ActiveSpaceFilter.ActiveSpace(groupingMethod.spaceSummary?.roomId)
                                    }
                            ).size

                            val dmRooms = session.getNotificationCountForRooms(
                                    roomSummaryQueryParams {
                                        memberships = listOf(Membership.JOIN)
                                        roomCategoryFilter = RoomCategoryFilter.ONLY_DM
                                        activeSpaceFilter = activeSpaceRoomId?.let { ActiveSpaceFilter.ActiveSpace(it) } ?: ActiveSpaceFilter.None
                                    }
                            )

                            val otherRooms = session.getNotificationCountForRooms(
                                    roomSummaryQueryParams {
                                        memberships = listOf(Membership.JOIN)
                                        roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
                                        activeSpaceFilter = ActiveSpaceFilter.ActiveSpace(groupingMethod.spaceSummary?.roomId)
                                    }
                            )

                            setState {
                                copy(
                                        notificationCountCatchup = dmRooms.totalCount + otherRooms.totalCount + roomsInvite + dmInvites,
                                        notificationHighlightCatchup = dmRooms.isHighlight || otherRooms.isHighlight || (dmInvites + roomsInvite) > 0,
                                        notificationCountPeople = dmRooms.totalCount + dmInvites,
                                        notificationHighlightPeople = dmRooms.isHighlight || dmInvites > 0,
                                        notificationCountRooms = otherRooms.totalCount + roomsInvite,
                                        notificationHighlightRooms = otherRooms.isHighlight || roomsInvite > 0,
                                        hasUnreadMessages = dmRooms.totalCount + otherRooms.totalCount > 0
                                )
                            }
                        }
                    }
                }
                .disposeOnClear()
    }
}
