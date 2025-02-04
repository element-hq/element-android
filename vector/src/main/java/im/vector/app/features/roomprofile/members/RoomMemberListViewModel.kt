/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.members

import androidx.lifecycle.asFlow
import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.powerlevel.PowerLevelsFlowFactory
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.UserVerificationLevel
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.members.roomMemberQueryParams
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.api.session.room.powerlevels.Role
import org.matrix.android.sdk.flow.flow
import org.matrix.android.sdk.flow.mapOptional
import org.matrix.android.sdk.flow.unwrap
import timber.log.Timber

class RoomMemberListViewModel @AssistedInject constructor(
        @Assisted initialState: RoomMemberListViewState,
        private val roomMemberSummaryComparator: RoomMemberSummaryComparator,
        private val session: Session
) :
        VectorViewModel<RoomMemberListViewState, RoomMemberListAction, EmptyViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<RoomMemberListViewModel, RoomMemberListViewState> {
        override fun create(initialState: RoomMemberListViewState): RoomMemberListViewModel
    }

    companion object : MavericksViewModelFactory<RoomMemberListViewModel, RoomMemberListViewState> by hiltMavericksViewModelFactory()

    private val room = session.getRoom(initialState.roomId)!!
    private val roomFlow = room.flow()

    init {
        observeRoomMemberSummaries()
        observeThirdPartyInvites()
        observeRoomSummary()
        observePowerLevel()
        observeIgnoredUsers()
    }

    private fun observeRoomMemberSummaries() {
        val roomMemberQueryParams = roomMemberQueryParams {
            displayName = QueryStringValue.IsNotEmpty
            memberships = Membership.activeMemberships()
        }

        combine(
                roomFlow.liveRoomMembers(roomMemberQueryParams),
                roomFlow
                        .liveStateEvent(EventType.STATE_ROOM_POWER_LEVELS, QueryStringValue.IsEmpty)
                        .mapOptional { it.content.toModel<PowerLevelsContent>() }
                        .unwrap()
        ) { roomMembers, powerLevelsContent ->
            buildRoomMemberSummaries(powerLevelsContent, roomMembers)
        }
                .execute { async ->
                    copy(roomMemberSummaries = async)
                }

        roomFlow.liveAreAllMembersLoaded()
                .distinctUntilChanged()
                .onEach {
                    setState {
                        copy(
                                areAllMembersLoaded = it
                        )
                    }
                }
                .launchIn(viewModelScope)

        if (room.roomCryptoService().isEncrypted()) {
            roomFlow.liveRoomMembers(roomMemberQueryParams)
                    .flatMapLatest { membersSummary ->
                        session.cryptoService().getLiveCryptoDeviceInfo(membersSummary.map { it.userId })
                                .asFlow()
                                .catch { Timber.e(it) }
                                .map { deviceList ->
                                    // If any key change, emit the userIds list
                                    deviceList.groupBy { it.userId }.mapValues {
                                        getUserTrustLevel(it.key, it.value)
                                    }
                                }
                    }
                    .execute { async ->
                        copy(trustLevelMap = async)
                    }
        }
    }

    private suspend fun getUserTrustLevel(userId: String, devices: List<CryptoDeviceInfo>): UserVerificationLevel {
        val allDeviceTrusted = devices.fold(devices.isNotEmpty()) { prev, next ->
            prev && next.trustLevel?.isCrossSigningVerified().orFalse()
        }
        val mxCrossSigningInfo = session.cryptoService().crossSigningService().getUserCrossSigningKeys(userId)
        return when {
            mxCrossSigningInfo == null -> {
                UserVerificationLevel.WAS_NEVER_VERIFIED
            }
            mxCrossSigningInfo.isTrusted() -> {
                if (allDeviceTrusted) UserVerificationLevel.VERIFIED_ALL_DEVICES_TRUSTED
                else UserVerificationLevel.VERIFIED_WITH_DEVICES_UNTRUSTED
            }
            else -> {
                if (mxCrossSigningInfo.wasTrustedOnce) {
                    UserVerificationLevel.UNVERIFIED_BUT_WAS_PREVIOUSLY
                } else {
                    UserVerificationLevel.WAS_NEVER_VERIFIED
                }
            }
        }
    }

    private fun observePowerLevel() {
        PowerLevelsFlowFactory(room).createFlow()
                .onEach {
                    val permissions = ActionPermissions(
                            canInvite = PowerLevelsHelper(it).isUserAbleToInvite(session.myUserId),
                            canRevokeThreePidInvite = PowerLevelsHelper(it).isUserAllowedToSend(
                                    userId = session.myUserId,
                                    isState = true,
                                    eventType = EventType.STATE_ROOM_THIRD_PARTY_INVITE
                            )
                    )
                    setState {
                        copy(actionsPermissions = permissions)
                    }
                }.launchIn(viewModelScope)
    }

    private fun observeRoomSummary() {
        roomFlow.liveRoomSummary()
                .unwrap()
                .execute { async ->
                    copy(roomSummary = async)
                }
    }

    private fun observeThirdPartyInvites() {
        roomFlow
                .liveStateEvents(setOf(EventType.STATE_ROOM_THIRD_PARTY_INVITE), QueryStringValue.IsNotNull)
                .execute { async ->
                    copy(threePidInvites = async)
                }
    }

    private fun observeIgnoredUsers() {
        session.flow()
                .liveIgnoredUsers()
                .execute { async ->
                    copy(
                            ignoredUserIds = async.invoke().orEmpty().map { it.userId }
                    )
                }
    }

    private fun buildRoomMemberSummaries(powerLevelsContent: PowerLevelsContent, roomMembers: List<RoomMemberSummary>): RoomMemberSummaries {
        val admins = ArrayList<RoomMemberSummary>()
        val moderators = ArrayList<RoomMemberSummary>()
        val users = ArrayList<RoomMemberSummary>(roomMembers.size)
        val customs = ArrayList<RoomMemberSummary>()
        val invites = ArrayList<RoomMemberSummary>()
        val powerLevelsHelper = PowerLevelsHelper(powerLevelsContent)
        roomMembers
                .forEach { roomMember ->
                    val userRole = powerLevelsHelper.getUserRole(roomMember.userId)
                    when {
                        roomMember.membership == Membership.INVITE -> invites.add(roomMember)
                        userRole == Role.Admin -> admins.add(roomMember)
                        userRole == Role.Moderator -> moderators.add(roomMember)
                        userRole == Role.Default -> users.add(roomMember)
                        else -> customs.add(roomMember)
                    }
                }

        return listOf(
                RoomMemberListCategories.ADMIN to admins.sortedWith(roomMemberSummaryComparator),
                RoomMemberListCategories.MODERATOR to moderators.sortedWith(roomMemberSummaryComparator),
                RoomMemberListCategories.CUSTOM to customs.sortedWith(roomMemberSummaryComparator),
                RoomMemberListCategories.INVITE to invites.sortedWith(roomMemberSummaryComparator),
                RoomMemberListCategories.USER to users.sortedWith(roomMemberSummaryComparator)
        )
    }

    override fun handle(action: RoomMemberListAction) {
        when (action) {
            is RoomMemberListAction.RevokeThreePidInvite -> handleRevokeThreePidInvite(action)
            is RoomMemberListAction.FilterMemberList -> handleFilterMemberList(action)
        }
    }

    private fun handleRevokeThreePidInvite(action: RoomMemberListAction.RevokeThreePidInvite) {
        viewModelScope.launch {
            room.stateService().sendStateEvent(
                    eventType = EventType.STATE_ROOM_THIRD_PARTY_INVITE,
                    stateKey = action.stateKey,
                    body = emptyMap()
            )
        }
    }

    private fun handleFilterMemberList(action: RoomMemberListAction.FilterMemberList) {
        setState {
            copy(
                    filter = action.searchTerm
            )
        }
    }
}
