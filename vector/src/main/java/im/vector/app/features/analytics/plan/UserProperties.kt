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

// GENERATED FILE, DO NOT EDIT. FOR MORE INFORMATION VISIT
// https://github.com/matrix-org/matrix-analytics-events/

/**
 * The user properties to apply when identifying. This is not an event
 * definition. These properties must all be device independent.
 */
data class UserProperties(
        /**
         * The active filter in the All Chats screen.
         */
        val allChatsActiveFilter: AllChatsActiveFilter? = null,
        /**
         * The selected messaging use case during the onboarding flow.
         */
        val ftueUseCaseSelection: FtueUseCaseSelection? = null,
        /**
         * Number of joined rooms the user has favourited.
         */
        val numFavouriteRooms: Int? = null,
        /**
         * Number of spaces (and sub-spaces) the user is joined to.
         */
        val numSpaces: Int? = null,
) {

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

    enum class AllChatsActiveFilter {

        /**
         * Filters are activated and All is selected.
         */
        All,

        /**
         * Filters are activated and Favourites is selected.
         */
        Favourites,

        /**
         * Filters are activated and People is selected.
         */
        People,

        /**
         * Filters are activated and Unreads is selected.
         */
        Unreads,
    }

    fun getProperties(): Map<String, Any>? {
        return mutableMapOf<String, Any>().apply {
            allChatsActiveFilter?.let { put("allChatsActiveFilter", it.name) }
            ftueUseCaseSelection?.let { put("ftueUseCaseSelection", it.name) }
            numFavouriteRooms?.let { put("numFavouriteRooms", it) }
            numSpaces?.let { put("numSpaces", it) }
        }.takeIf { it.isNotEmpty() }
    }
}
