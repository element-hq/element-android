/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
         * Whether the user has the favourites space enabled.
         */
        val webMetaSpaceFavouritesEnabled: Boolean? = null,
        /**
         * Whether the user has the home space set to all rooms.
         */
        val webMetaSpaceHomeAllRooms: Boolean? = null,
        /**
         * Whether the user has the home space enabled.
         */
        val webMetaSpaceHomeEnabled: Boolean? = null,
        /**
         * Whether the user has the other rooms space enabled.
         */
        val webMetaSpaceOrphansEnabled: Boolean? = null,
        /**
         * Whether the user has the people space enabled.
         */
        val webMetaSpacePeopleEnabled: Boolean? = null,
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
            webMetaSpaceFavouritesEnabled?.let { put("WebMetaSpaceFavouritesEnabled", it) }
            webMetaSpaceHomeAllRooms?.let { put("WebMetaSpaceHomeAllRooms", it) }
            webMetaSpaceHomeEnabled?.let { put("WebMetaSpaceHomeEnabled", it) }
            webMetaSpaceOrphansEnabled?.let { put("WebMetaSpaceOrphansEnabled", it) }
            webMetaSpacePeopleEnabled?.let { put("WebMetaSpacePeopleEnabled", it) }
            allChatsActiveFilter?.let { put("allChatsActiveFilter", it.name) }
            ftueUseCaseSelection?.let { put("ftueUseCaseSelection", it.name) }
            numFavouriteRooms?.let { put("numFavouriteRooms", it) }
            numSpaces?.let { put("numSpaces", it) }
        }.takeIf { it.isNotEmpty() }
    }
}
