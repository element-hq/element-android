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
 * Triggered when a call is started.
 */
data class CallStarted(
        /**
         * Whether its a video call or not.
         */
        val isVideo: Boolean,
        /**
         * Number of participants in the call.
         */
        val numParticipants: Int,
        /**
         * Whether this user placed it.
         */
        val placed: Boolean,
) : VectorAnalyticsEvent {

    override fun getName() = "CallStarted"

    override fun getProperties(): Map<String, Any>? {
        return mutableMapOf<String, Any>().apply {
            put("isVideo", isVideo)
            put("numParticipants", numParticipants)
            put("placed", placed)
        }.takeIf { it.isNotEmpty() }
    }
}
