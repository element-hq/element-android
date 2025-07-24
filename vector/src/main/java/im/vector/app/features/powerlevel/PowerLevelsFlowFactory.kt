/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.powerlevel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.session.room.model.create.RoomCreateContent
import org.matrix.android.sdk.api.session.room.model.create.getRoomCreateContentWithSender
import org.matrix.android.sdk.api.session.room.powerlevels.RoomPowerLevels
import org.matrix.android.sdk.flow.flow
import org.matrix.android.sdk.flow.mapOptional

class PowerLevelsFlowFactory(private val room: Room) {

    fun createFlow(): Flow<RoomPowerLevels> {
        val flowRoom = room.flow()
        val powerLevelsFlow = flowRoom
                .liveStateEvent(EventType.STATE_ROOM_POWER_LEVELS, QueryStringValue.IsEmpty)
                .mapOptional { it.content.toModel<PowerLevelsContent>() }
                .flowOn(Dispatchers.Default)

        val roomCreateFlow = flowRoom
                .liveStateEvent(EventType.STATE_ROOM_CREATE, QueryStringValue.IsEmpty)
                .mapOptional { event ->
                    event.getRoomCreateContentWithSender()
                }
                .flowOn(Dispatchers.Default)

        return combine(powerLevelsFlow, roomCreateFlow) { powerLevelsContent, roomCreateContent ->
            RoomPowerLevels(
                    powerLevelsContent = powerLevelsContent.getOrNull(),
                    roomCreateContent = roomCreateContent.getOrNull()
            )
        }
    }
}
