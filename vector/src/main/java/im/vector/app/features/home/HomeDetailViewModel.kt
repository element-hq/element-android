/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home

import androidx.lifecycle.asFlow
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.SpaceStateHandler
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.extensions.singletonEntryPoint
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.VectorOverrides
import im.vector.app.features.call.dialpad.DialPadLookup
import im.vector.app.features.call.lookup.CallProtocolsChecker
import im.vector.app.features.call.webrtc.WebRtcCallManager
import im.vector.app.features.createdirect.DirectRoomHelper
import im.vector.app.features.invite.AutoAcceptInvites
import im.vector.app.features.invite.showInvites
import im.vector.app.features.settings.VectorDataStore
import im.vector.app.features.ui.UiStateRepository
import im.vector.lib.core.utils.flow.throttleFirst
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.query.RoomCategoryFilter
import org.matrix.android.sdk.api.query.toActiveSpaceOrNoFilter
import org.matrix.android.sdk.api.query.toActiveSpaceOrOrphanRooms
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.NewSessionListener
import org.matrix.android.sdk.api.session.room.RoomSortOrder
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.api.session.sync.SyncRequestState
import org.matrix.android.sdk.api.util.toMatrixItem
import org.matrix.android.sdk.flow.flow
import timber.log.Timber

/**
 * View model used to update the home bottom bar notification counts, observe the sync state and
 * change the selected room list view.
 */
class HomeDetailViewModel @AssistedInject constructor(
        @Assisted initialState: HomeDetailViewState,
        private val session: Session,
        private val uiStateRepository: UiStateRepository,
        private val vectorDataStore: VectorDataStore,
        private val callManager: WebRtcCallManager,
        private val directRoomHelper: DirectRoomHelper,
        private val spaceStateHandler: SpaceStateHandler,
        private val autoAcceptInvites: AutoAcceptInvites,
        private val vectorOverrides: VectorOverrides
) : VectorViewModel<HomeDetailViewState, HomeDetailAction, HomeDetailViewEvents>(initialState),
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

    private val refreshRoomSummariesOnCryptoSessionChange = object : NewSessionListener {
        override fun onNewSession(roomId: String?, sessionId: String) {
            session.roomService().refreshJoinedRoomSummaryPreviews(roomId)
        }
    }

    init {
        observeSyncState()
        observeRoomGroupingMethod()
        session.cryptoService().addNewSessionListener(refreshRoomSummariesOnCryptoSessionChange)
        observeRoomSummaries()
        updatePstnSupportFlag()
        observeDataStore()
        callManager.addProtocolsCheckerListener(this)
        session.flow().liveUser(session.myUserId).execute {
            copy(
                    myMatrixItem = it.invoke()?.getOrNull()?.toMatrixItem()
            )
        }
    }

    private fun observeDataStore() {
        vectorDataStore.pushCounterFlow.setOnEach { nbOfPush ->
            copy(
                    pushCounter = nbOfPush
            )
        }
        vectorOverrides.forceDialPad.setOnEach { force ->
            copy(
                    forceDialPadTab = force
            )
        }
    }

    override fun handle(action: HomeDetailAction) {
        when (action) {
            is HomeDetailAction.SwitchTab -> handleSwitchTab(action)
            HomeDetailAction.MarkAllRoomsRead -> handleMarkAllRoomsRead()
            is HomeDetailAction.StartCallWithPhoneNumber -> handleStartCallWithPhoneNumber(action)
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
        session.cryptoService().removeSessionListener(refreshRoomSummariesOnCryptoSessionChange)
    }

    override fun onPSTNSupportUpdated() {
        updatePstnSupportFlag()
    }

    private fun updatePstnSupportFlag() {
        setState {
            copy(pstnSupportFlag = callManager.supportsPSTNProtocol)
        }
    }

    // PRIVATE METHODS *****************************************************************************

    private fun handleMarkAllRoomsRead() = withState { _ ->
        // questionable to use viewmodelscope
        viewModelScope.launch(Dispatchers.Default) {
            val roomIds = session.roomService().getRoomSummaries(
                    roomSummaryQueryParams {
                        memberships = listOf(Membership.JOIN)
                        roomCategoryFilter = RoomCategoryFilter.ONLY_WITH_NOTIFICATIONS
                    }
            )
                    .map { it.roomId }
            try {
                session.roomService().markAllAsRead(roomIds)
            } catch (failure: Throwable) {
                Timber.d(failure, "Failed to mark all as read")
            }
        }
    }

    private fun observeSyncState() {
        session.flow()
                .liveSyncState()
                .setOnEach { syncState ->
                    copy(syncState = syncState)
                }

        session.syncService().getSyncRequestStateFlow()
                .filterIsInstance<SyncRequestState.IncrementalSyncRequestState>()
                .setOnEach {
                    copy(incrementalSyncRequestState = it)
                }
    }

    private fun observeRoomGroupingMethod() {
        spaceStateHandler.getSelectedSpaceFlow()
                .setOnEach {
                    copy(
                            selectedSpace = it.orNull()
                    )
                }
    }

    private fun observeRoomSummaries() {
        spaceStateHandler.getSelectedSpaceFlow().distinctUntilChanged().flatMapLatest {
            // we use it as a trigger to all changes in room, but do not really load
            // the actual models
            session.roomService().getPagedRoomSummariesLive(
                    roomSummaryQueryParams {
                        memberships = Membership.activeMemberships()
                    },
                    sortOrder = RoomSortOrder.NONE
            ).asFlow()
        }
                .throttleFirst(300)
                .onEach {
                    val activeSpaceRoomId = spaceStateHandler.getCurrentSpace()?.roomId
                    var dmInvites = 0
                    var roomsInvite = 0
                    if (autoAcceptInvites.showInvites()) {
                        dmInvites = session.roomService().getRoomSummaries(
                                roomSummaryQueryParams {
                                    memberships = listOf(Membership.INVITE)
                                    roomCategoryFilter = RoomCategoryFilter.ONLY_DM
                                    spaceFilter = activeSpaceRoomId.toActiveSpaceOrNoFilter()
                                }
                        ).size

                        roomsInvite = session.roomService().getRoomSummaries(
                                roomSummaryQueryParams {
                                    memberships = listOf(Membership.INVITE)
                                    roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
                                    spaceFilter = activeSpaceRoomId.toActiveSpaceOrOrphanRooms()
                                }
                        ).size
                    }

                    val dmRooms = session.roomService().getNotificationCountForRooms(
                            roomSummaryQueryParams {
                                memberships = listOf(Membership.JOIN)
                                roomCategoryFilter = RoomCategoryFilter.ONLY_DM
                                spaceFilter = activeSpaceRoomId.toActiveSpaceOrNoFilter()
                            }
                    )

                    val otherRooms = session.roomService().getNotificationCountForRooms(
                            roomSummaryQueryParams {
                                memberships = listOf(Membership.JOIN)
                                roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
                                spaceFilter = activeSpaceRoomId.toActiveSpaceOrOrphanRooms()
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
                .launchIn(viewModelScope)
    }
}
