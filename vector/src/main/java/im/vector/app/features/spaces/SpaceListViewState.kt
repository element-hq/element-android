/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.spaces

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.RoomGroupingMethod
import org.matrix.android.sdk.api.session.group.model.GroupSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.summary.RoomAggregateNotificationCount
import org.matrix.android.sdk.api.util.MatrixItem

data class SpaceListViewState(
        val myMxItem: Async<MatrixItem.UserItem> = Uninitialized,
        val asyncSpaces: Async<List<RoomSummary>> = Uninitialized,
        val selectedGroupingMethod: RoomGroupingMethod = RoomGroupingMethod.BySpace(null),
        val rootSpacesOrdered: List<RoomSummary>? = null,
        val spaceOrderInfo: Map<String, String?>? = null,
        val spaceOrderLocalEchos: Map<String, String?>? = null,
        val legacyGroups: List<GroupSummary>? = null,
        val expandedStates: Map<String, Boolean> = emptyMap(),
        val homeAggregateCount: RoomAggregateNotificationCount = RoomAggregateNotificationCount(0, 0)
) : MavericksState
