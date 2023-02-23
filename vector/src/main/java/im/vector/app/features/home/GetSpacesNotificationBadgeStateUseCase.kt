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
