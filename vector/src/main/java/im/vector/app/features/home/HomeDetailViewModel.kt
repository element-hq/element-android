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

import androidx.lifecycle.asFlow
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import fr.gouv.tchap.core.utils.TchapUtils
import im.vector.app.AppStateHandler
import im.vector.app.RoomGroupingMethod
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.extensions.singletonEntryPoint
import im.vector.app.core.flow.throttleFirst
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.call.dialpad.DialPadLookup
import im.vector.app.features.call.lookup.CallProtocolsChecker
import im.vector.app.features.call.webrtc.WebRtcCallManager
import im.vector.app.features.createdirect.DirectRoomHelper
import im.vector.app.features.invite.AutoAcceptInvites
import im.vector.app.features.invite.showInvites
import im.vector.app.features.settings.VectorDataStore
import im.vector.app.features.ui.UiStateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.query.ActiveSpaceFilter
import org.matrix.android.sdk.api.query.RoomCategoryFilter
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.initsync.SyncStatusService
import org.matrix.android.sdk.api.session.room.RoomSortOrder
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.util.toMatrixItem
import org.matrix.android.sdk.flow.flow
import timber.log.Timber

/**
 * View model used to update the home bottom bar notification counts, observe the sync state and
 * change the selected room list view
 */
class HomeDetailViewModel @AssistedInject constructor(@Assisted initialState: HomeDetailViewState,
                                                      private val session: Session,
                                                      private val uiStateRepository: UiStateRepository,
                                                      private val vectorDataStore: VectorDataStore,
                                                      private val callManager: WebRtcCallManager,
                                                      private val directRoomHelper: DirectRoomHelper,
                                                      private val appStateHandler: AppStateHandler,
                                                      private val autoAcceptInvites: AutoAcceptInvites) :
        VectorViewModel<HomeDetailViewState, HomeDetailAction, HomeDetailViewEvents>(initialState),
        CallProtocolsChecker.Listener {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<HomeDetailViewModel, HomeDetailViewState> {
        override fun create(initialState: HomeDetailViewState): HomeDetailViewModel
    }

    companion object : MavericksViewModelFactory<HomeDetailViewModel, HomeDetailViewState> by hiltMavericksViewModelFactory() {

        override fun initialState(viewModelContext: ViewModelContext): HomeDetailViewState {
            val uiStateRepository = viewModelContext.activity.singletonEntryPoint().uiStateRepository()
            return HomeDetailViewState(
                    currentTab = HomeTab.RoomList(uiStateRepository.getDisplayMode())
            )
        }
    }

    init {
        observeSyncState()
        observeRoomGroupingMethod()
        observeRoomSummaries()
        updateShowDialPadTab()
        observeDataStore()
        callManager.addProtocolsCheckerListener(this)
        session.flow().liveUser(session.myUserId).execute {
            copy(
                    myMatrixItem = it.invoke()?.getOrNull()?.toMatrixItem()
            )
        }
    }

    private fun observeDataStore() {
        viewModelScope.launch {
            vectorDataStore.pushCounterFlow.collect { nbOfPush ->
                setState {
                    copy(
                            pushCounter = nbOfPush
                    )
                }
            }
        }
    }

    override fun handle(action: HomeDetailAction) {
        when (action) {
            is HomeDetailAction.SwitchTab                   -> handleSwitchTab(action)
            HomeDetailAction.MarkAllRoomsRead               -> handleMarkAllRoomsRead()
            is HomeDetailAction.StartCallWithPhoneNumber    -> handleStartCallWithPhoneNumber(action)
            is HomeDetailAction.InviteByEmail               -> handleIndividualInviteByEmail(action)
            is HomeDetailAction.SelectContact               -> handleSelectContact(action)
            is HomeDetailAction.CreateDirectMessageByEmail  -> handleCreateDirectMessage(action)
            is HomeDetailAction.CreateDirectMessageByUserId -> handleCreateDirectMessage(action)
            HomeDetailAction.UnauthorizedEmail              -> handleUnauthorizedEmail()
        }
    }

    private fun handleStartCallWithPhoneNumber(action: HomeDetailAction.StartCallWithPhoneNumber) {
        viewModelScope.launch {
            try {
                _viewEvents.post(HomeDetailViewEvents.Loading)
                val result = DialPadLookup(session, callManager, directRoomHelper).lookupPhoneNumber(action.phoneNumber)
                callManager.startOutgoingCall(result.roomId, result.userId, isVideoCall = false)
                _viewEvents.post(HomeDetailViewEvents.CallStarted)
            } catch (failure: Throwable) {
                _viewEvents.post(HomeDetailViewEvents.FailToCall(failure))
            }
        }
    }

    private fun handleSwitchTab(action: HomeDetailAction.SwitchTab) = withState { state ->
        if (state.currentTab != action.tab) {
            setState {
                copy(currentTab = action.tab)
            }
            if (action.tab is HomeTab.RoomList) {
                uiStateRepository.storeDisplayMode(action.tab.displayMode)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        callManager.removeProtocolsCheckerListener(this)
    }

    override fun onPSTNSupportUpdated() {
        updateShowDialPadTab()
    }

    private fun updateShowDialPadTab() {
        setState {
            copy(showDialPadTab = callManager.supportsPSTNProtocol)
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
                _viewEvents.post(HomeDetailViewEvents.GetPlatform(action.email))
            } else {
                val userId = data.find { it.threePid.value == action.email }?.matrixId
                userId?.let {
                    val user = tryOrNull { session.resolveUser(it) } ?: User(it, TchapUtils.computeDisplayNameFromUserId(it), null)
                    _viewEvents.post(HomeDetailViewEvents.InviteIgnoredForDiscoveredUser(user))
                }
            }
        }
    }

    private fun handleSelectContact(action: HomeDetailAction.SelectContact) {
        val directRoomId = session.getExistingDirectRoomWithUser(action.user.userId)
        if (directRoomId != null) {
            _viewEvents.post(HomeDetailViewEvents.OpenDirectChat(directRoomId))
        } else {
            _viewEvents.post(HomeDetailViewEvents.PromptCreateDirectChat(action.user))
        }
    }

    private fun handleUnauthorizedEmail() = withState {
        it.inviteEmail ?: return@withState

        if (it.existingRoom.isNullOrEmpty()) {
            _viewEvents.post(HomeDetailViewEvents.InviteIgnoredForUnauthorizedEmail(it.inviteEmail))
        } else {
            // Ignore the error, notify the user that the invite has been already sent
            _viewEvents.post(HomeDetailViewEvents.InviteIgnoredForExistingRoom(it.inviteEmail))
        }
    }

    private fun handleCreateDirectMessage(action: HomeDetailAction.CreateDirectMessageByEmail) = withState {
        it.inviteEmail ?: return@withState

        if (it.existingRoom.isNullOrEmpty()) {
            // Send the invite if the email is authorized
            viewModelScope.launch {
                createDirectMessage(it.inviteEmail)
            }
        } else {
            // There is already a discussion with this email
            // We do not re-invite the NoTchapUser except if
            // the email is bound to the external instance (for which the invites may expire).
            if (action.isExternalEmail) {
                // Revoke the pending invite and leave this empty discussion, we will invite again this email.
                // We don't have a way for the moment to check if the invite expired or not...
                viewModelScope.launch {
                    revokePendingInviteAndLeave(it.existingRoom)
                    createDirectMessage(it.inviteEmail)
                }
            } else {
                // Notify the user that the invite has been already sent
                _viewEvents.post(HomeDetailViewEvents.InviteIgnoredForExistingRoom(it.inviteEmail))
            }
        }
    }

    private fun handleCreateDirectMessage(action: HomeDetailAction.CreateDirectMessageByUserId) {
        viewModelScope.launch {
            val roomId = try {
                directRoomHelper.ensureDMExists(action.userId)
            } catch (failure: Throwable) {
                _viewEvents.post(HomeDetailViewEvents.Failure(failure))
                return@launch
            }
            _viewEvents.post(HomeDetailViewEvents.OpenDirectChat(roomId = roomId))
        }
    }

    private suspend fun createDirectMessage(email: String) {
        val roomParams = CreateRoomParams()
                .apply {
                    invite3pids.add(ThreePid.Email(email))
                    setDirectMessage()
                }

        runCatching { session.createRoom(roomParams) }.fold(
                { _ -> _viewEvents.post(HomeDetailViewEvents.InviteNoTchapUserByEmail) },
                { failure -> _viewEvents.post(HomeDetailViewEvents.Failure(failure)) }
        )
    }

    private suspend fun revokePendingInviteAndLeave(roomId: String) {
        session.getRoom(roomId)?.let { room ->
            val token = room.getStateEvent(EventType.STATE_ROOM_THIRD_PARTY_INVITE)?.stateKey

            try {
                if (!token.isNullOrEmpty()) {
                    room.sendStateEvent(
                            eventType = EventType.STATE_ROOM_THIRD_PARTY_INVITE,
                            stateKey = token,
                            body = emptyMap()
                    )
                } else {
                    Timber.d("unable to revoke invite (no pending invite)")
                }

                room.leave()
            } catch (failure: Throwable) {
                _viewEvents.post(HomeDetailViewEvents.Failure(failure))
            }
        }
    }

    private fun observeSyncState() {
        session.flow()
                .liveSyncState()
                .setOnEach { syncState ->
                    copy(syncState = syncState)
                }

        session.getSyncStatusLive()
                .asFlow()
                .filterIsInstance<SyncStatusService.Status.IncrementalSyncStatus>()
                .setOnEach {
                    copy(incrementalSyncStatus = it)
                }
    }

    private fun observeRoomGroupingMethod() {
        appStateHandler.selectedRoomGroupingObservable
                .setOnEach {
                    copy(
                            roomGroupingMethod = it.orNull() ?: RoomGroupingMethod.BySpace(null)
                    )
                }
    }

    private fun observeRoomSummaries() {
        appStateHandler.selectedRoomGroupingObservable.distinctUntilChanged().flatMapLatest {
            // we use it as a trigger to all changes in room, but do not really load
            // the actual models
            session.getPagedRoomSummariesLive(
                    roomSummaryQueryParams {
                        memberships = Membership.activeMemberships()
                    },
                    sortOrder = RoomSortOrder.NONE
            ).asFlow()
        }
                .throttleFirst(300)
                .onEach {
                    when (val groupingMethod = appStateHandler.getCurrentRoomGroupingMethod()) {
                        is RoomGroupingMethod.ByLegacyGroup -> {
                            // TODO!!
                        }
                        is RoomGroupingMethod.BySpace       -> {
                            val activeSpaceRoomId = groupingMethod.spaceSummary?.roomId
                            var dmInvites = 0
                            var roomsInvite = 0
                            if (autoAcceptInvites.showInvites()) {
                                dmInvites = session.getRoomSummaries(
                                        roomSummaryQueryParams {
                                            memberships = listOf(Membership.INVITE)
                                            roomCategoryFilter = RoomCategoryFilter.ONLY_DM
                                            activeSpaceFilter = activeSpaceRoomId?.let { ActiveSpaceFilter.ActiveSpace(it) } ?: ActiveSpaceFilter.None
                                        }
                                ).size

                                roomsInvite = session.getRoomSummaries(
                                        roomSummaryQueryParams {
                                            memberships = listOf(Membership.INVITE)
                                            roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
                                            activeSpaceFilter = ActiveSpaceFilter.ActiveSpace(groupingMethod.spaceSummary?.roomId)
                                        }
                                ).size
                            }

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
                .launchIn(viewModelScope)
    }
}
