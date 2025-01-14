/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.summary.RoomAggregateNotificationCount
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.util.MatrixItem

data class SpaceListViewState(
        val myMxItem: Async<MatrixItem.UserItem> = Uninitialized,
        val asyncSpaces: Async<List<RoomSummary>> = Uninitialized,
        val spaces: List<RoomSummary> = emptyList(),
        val selectedSpace: RoomSummary? = null,
        val inviters: List<User> = emptyList(),
        val rootSpacesOrdered: List<RoomSummary>? = null,
        val spaceOrderInfo: Map<String, String?>? = null,
        val spaceOrderLocalEchos: Map<String, String?>? = null,
        val expandedStates: Map<String, Boolean> = emptyMap(),
        val homeAggregateCount: RoomAggregateNotificationCount = RoomAggregateNotificationCount(0, 0)
) : MavericksState
