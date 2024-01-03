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
 * Triggered when a poll has been ended.
 */
data class PollEnd(
        /**
         * Do not use this. Remove this property when the kotlin type generator
         * can properly generate types without proprties other than the event
         * name.
         */
        val doNotUse: Boolean? = null,
) : VectorAnalyticsEvent {

    override fun getName() = "PollEnd"

    override fun getProperties(): Map<String, Any>? {
        return mutableMapOf<String, Any>().apply {
            doNotUse?.let { put("doNotUse", it) }
        }.takeIf { it.isNotEmpty() }
    }
}
