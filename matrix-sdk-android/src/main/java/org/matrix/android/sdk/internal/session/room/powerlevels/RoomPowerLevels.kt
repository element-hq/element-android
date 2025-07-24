/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.powerlevels

import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.session.room.model.create.getRoomCreateContentWithSender
import org.matrix.android.sdk.api.session.room.powerlevels.RoomPowerLevels
import org.matrix.android.sdk.internal.session.room.state.StateEventDataSource

internal fun StateEventDataSource.getRoomPowerLevels(roomId: String): RoomPowerLevels {
    val powerLevelsContent = getStateEvent(roomId, EventType.STATE_ROOM_POWER_LEVELS, QueryStringValue.IsEmpty)
            ?.content?.toModel<PowerLevelsContent>()
    val roomCreateContent = getStateEvent(roomId, EventType.STATE_ROOM_CREATE, QueryStringValue.IsEmpty)?.getRoomCreateContentWithSender()
    return RoomPowerLevels(powerLevelsContent, roomCreateContent)
}
