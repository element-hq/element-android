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

import im.vector.app.core.platform.VectorViewModelAction
import org.matrix.android.sdk.api.session.group.model.GroupSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary

sealed class SpaceListAction : VectorViewModelAction {
    data class SelectSpace(val spaceSummary: RoomSummary?) : SpaceListAction()
    data class OpenSpaceInvite(val spaceSummary: RoomSummary) : SpaceListAction()
    data class LeaveSpace(val spaceSummary: RoomSummary) : SpaceListAction()
    data class ToggleExpand(val spaceSummary: RoomSummary) : SpaceListAction()
    object AddSpace : SpaceListAction()
    data class MoveSpace(val spaceId: String, val delta : Int) : SpaceListAction()
    data class OnStartDragging(val spaceId: String, val expanded: Boolean) : SpaceListAction()
    data class OnEndDragging(val spaceId: String, val expanded: Boolean) : SpaceListAction()

    data class SelectLegacyGroup(val groupSummary: GroupSummary?) : SpaceListAction()
}
