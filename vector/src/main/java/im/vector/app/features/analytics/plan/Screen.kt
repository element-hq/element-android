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

import im.vector.app.features.analytics.itf.VectorAnalyticsScreen

// GENERATED FILE, DO NOT EDIT. FOR MORE INFORMATION VISIT
// https://github.com/matrix-org/matrix-analytics-events/

/**
 * Triggered when the user changed screen
 */
data class Screen(
        /**
         * How long the screen was displayed for in milliseconds.
         */
        val durationMs: Int? = null,
        val screenName: ScreenName,
) : VectorAnalyticsScreen {

    enum class ScreenName {
        /**
         * The screen shown to create a new (non-direct) room.
         */
        CreateRoom,

        /**
         * The confirmation screen shown before deactivating an account.
         */
        DeactivateAccount,

        /**
         * The form for the forgot password use case
         */
        ForgotPassword,

        /**
         * Legacy: The screen that shows information about a specific group.
         */
        Group,

        /**
         * The Home tab on iOS | possibly the same on Android? | The Home space
         * on Web?
         */
        Home,

        /**
         * The screen that displays the login flow (when the user already has an
         * account).
         */
        Login,

        /**
         * The screen that displays the user's breadcrumbs.
         */
        MobileBreadcrumbs,

        /**
         * The tab on mobile that displays the dialpad.
         */
        MobileDialpad,

        /**
         * The Favourites tab on mobile that lists your favourite people/rooms.
         */
        MobileFavourites,

        /**
         * The screen shown to share a link to download the app.
         */
        MobileInviteFriends,

        /**
         * The People tab on mobile that lists all the DM rooms you have joined.
         */
        MobilePeople,

        /**
         * The Rooms tab on mobile that lists all the (non-direct) rooms you've
         * joined.
         */
        MobileRooms,

        /**
         * The Files tab shown in the global search screen on Mobile.
         */
        MobileSearchFiles,

        /**
         * The Messages tab shown in the global search screen on Mobile.
         */
        MobileSearchMessages,

        /**
         * The People tab shown in the global search screen on Mobile.
         */
        MobileSearchPeople,

        /**
         * The Rooms tab shown in the global search screen on Mobile.
         */
        MobileSearchRooms,

        /**
         * The sidebar shown on mobile with spaces, settings etc.
         */
        MobileSidebar,

        /**
         * The screen shown to select which room directory you'd like to use.
         */
        MobileSwitchDirectory,

        /**
         * Legacy: The screen that shows all groups/communities you have joined.
         */
        MyGroups,

        /**
         * The screen that displays the registration flow (when the user wants
         * to create an account)
         */
        Register,

        /**
         * The screen that displays the messages and events received in a room.
         */
        Room,

        /**
         * The screen shown when tapping the name of a room from the Room
         * screen.
         */
        RoomDetails,

        /**
         * The screen that lists public rooms for you to discover.
         */
        RoomDirectory,

        /**
         * The screen that lists all the user's rooms and let them filter the
         * rooms.
         */
        RoomFilter,

        /**
         * The screen that displays the list of members that are part of a room.
         */
        RoomMembers,

        /**
         * The notifications settings screen shown from the Room Details screen.
         */
        RoomNotifications,

        /**
         * The screen that allows you to search for messages/files in a specific
         * room.
         */
        RoomSearch,

        /**
         * The settings screen shown from the Room Details screen.
         */
        RoomSettings,

        /**
         * The screen that allows you to see all of the files sent in a specific
         * room.
         */
        RoomUploads,

        /**
         * The global settings screen shown in the app.
         */
        Settings,

        /**
         * The settings screen to change the default notification options.
         */
        SettingsDefaultNotifications,

        /**
         * The settings screen to manage notification mentions and keywords.
         */
        SettingsMentionsAndKeywords,

        /**
         * The global security settings screen.
         */
        SettingsSecurity,

        /**
         * The screen shown to create a new direct room.
         */
        StartChat,

        /**
         * A screen that shows information about a room member.
         */
        User,

        /**
         * ?
         */
        WebCompleteSecurity,

        /**
         * ?
         */
        WebE2ESetup,

        /**
         * ?
         */
        WebLoading,

        /**
         * ?
         */
        WebSoftLogout,

        /**
         * The splash screen.
         */
        Welcome,
    }

    override fun getName() = screenName.name

    override fun getProperties(): Map<String, Any>? {
        return mutableMapOf<String, Any>().apply {
            durationMs?.let { put("durationMs", it) }
        }.takeIf { it.isNotEmpty() }
    }
}
