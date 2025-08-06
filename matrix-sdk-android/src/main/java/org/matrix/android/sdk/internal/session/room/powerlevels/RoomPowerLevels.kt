/*
 * Copyright 2025 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.matrix.android.sdk.internal.session.room.powerlevels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.session.room.model.create.getRoomCreateContentWithSender
import org.matrix.android.sdk.api.session.room.powerlevels.RoomPowerLevels
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.session.room.state.StateEventDataSource

internal fun StateEventDataSource.getRoomPowerLevels(roomId: String): RoomPowerLevels {
    val powerLevelsEvent = getStateEvent(roomId, EventType.STATE_ROOM_POWER_LEVELS, QueryStringValue.IsEmpty)
    val roomCreateEvent = getStateEvent(roomId, EventType.STATE_ROOM_CREATE, QueryStringValue.IsEmpty)
    return createRoomPowerLevels(
            powerLevelsEvent = powerLevelsEvent,
            roomCreateEvent = roomCreateEvent
    )
}

internal fun StateEventDataSource.getRoomPowerLevelsLive(roomId: String): LiveData<RoomPowerLevels> {
    val powerLevelsEventLive = getStateEventLive(roomId, EventType.STATE_ROOM_POWER_LEVELS, QueryStringValue.IsEmpty)
    val roomCreateEventLive = getStateEventLive(roomId, EventType.STATE_ROOM_CREATE, QueryStringValue.IsEmpty)
    val resultLiveData = MediatorLiveData<RoomPowerLevels>()

    fun emitIfReady(powerLevelEvent: Optional<Event>?, roomCreateEvent: Optional<Event>?) {
        if (powerLevelEvent != null && roomCreateEvent != null) {
            val roomPowerLevels = createRoomPowerLevels(
                    powerLevelsEvent = powerLevelEvent.getOrNull(),
                    roomCreateEvent = roomCreateEvent.getOrNull()
            )
            resultLiveData.postValue(roomPowerLevels)
        }
    }
    resultLiveData.apply {
        var powerLevelEvent: Optional<Event>? = null
        var roomCreateEvent: Optional<Event>? = null

        addSource(powerLevelsEventLive) {
            powerLevelEvent = it
            emitIfReady(powerLevelEvent, roomCreateEvent)
        }
        addSource(roomCreateEventLive) {
            roomCreateEvent = it
            emitIfReady(powerLevelEvent, roomCreateEvent)
        }
    }
    return resultLiveData
}

private fun createRoomPowerLevels(powerLevelsEvent: Event?, roomCreateEvent: Event?): RoomPowerLevels {
    val powerLevelsContent = powerLevelsEvent?.content?.toModel<PowerLevelsContent>()
    val roomCreateContent = roomCreateEvent?.getRoomCreateContentWithSender()
    return RoomPowerLevels(powerLevelsContent, roomCreateContent)
}
