/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.space

import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.session.room.model.RoomType
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams

class CreateSpaceParams : CreateRoomParams() {

    init {
        // Space-rooms are distinguished from regular messaging rooms by the m.room.type of m.space
        roomType = RoomType.SPACE

        // Space-rooms should be created with a power level for events_default of 100,
        // to prevent the rooms accidentally/maliciously clogging up with messages from random members of the space.
        powerLevelContentOverride = PowerLevelsContent(
                eventsDefault = 100
        )
    }
}
