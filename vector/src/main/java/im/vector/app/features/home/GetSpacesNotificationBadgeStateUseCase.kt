/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home

import im.vector.app.features.home.room.list.UnreadCounterBadgeView
import im.vector.app.features.spaces.GetSpacesUseCase
import im.vector.app.features.spaces.notification.GetNotificationCountForSpacesUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.query.SpaceFilter
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.spaceSummaryQueryParams
import org.matrix.android.sdk.api.session.room.summary.RoomAggregateNotificationCount
import javax.inject.Inject

class GetSpacesNotificationBadgeStateUseCase @Inject constructor(
        private val getNotificationCountForSpacesUseCase: GetNotificationCountForSpacesUseCase,
        private val getSpacesUseCase: GetSpacesUseCase,
) {

    fun execute(): Flow<UnreadCounterBadgeView.State> {
        val params = spaceSummaryQueryParams {
            memberships = listOf(Membership.INVITE)
            displayName = QueryStringValue.IsNotEmpty
        }
        return combine(
                getNotificationCountForSpacesUseCase.execute(SpaceFilter.NoFilter),
                getSpacesUseCase.execute(params),
        ) { spacesNotificationCount, spaceInvites ->
            computeSpacesNotificationCounterBadgeState(spacesNotificationCount, spaceInvites)
        }
    }

    private fun computeSpacesNotificationCounterBadgeState(
            spacesNotificationCount: RoomAggregateNotificationCount,
            spaceInvites: List<RoomSummary>,
    ): UnreadCounterBadgeView.State {
        val hasPendingSpaceInvites = spaceInvites.isNotEmpty()
        return if (hasPendingSpaceInvites && spacesNotificationCount.notificationCount == 0) {
            UnreadCounterBadgeView.State.Text(
                    text = "!",
                    highlighted = true,
            )
        } else {
            UnreadCounterBadgeView.State.Count(
                    count = spacesNotificationCount.notificationCount,
                    highlighted = spacesNotificationCount.isHighlight || hasPendingSpaceInvites,
            )
        }
    }
}
