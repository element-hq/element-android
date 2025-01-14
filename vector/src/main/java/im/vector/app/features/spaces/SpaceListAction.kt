/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces

import im.vector.app.core.platform.VectorViewModelAction
import org.matrix.android.sdk.api.session.room.model.RoomSummary

sealed class SpaceListAction : VectorViewModelAction {
    data class SelectSpace(val spaceSummary: RoomSummary?, val isSubSpace: Boolean) : SpaceListAction()
    data class OpenSpaceInvite(val spaceSummary: RoomSummary) : SpaceListAction()
    data class LeaveSpace(val spaceSummary: RoomSummary) : SpaceListAction()
    data class ToggleExpand(val spaceSummary: RoomSummary) : SpaceListAction()
    object AddSpace : SpaceListAction()
    data class MoveSpace(val spaceId: String, val delta: Int) : SpaceListAction()
    data class OnStartDragging(val spaceId: String, val expanded: Boolean) : SpaceListAction()
    data class OnEndDragging(val spaceId: String, val expanded: Boolean) : SpaceListAction()
}
