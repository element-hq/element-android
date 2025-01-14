/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.analytics.extensions

import im.vector.app.features.analytics.plan.UserProperties
import im.vector.app.features.home.room.list.home.header.HomeRoomFilter
import im.vector.app.features.onboarding.FtueUseCase

fun FtueUseCase.toTrackingValue(): UserProperties.FtueUseCaseSelection {
    return when (this) {
        FtueUseCase.FRIENDS_FAMILY -> UserProperties.FtueUseCaseSelection.PersonalMessaging
        FtueUseCase.TEAMS -> UserProperties.FtueUseCaseSelection.WorkMessaging
        FtueUseCase.COMMUNITIES -> UserProperties.FtueUseCaseSelection.CommunityMessaging
        FtueUseCase.SKIP -> UserProperties.FtueUseCaseSelection.Skip
    }
}

fun HomeRoomFilter.toTrackingValue(): UserProperties.AllChatsActiveFilter {
    return when (this) {
        HomeRoomFilter.ALL -> UserProperties.AllChatsActiveFilter.All
        HomeRoomFilter.UNREADS -> UserProperties.AllChatsActiveFilter.Unreads
        HomeRoomFilter.FAVOURITES -> UserProperties.AllChatsActiveFilter.Favourites
        HomeRoomFilter.PEOPlE -> UserProperties.AllChatsActiveFilter.People
    }
}
