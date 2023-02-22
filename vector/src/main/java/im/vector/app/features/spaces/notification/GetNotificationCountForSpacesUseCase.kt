/*
 * Copyright (c) 2023 New Vector Ltd
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

package im.vector.app.features.spaces.notification

import androidx.lifecycle.asFlow
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.features.invite.AutoAcceptInvites
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.sample
import org.matrix.android.sdk.api.query.SpaceFilter
import org.matrix.android.sdk.api.session.room.RoomSortOrder
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.summary.RoomAggregateNotificationCount
import javax.inject.Inject

class GetNotificationCountForSpacesUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
        private val autoAcceptInvites: AutoAcceptInvites,
) {

    fun execute(spaceFilter: SpaceFilter): Flow<RoomAggregateNotificationCount> {
        val session = activeSessionHolder.getSafeActiveSession()

        val spaceQueryParams = roomSummaryQueryParams {
            this.memberships = listOf(Membership.JOIN)
            this.spaceFilter = spaceFilter
        }
        return session
                ?.roomService()
                ?.getPagedRoomSummariesLive(queryParams = spaceQueryParams, sortOrder = RoomSortOrder.NONE)
                ?.asFlow()
                ?.sample(300)
                ?.mapLatest {
                    val inviteCount = if (autoAcceptInvites.hideInvites) {
                        0
                    } else {
                        session.roomService().getRoomSummaries(
                                roomSummaryQueryParams { this.memberships = listOf(Membership.INVITE) }
                        ).size
                    }
                    val totalCount = session.roomService().getNotificationCountForRooms(spaceQueryParams)
                    RoomAggregateNotificationCount(
                            notificationCount = totalCount.notificationCount + inviteCount,
                            highlightCount = totalCount.highlightCount + inviteCount,
                    )
                }
                ?.flowOn(session.coroutineDispatchers.main)
                ?: emptyFlow()
    }
}
