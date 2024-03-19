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
 * Triggered when a poll is created or edited.
 */
data class PollCreation(
        /**
         * Whether this poll has been created or edited.
         */
        val action: Action,
        /**
         * Whether this poll is undisclosed.
         */
        val isUndisclosed: Boolean,
        /**
         * Number of answers in the poll.
         */
        val numberOfAnswers: Int,
) : VectorAnalyticsEvent {

    enum class Action {
        /**
         * Newly created poll.
         */
        Create,

        /**
         * Edit of an existing poll.
         */
        Edit,
    }

    override fun getName() = "PollCreation"

    override fun getProperties(): Map<String, Any>? {
        return mutableMapOf<String, Any>().apply {
            put("action", action.name)
            put("isUndisclosed", isUndisclosed)
            put("numberOfAnswers", numberOfAnswers)
        }.takeIf { it.isNotEmpty() }
    }
}
