/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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
