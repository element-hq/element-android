/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.flow

import androidx.lifecycle.asFlow
import androidx.paging.PagedList
import kotlinx.coroutines.flow.Flow
import org.matrix.android.sdk.api.query.QueryStateEventValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.accountdata.UserAccountDataEvent
import org.matrix.android.sdk.api.session.crypto.crosssigning.MXCrossSigningInfo
import org.matrix.android.sdk.api.session.crypto.crosssigning.PrivateKeysInfo
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.pushers.Pusher
import org.matrix.android.sdk.api.session.room.RoomSortOrder
import org.matrix.android.sdk.api.session.room.RoomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.accountdata.RoomAccountDataEvent
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.space.SpaceSummaryQueryParams
import org.matrix.android.sdk.api.session.sync.SyncState
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.session.widgets.model.Widget
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toOptional

class FlowSession(private val session: Session) {

    fun liveRoomSummary(roomId: String): Flow<Optional<RoomSummary>> {
        return session.roomService().getRoomSummaryLive(roomId).asFlow()
                .startWith(session.coroutineDispatchers.io) {
                    session.roomService().getRoomSummary(roomId).toOptional()
                }
    }

    fun liveRoomSummaries(queryParams: RoomSummaryQueryParams, sortOrder: RoomSortOrder = RoomSortOrder.NONE): Flow<List<RoomSummary>> {
        return session.roomService().getRoomSummariesLive(queryParams, sortOrder).asFlow()
                .startWith(session.coroutineDispatchers.io) {
                    session.roomService().getRoomSummaries(queryParams, sortOrder)
                }
    }

    fun liveSpaceSummaries(queryParams: SpaceSummaryQueryParams): Flow<List<RoomSummary>> {
        return session.spaceService().getSpaceSummariesLive(queryParams).asFlow()
                .startWith(session.coroutineDispatchers.io) {
                    session.spaceService().getSpaceSummaries(queryParams)
                }
    }

    fun liveBreadcrumbs(queryParams: RoomSummaryQueryParams): Flow<List<RoomSummary>> {
        return session.roomService().getBreadcrumbsLive(queryParams).asFlow()
                .startWith(session.coroutineDispatchers.io) {
                    session.roomService().getBreadcrumbs(queryParams)
                }
    }

    fun liveMyDevicesInfo(): Flow<List<DeviceInfo>> {
        return session.cryptoService().getLiveMyDevicesInfo().asFlow()
                .startWith(session.coroutineDispatchers.io) {
                    session.cryptoService().getMyDevicesInfo()
                }
    }

    fun liveSyncState(): Flow<SyncState> {
        return session.syncService().getSyncStateLive().asFlow()
    }

    fun livePushers(): Flow<List<Pusher>> {
        return session.pushersService().getPushersLive().asFlow()
    }

    fun liveUser(userId: String): Flow<Optional<User>> {
        return session.userService().getUserLive(userId).asFlow()
                .startWith(session.coroutineDispatchers.io) {
                    session.userService().getUser(userId).toOptional()
                }
    }

    fun liveRoomMember(userId: String, roomId: String): Flow<Optional<RoomMemberSummary>> {
        return session.roomService().getRoomMemberLive(userId, roomId).asFlow()
                .startWith(session.coroutineDispatchers.io) {
                    session.roomService().getRoomMember(userId, roomId).toOptional()
                }
    }

    fun liveUsers(): Flow<List<User>> {
        return session.userService().getUsersLive().asFlow()
    }

    fun liveIgnoredUsers(): Flow<List<User>> {
        return session.userService().getIgnoredUsersLive().asFlow()
    }

    fun livePagedUsers(filter: String? = null, excludedUserIds: Set<String>? = null): Flow<PagedList<User>> {
        return session.userService().getPagedUsersLive(filter, excludedUserIds).asFlow()
    }

    fun liveThreePIds(refreshData: Boolean): Flow<List<ThreePid>> {
        return session.profileService().getThreePidsLive(refreshData).asFlow()
                .startWith(session.coroutineDispatchers.io) {
                    session.profileService().getThreePids()
                }
    }

    fun livePendingThreePIds(): Flow<List<ThreePid>> {
        return session.profileService().getPendingThreePidsLive().asFlow()
                .startWith(session.coroutineDispatchers.io) {
                    session.profileService().getPendingThreePids()
                }
    }

    fun liveUserCryptoDevices(userId: String): Flow<List<CryptoDeviceInfo>> {
        return session.cryptoService().getLiveCryptoDeviceInfo(userId).asFlow()
                .startWith(session.coroutineDispatchers.io) {
                    session.cryptoService().getCryptoDeviceInfo(userId)
                }
    }

    fun liveCrossSigningInfo(userId: String): Flow<Optional<MXCrossSigningInfo>> {
        return session.cryptoService().crossSigningService().getLiveCrossSigningKeys(userId).asFlow()
                .startWith(session.coroutineDispatchers.io) {
                    session.cryptoService().crossSigningService().getUserCrossSigningKeys(userId).toOptional()
                }
    }

    fun liveCrossSigningPrivateKeys(): Flow<Optional<PrivateKeysInfo>> {
        return session.cryptoService().crossSigningService().getLiveCrossSigningPrivateKeys().asFlow()
                .startWith(session.coroutineDispatchers.io) {
                    session.cryptoService().crossSigningService().getCrossSigningPrivateKeys().toOptional()
                }
    }

    fun liveUserAccountData(types: Set<String>): Flow<List<UserAccountDataEvent>> {
        return session.accountDataService().getLiveUserAccountDataEvents(types).asFlow()
                .startWith(session.coroutineDispatchers.io) {
                    session.accountDataService().getUserAccountDataEvents(types)
                }
    }

    fun liveUserAccountData(type: String): Flow<Optional<UserAccountDataEvent>> {
        return session.accountDataService().getLiveUserAccountDataEvent(type).asFlow()
                .startWith(session.coroutineDispatchers.io) {
                    session.accountDataService().getUserAccountDataEvent(type).toOptional()
                }
    }

    fun liveRoomAccountData(types: Set<String>): Flow<List<RoomAccountDataEvent>> {
        return session.accountDataService().getLiveRoomAccountDataEvents(types).asFlow()
                .startWith(session.coroutineDispatchers.io) {
                    session.accountDataService().getRoomAccountDataEvents(types)
                }
    }

    fun liveRoomWidgets(
            roomId: String,
            widgetId: QueryStateEventValue,
            widgetTypes: Set<String>? = null,
            excludedTypes: Set<String>? = null
    ): Flow<List<Widget>> {
        return session.widgetService().getRoomWidgetsLive(roomId, widgetId, widgetTypes, excludedTypes).asFlow()
                .startWith(session.coroutineDispatchers.io) {
                    session.widgetService().getRoomWidgets(roomId, widgetId, widgetTypes, excludedTypes)
                }
    }

    fun liveRoomChangeMembershipState(): Flow<Map<String, ChangeMembershipState>> {
        return session.roomService().getChangeMembershipsLive().asFlow()
    }
}

fun Session.flow(): FlowSession {
    return FlowSession(this)
}
