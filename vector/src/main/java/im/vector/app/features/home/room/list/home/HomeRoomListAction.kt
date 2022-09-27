/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.home.room.list.home

import im.vector.app.core.platform.VectorViewModelAction
import im.vector.app.features.home.room.list.home.header.HomeRoomFilter
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.notification.RoomNotificationState

sealed class HomeRoomListAction : VectorViewModelAction {
    data class SelectRoom(val roomSummary: RoomSummary) : HomeRoomListAction()
    data class ChangeRoomNotificationState(val roomId: String, val notificationState: RoomNotificationState) : HomeRoomListAction()
    data class ToggleTag(val roomId: String, val tag: String) : HomeRoomListAction()
    data class LeaveRoom(val roomId: String) : HomeRoomListAction()
    data class ChangeRoomFilter(val filter: HomeRoomFilter) : HomeRoomListAction()
    object DeleteAllLocalRoom : HomeRoomListAction()
}
