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
 * Triggered when the user sends a message via the composer.
 */
data class Composer(
        /**
         * Whether the user was using the composer inside of a thread.
         */
        val inThread: Boolean,
        /**
         * Whether the user's composer interaction was editing a previously sent
         * event.
         */
        val isEditing: Boolean,
        /**
         * Whether the user's composer interaction was a reply to a previously
         * sent event.
         */
        val isReply: Boolean,
        /**
         * Whether this message begins a new thread or not.
         */
        val startsThread: Boolean? = null,
) : VectorAnalyticsEvent {

    override fun getName() = "Composer"

    override fun getProperties(): Map<String, Any>? {
        return mutableMapOf<String, Any>().apply {
            put("inThread", inThread)
            put("isEditing", isEditing)
            put("isReply", isReply)
            startsThread?.let { put("startsThread", it) }
        }.takeIf { it.isNotEmpty() }
    }
}
