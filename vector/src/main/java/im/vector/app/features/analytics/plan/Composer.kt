/*
 * Copyright (c) 2021 New Vector Ltd
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
