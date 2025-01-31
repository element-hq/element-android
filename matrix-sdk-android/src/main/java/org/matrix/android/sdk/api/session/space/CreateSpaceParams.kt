/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
