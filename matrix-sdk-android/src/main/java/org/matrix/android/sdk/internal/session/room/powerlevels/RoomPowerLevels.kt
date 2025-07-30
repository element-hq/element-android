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
