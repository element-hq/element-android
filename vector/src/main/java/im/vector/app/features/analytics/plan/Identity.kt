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
 * The user properties to apply when identifying
 */
data class Identity(
        /**
         * The selected messaging use case during the onboarding flow.
         */
        val ftueUseCaseSelection: FtueUseCaseSelection? = null,
) : VectorAnalyticsEvent {

    enum class FtueUseCaseSelection {
        /**
         * The third option, Communities.
         */
        CommunityMessaging,

        /**
         * The first option, Friends and family.
         */
        PersonalMessaging,

        /**
         * The footer option to skip the question.
         */
        Skip,

        /**
         * The second option, Teams.
         */
        WorkMessaging,
    }

    override fun getName() = "Identity"

    override fun getProperties(): Map<String, Any>? {
        return mutableMapOf<String, Any>().apply {
            ftueUseCaseSelection?.let { put("ftueUseCaseSelection", it.name) }
        }.takeIf { it.isNotEmpty() }
    }
}
