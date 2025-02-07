/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile

import androidx.lifecycle.asFlow
import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.analytics.AnalyticsTracker
import im.vector.app.features.analytics.plan.Interaction
import im.vector.app.features.home.ShortcutCreator
import im.vector.app.features.powerlevel.PowerLevelsFlowFactory
import im.vector.app.features.session.coroutineScope
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.getStateEvent
import org.matrix.android.sdk.api.session.room.members.roomMemberQueryParams
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.create.RoomCreateContent
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.api.session.room.state.isPublic
import org.matrix.android.sdk.flow.FlowRoom
import org.matrix.android.sdk.flow.flow
import org.matrix.android.sdk.flow.mapOptional
import org.matrix.android.sdk.flow.unwrap
import timber.log.Timber

class RoomProfileViewModel @AssistedInject constructor(
        @Assisted private val initialState: RoomProfileViewState,
        private val stringProvider: StringProvider,
        private val shortcutCreator: ShortcutCreator,
        private val session: Session,
        private val analyticsTracker: AnalyticsTracker
) : VectorViewModel<RoomProfileViewState, RoomProfileAction, RoomProfileViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<RoomProfileViewModel, RoomProfileViewState> {
        override fun create(initialState: RoomProfileViewState): RoomProfileViewModel
    }

    companion object : MavericksViewModelFactory<RoomProfileViewModel, RoomProfileViewState> by hiltMavericksViewModelFactory()

    private val room = session.getRoom(initialState.roomId)!!

    init {
        val flowRoom = room.flow()
        observeRoomSummary(flowRoom)
        observeRoomCreateContent(flowRoom)
        observeBannedRoomMembers(flowRoom)
        observePermissions()
        observePowerLevels()
        observeCryptoSettings(flowRoom)
    }

    private fun observeCryptoSettings(flowRoom: FlowRoom) {
        val perRoomBlockStatus = session.cryptoService().getLiveBlockUnverifiedDevices(initialState.roomId)
                .asFlow()

        perRoomBlockStatus
                .execute {
                    copy(encryptToVerifiedDeviceOnly = it)
                }

        val globalBlockStatus = session.cryptoService().getLiveGlobalCryptoConfig()
                .asFlow()

        globalBlockStatus
                .execute {
                    copy(globalCryptoConfig = it)
                }

        perRoomBlockStatus.combine(globalBlockStatus) { perRoom, global ->
            perRoom || global.globalBlockUnverifiedDevices
        }.flatMapLatest {
            if (it) {
                flowRoom.liveRoomMembers(roomMemberQueryParams { memberships = Membership.activeMemberships() })
                        .map { it.map { it.userId } }
                        .flatMapLatest {
                            session.cryptoService().getLiveCryptoDeviceInfo(it).asFlow()
                        }
            } else {
                flowOf(emptyList())
            }
        }.map {
            it.isNotEmpty()
        }.execute {
            copy(
                    unverifiedDevicesInTheRoom = it
            )
        }
    }

    private fun observePowerLevels() {
        val powerLevelsContentLive = PowerLevelsFlowFactory(room).createFlow()
        powerLevelsContentLive
                .onEach {
                    val powerLevelsHelper = PowerLevelsHelper(it)
                    val canUpdateRoomState = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true, EventType.STATE_ROOM_ENCRYPTION)
                    setState {
                        copy(canUpdateRoomState = canUpdateRoomState)
                    }
                }.launchIn(viewModelScope)
    }

    private fun observeRoomCreateContent(flowRoom: FlowRoom) {
        flowRoom.liveStateEvent(EventType.STATE_ROOM_CREATE, QueryStringValue.IsEmpty)
                .mapOptional { it.content.toModel<RoomCreateContent>() }
                .unwrap()
                .execute { async ->
                    copy(
                            roomCreateContent = async,
                            // This is a shortcut, we should do the next lines elsewhere, but keep it like that for the moment.
                            recommendedRoomVersion = room.roomVersionService().getRecommendedVersion(),
                            isUsingUnstableRoomVersion = room.roomVersionService().isUsingUnstableRoomVersion(),
                            canUpgradeRoom = room.roomVersionService().userMayUpgradeRoom(session.myUserId),
                            isTombstoned = room.getStateEvent(EventType.STATE_ROOM_TOMBSTONE, QueryStringValue.IsEmpty) != null
                    )
                }
    }

    private fun observeRoomSummary(flowRoom: FlowRoom) {
        flowRoom.liveRoomSummary()
                .unwrap()
                .execute {
                    copy(roomSummary = it)
                }
    }

    private fun observeBannedRoomMembers(flowRoom: FlowRoom) {
        flowRoom.liveRoomMembers(roomMemberQueryParams { memberships = listOf(Membership.BAN) })
                .execute {
                    copy(bannedMembership = it)
                }
    }

    private fun observePermissions() {
        PowerLevelsFlowFactory(room)
                .createFlow()
                .setOnEach {
                    val powerLevelsHelper = PowerLevelsHelper(it)
                    val permissions = RoomProfileViewState.ActionPermissions(
                            canEnableEncryption = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true, EventType.STATE_ROOM_ENCRYPTION)
                    )
                    copy(actionPermissions = permissions)
                }
    }

    override fun handle(action: RoomProfileAction) {
        when (action) {
            is RoomProfileAction.EnableEncryption -> handleEnableEncryption()
            RoomProfileAction.LeaveRoom -> handleLeaveRoom()
            is RoomProfileAction.ChangeRoomNotificationState -> handleChangeNotificationMode(action)
            is RoomProfileAction.ShareRoomProfile -> handleShareRoomProfile()
            RoomProfileAction.CreateShortcut -> handleCreateShortcut()
            RoomProfileAction.RestoreEncryptionState -> restoreEncryptionState()
            is RoomProfileAction.SetEncryptToVerifiedDeviceOnly -> setEncryptToVerifiedDeviceOnly(action.enabled)
            is RoomProfileAction.ReportRoom -> handleReportRoom(action.reason)
        }
    }

    private fun handleReportRoom(reason: String) {
        _viewEvents.post(RoomProfileViewEvents.Loading())
        session.coroutineScope.launch {
            try {
                room.reportingService().reportRoom(reason = reason)
                _viewEvents.post(
                        RoomProfileViewEvents.Success(
                                stringProvider.getString(CommonStrings.room_profile_section_more_report_success_content)
                        )
                )
            } catch (failure: Throwable) {
                Timber.e(failure, "Failed to report room ${room.roomId}")
                _viewEvents.post(RoomProfileViewEvents.Failure(failure))
            } finally {
                _viewEvents.post(RoomProfileViewEvents.DismissLoading)
            }
        }
    }

    fun isPublicRoom(): Boolean {
        return room.stateService().isPublic()
    }

    private fun handleEnableEncryption() {
        postLoading(true)

        viewModelScope.launch {
            val result = runCatching { room.roomCryptoService().enableEncryption() }
            postLoading(false)
            result.onFailure { failure ->
                _viewEvents.post(RoomProfileViewEvents.Failure(failure))
            }
        }
    }

    private fun postLoading(isLoading: Boolean) {
        setState {
            copy(isLoading = isLoading)
        }
    }

    private fun handleCreateShortcut() {
        viewModelScope.launch(Dispatchers.IO) {
            withState { state ->
                state.roomSummary()
                        ?.let { shortcutCreator.create(it) }
                        ?.let { _viewEvents.post(RoomProfileViewEvents.OnShortcutReady(it)) }
            }
        }
    }

    private fun handleChangeNotificationMode(action: RoomProfileAction.ChangeRoomNotificationState) {
        viewModelScope.launch {
            try {
                room.roomPushRuleService().setRoomNotificationState(action.notificationState)
            } catch (failure: Throwable) {
                _viewEvents.post(RoomProfileViewEvents.Failure(failure))
            }
        }
    }

    private fun handleLeaveRoom() {
        _viewEvents.post(RoomProfileViewEvents.Loading(stringProvider.getString(CommonStrings.room_profile_leaving_room)))
        viewModelScope.launch {
            try {
                session.roomService().leaveRoom(room.roomId)
                analyticsTracker.capture(
                        Interaction(
                                index = null,
                                interactionType = null,
                                name = Interaction.Name.MobileRoomLeave
                        )
                )
                // Do nothing, we will be closing the room automatically when it will get back from sync
            } catch (failure: Throwable) {
                _viewEvents.post(RoomProfileViewEvents.Failure(failure))
            }
        }
    }

    private fun handleShareRoomProfile() {
        session.permalinkService().createRoomPermalink(initialState.roomId)
                ?.let { permalink ->
                    _viewEvents.post(RoomProfileViewEvents.ShareRoomProfile(permalink))
                }
    }

    private fun setEncryptToVerifiedDeviceOnly(enabled: Boolean) {
        session.coroutineScope.launch {
            session.cryptoService().setRoomBlockUnverifiedDevices(room.roomId, enabled)
        }
    }

    private fun restoreEncryptionState() {
        _viewEvents.post(RoomProfileViewEvents.Loading())
        session.coroutineScope.launch {
            try {
                room.roomCryptoService().enableEncryption(force = true)
            } catch (failure: Throwable) {
                Timber.e(failure, "Failed to restore encryption state in room ${room.roomId}")
                _viewEvents.post(RoomProfileViewEvents.Failure(failure))
            } finally {
                _viewEvents.post(RoomProfileViewEvents.DismissLoading)
            }
        }
    }
}
