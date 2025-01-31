/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.analytics.plan

import im.vector.app.features.analytics.itf.VectorAnalyticsEvent

// GENERATED FILE, DO NOT EDIT. FOR MORE INFORMATION VISIT
// https://github.com/matrix-org/matrix-analytics-events/

/**
 * Triggered when the user creates a room.
 */
data class CreatedRoom(
        /**
         * Whether the room is a DM.
         */
        val isDM: Boolean,
) : VectorAnalyticsEvent {

    override fun getName() = "CreatedRoom"

    override fun getProperties(): Map<String, Any>? {
        return mutableMapOf<String, Any>().apply {
            put("isDM", isDM)
        }.takeIf { it.isNotEmpty() }
    }
}
