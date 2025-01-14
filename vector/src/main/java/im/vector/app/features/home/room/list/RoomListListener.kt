/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list

import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.SpaceChildInfo

interface RoomListListener {
    fun onRoomClicked(room: RoomSummary)
    fun onRoomLongClicked(room: RoomSummary): Boolean
    fun onRejectRoomInvitation(room: RoomSummary)
    fun onAcceptRoomInvitation(room: RoomSummary)
    fun onJoinSuggestedRoom(room: SpaceChildInfo)
    fun onSuggestedRoomClicked(room: SpaceChildInfo)
}
