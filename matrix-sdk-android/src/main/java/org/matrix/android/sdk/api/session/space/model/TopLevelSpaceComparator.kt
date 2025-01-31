/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.space.model

import org.matrix.android.sdk.api.session.room.model.RoomSummary

// Can't use regular compare by because Null is considered less than any value, and for space order it's the opposite
class TopLevelSpaceComparator(val orders: Map<String, String?>) : Comparator<RoomSummary> {

    override fun compare(left: RoomSummary?, right: RoomSummary?): Int {
        val leftOrder = left?.roomId?.let { orders[it] }
        val rightOrder = right?.roomId?.let { orders[it] }
        return if (leftOrder != null && rightOrder != null) {
            leftOrder.compareTo(rightOrder)
        } else {
            if (leftOrder == null) {
                if (rightOrder == null) {
                    compareValues(left?.roomId, right?.roomId)
                } else {
                    1
                }
            } else {
                -1
            }
        }
//                .also {
//            Timber.w("VAL: compare(${left?.displayName} | $leftOrder ,${right?.displayName} | $rightOrder) = $it")
//        }
    }
}
