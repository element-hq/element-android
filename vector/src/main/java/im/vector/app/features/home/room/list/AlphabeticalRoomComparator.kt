/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list

import org.matrix.android.sdk.api.session.room.model.RoomSummary
import javax.inject.Inject

class AlphabeticalRoomComparator @Inject constructor() : Comparator<RoomSummary> {

    override fun compare(leftRoomSummary: RoomSummary?, rightRoomSummary: RoomSummary?): Int {
        return when {
            rightRoomSummary?.displayName == null -> -1
            leftRoomSummary?.displayName == null -> 1
            else -> leftRoomSummary.displayName.compareTo(rightRoomSummary.displayName)
        }
    }
}
