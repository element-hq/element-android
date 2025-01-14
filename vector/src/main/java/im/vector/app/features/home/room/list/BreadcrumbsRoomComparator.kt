/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list

import org.matrix.android.sdk.api.session.room.model.RoomSummary
import javax.inject.Inject

class BreadcrumbsRoomComparator @Inject constructor(
        private val chronologicalRoomComparator: ChronologicalRoomComparator
) : Comparator<RoomSummary> {

    override fun compare(leftRoomSummary: RoomSummary?, rightRoomSummary: RoomSummary?): Int {
        val leftBreadcrumbsIndex = leftRoomSummary?.breadcrumbsIndex ?: RoomSummary.NOT_IN_BREADCRUMBS
        val rightBreadcrumbsIndex = rightRoomSummary?.breadcrumbsIndex ?: RoomSummary.NOT_IN_BREADCRUMBS

        return if (leftBreadcrumbsIndex == RoomSummary.NOT_IN_BREADCRUMBS) {
            if (rightBreadcrumbsIndex == RoomSummary.NOT_IN_BREADCRUMBS) {
                chronologicalRoomComparator.compare(leftRoomSummary, rightRoomSummary)
            } else {
                1
            }
        } else {
            if (rightBreadcrumbsIndex == RoomSummary.NOT_IN_BREADCRUMBS) {
                -1
            } else {
                leftBreadcrumbsIndex - rightBreadcrumbsIndex
            }
        }
    }
}
