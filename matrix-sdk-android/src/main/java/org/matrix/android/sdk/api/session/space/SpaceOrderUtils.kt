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

package org.matrix.android.sdk.api.session.space

import org.matrix.android.sdk.api.util.StringOrderUtils

object SpaceOrderUtils {

    data class SpaceReOrderCommand(
            val spaceId: String,
            val order: String
    )

    fun orderCommandsForMove(orderedSpacesToOrderMap: List<Pair<String, String?>>, movedSpaceId: String, delta: Int): List<SpaceReOrderCommand> {
        val movedIndex = orderedSpacesToOrderMap.indexOfFirst { it.first == movedSpaceId }
        if (movedIndex == -1) return emptyList()
        if (delta == 0) return emptyList()

        val targetIndex = if (delta > 0) movedIndex + delta else (movedIndex + delta - 1)

        val nodesToReNumber = mutableListOf<String>()
        var lowerBondOrder: String? = null
        var afterSpace: Pair<String, String?>? = null
//        if (delta > 0) {
        var index = targetIndex
        while (index >= 0 && lowerBondOrder == null) {
            val node = orderedSpacesToOrderMap.getOrNull(index)
            if (node != null /*null when adding at the end*/) {
                val nodeOrder = node.second
                if (node.first == movedSpaceId) break
                if (nodeOrder == null) {
                    nodesToReNumber.add(0, node.first)
                } else {
                    lowerBondOrder = nodeOrder
                }
            }
            index--
        }
        nodesToReNumber.add(movedSpaceId)
        afterSpace = if (orderedSpacesToOrderMap.indices.contains(targetIndex + 1)) orderedSpacesToOrderMap[targetIndex + 1] else null

        val defaultMaxOrder = CharArray(4) { StringOrderUtils.DEFAULT_ALPHABET.last() }
                .joinToString("")

        val defaultMinOrder = CharArray(4) { StringOrderUtils.DEFAULT_ALPHABET.first() }
                .joinToString("")

        val afterOrder = afterSpace?.second ?: defaultMaxOrder

        val beforeOrder = lowerBondOrder ?: defaultMinOrder

        val newOrder = StringOrderUtils.midPoints(beforeOrder, afterOrder, nodesToReNumber.size)

        if (newOrder.isNullOrEmpty()) {
            // re order all?
            val expectedList = orderedSpacesToOrderMap.toMutableList()
            expectedList.removeAt(movedIndex).let {
                expectedList.add(movedIndex + delta, it)
            }

            return StringOrderUtils.midPoints(defaultMinOrder, defaultMaxOrder, orderedSpacesToOrderMap.size)?.let { orders ->
                expectedList.mapIndexed { index, pair ->
                    SpaceReOrderCommand(
                            pair.first,
                            orders[index]
                    )
                }
            } ?: emptyList()
        } else {
            return nodesToReNumber.mapIndexed { index, s ->
                SpaceReOrderCommand(
                        s,
                        newOrder[index]
                )
            }
        }
    }
}
