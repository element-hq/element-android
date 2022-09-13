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
 * Triggered when an error occurred in a call.
 */
data class CallError(
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

    override fun getName() = "CallError"

    override fun getProperties(): Map<String, Any>? {
        return mutableMapOf<String, Any>().apply {
            put("isVideo", isVideo)
            put("numParticipants", numParticipants)
            put("placed", placed)
        }.takeIf { it.isNotEmpty() }
    }
}
