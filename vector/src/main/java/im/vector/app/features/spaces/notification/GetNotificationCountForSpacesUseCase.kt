/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
