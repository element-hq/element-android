/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.members

import androidx.core.util.Predicate
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import javax.inject.Inject

class RoomMemberSummaryFilter @Inject constructor() : Predicate<RoomMemberSummary> {
    var filter: String = ""

    override fun test(roomMemberSummary: RoomMemberSummary): Boolean {
        if (filter.isEmpty()) {
            // No filter
            return true
        }
        // if filter is "Jo Do", it should match "John Doe"
        return filter.split(" ").all {
            roomMemberSummary.displayName?.contains(it, ignoreCase = true).orFalse() ||
                    // We should maybe exclude the domain from the userId
                    roomMemberSummary.userId.contains(it, ignoreCase = true)
        }
    }
}
