/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.notifications

/**
 * Data class to hold information about a group of notifications for a room.
 */
data class RoomEventGroupInfo(
        val roomId: String,
        val roomDisplayName: String = "",
        val isDirect: Boolean = false
) {
    // An event in the list has not yet been display
    var hasNewEvent: Boolean = false

    // true if at least one on the not yet displayed event is noisy
    var shouldBing: Boolean = false
    var customSound: String? = null
    var hasSmartReplyError: Boolean = false
    var isUpdated: Boolean = false
}
