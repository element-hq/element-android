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
         * Whether the user has the favourites space enabled
         */
        val WebMetaSpaceFavouritesEnabled: Boolean? = null,
        /**
         * Whether the user has the home space set to all rooms
         */
        val WebMetaSpaceHomeAllRooms: Boolean? = null,
        /**
         * Whether the user has the home space enabled
         */
        val WebMetaSpaceHomeEnabled: Boolean? = null,
        /**
         * Whether the user has the other rooms space enabled
         */
        val WebMetaSpaceOrphansEnabled: Boolean? = null,
        /**
         * Whether the user has the people space enabled
         */
        val WebMetaSpacePeopleEnabled: Boolean? = null,
        /**
         * The selected messaging use case during the onboarding flow.
         */
        val ftueUseCaseSelection: FtueUseCaseSelection? = null,
        /**
         * Number of joined rooms the user has favourited
         */
        val numFavouriteRooms: Int? = null,
        /**
         * Number of spaces (and sub-spaces) the user is joined to
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

    fun getProperties(): Map<String, Any>? {
        return mutableMapOf<String, Any>().apply {
            WebMetaSpaceFavouritesEnabled?.let { put("WebMetaSpaceFavouritesEnabled", it) }
            WebMetaSpaceHomeAllRooms?.let { put("WebMetaSpaceHomeAllRooms", it) }
            WebMetaSpaceHomeEnabled?.let { put("WebMetaSpaceHomeEnabled", it) }
            WebMetaSpaceOrphansEnabled?.let { put("WebMetaSpaceOrphansEnabled", it) }
            WebMetaSpacePeopleEnabled?.let { put("WebMetaSpacePeopleEnabled", it) }
            ftueUseCaseSelection?.let { put("ftueUseCaseSelection", it.name) }
            numFavouriteRooms?.let { put("numFavouriteRooms", it) }
            numSpaces?.let { put("numSpaces", it) }
        }.takeIf { it.isNotEmpty() }
    }
}
