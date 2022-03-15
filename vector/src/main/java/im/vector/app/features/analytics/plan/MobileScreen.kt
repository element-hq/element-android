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
 * Triggered when the user changed screen on Element Android/iOS
 */
data class MobileScreen(
        /**
         * How long the screen was displayed for in milliseconds.
         */
        val durationMs: Int? = null,
        val screenName: ScreenName,
) : VectorAnalyticsScreen {

    enum class ScreenName {
        /**
         * The screen that displays the user's breadcrumbs.
         */
        Breadcrumbs,

        /**
         * The screen shown to create a new (non-direct) room.
         */
        CreateRoom,

        /**
         * The confirmation screen shown before deactivating an account.
         */
        DeactivateAccount,

        /**
         * The tab on mobile that displays the dialpad.
         */
        Dialpad,

        /**
         * The Favourites tab on mobile that lists your favourite people/rooms.
         */
        Favourites,

        /**
         * The form for the forgot password use case
         */
        ForgotPassword,

        /**
         * Legacy: The screen that shows information about a specific group.
         */
        Group,

        /**
         * The Home tab on iOS | possibly the same on Android?
         */
        Home,

        /**
         * The screen shown to share a link to download the app.
         */
        InviteFriends,

        /**
         * The screen that displays the login flow (when the user already has an
         * account).
         */
        Login,

        /**
         * Legacy: The screen that shows all groups/communities you have joined.
         */
        MyGroups,

        /**
         * The People tab on mobile that lists all the DM rooms you have joined.
         */
        People,

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
         * The room addresses screen shown from the Room Details screen.
         */
        RoomAddresses,

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
         * The roles permissions screen shown from the Room Details screen.
         */
        RoomPermissions,

        /**
         * Screen that displays room preview if user hasn't joined yet
         */
        RoomPreview,

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
         * The Rooms tab on mobile that lists all the (non-direct) rooms you've
         * joined.
         */
        Rooms,

        /**
         * The Files tab shown in the global search screen on Mobile.
         */
        SearchFiles,

        /**
         * The Messages tab shown in the global search screen on Mobile.
         */
        SearchMessages,

        /**
         * The People tab shown in the global search screen on Mobile.
         */
        SearchPeople,

        /**
         * The Rooms tab shown in the global search screen on Mobile.
         */
        SearchRooms,

        /**
         * The global settings screen shown in the app.
         */
        Settings,

        /**
         * The advanced settings screen (developer mode, rageshake, push
         * notification rules)
         */
        SettingsAdvanced,

        /**
         * The settings screen to change the default notification options.
         */
        SettingsDefaultNotifications,

        /**
         * The settings screen with general profile settings.
         */
        SettingsGeneral,

        /**
         * The Help and About screen
         */
        SettingsHelp,

        /**
         * The settings screen with list of the ignored users.
         */
        SettingsIgnoredUsers,

        /**
         * The experimental features settings screen,
         */
        SettingsLabs,

        /**
         * The settings screen with legals information
         */
        SettingsLegals,

        /**
         * The settings screen to manage notification mentions and keywords.
         */
        SettingsMentionsAndKeywords,

        /**
         * The notifications settings screen.
         */
        SettingsNotifications,

        /**
         * The preferences screen (theme, language, editor preferences, etc.
         */
        SettingsPreferences,

        /**
         * The global security settings screen.
         */
        SettingsSecurity,

        /**
         * The calls settings screen.
         */
        SettingsVoiceVideo,

        /**
         * The sidebar shown on mobile with spaces, settings etc.
         */
        Sidebar,

        /**
         * Screen that displays the list of rooms and spaces of a space
         */
        SpaceExploreRooms,

        /**
         * Screen that displays the list of members of a space
         */
        SpaceMembers,

        /**
         * The bottom sheet that list all space options
         */
        SpaceMenu,

        /**
         * The screen shown to create a new direct room.
         */
        StartChat,

        /**
         * The screen shown to select which room directory you'd like to use.
         */
        SwitchDirectory,

        /**
         * Screen that displays list of threads for a room
         */
        ThreadList,

        /**
         * A screen that shows information about a room member.
         */
        User,

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
