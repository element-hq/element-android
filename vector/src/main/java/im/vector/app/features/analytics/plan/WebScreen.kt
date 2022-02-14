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
 * Triggered when the user changed screen on Element Web/Desktop
 */
data class WebScreen(
        val $current_url: $current_url,
        /**
         * How long the screen took to load, if applicable.
         */
        val durationMs: Int? = null,
) : VectorAnalyticsEvent {

    enum class $current_url {
        /**
         * Screen showing flow to trust this new device with cross-signing.
         */
        CompleteSecurity,

        /**
         * The screen shown to create a new (non-direct) room.
         */
        CreateRoom,

        /**
         * The confirmation screen shown before deactivating an account.
         */
        DeactivateAccount,

        /**
         * Screen showing flow to setup SSSS / cross-signing on this account.
         */
        E2ESetup,

        /**
         * The form for the forgot password use case
         */
        ForgotPassword,

        /**
         * Legacy: The screen that shows information about a specific group.
         */
        Group,

        /**
         * Home page.
         */
        Home,

        /**
         * Screen showing loading spinner.
         */
        Loading,

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
         * Screen showing device has been soft logged out by the server.
         */
        SoftLogout,

        /**
         * Screen that displays the list of rooms and spaces of a space
         */
        SpaceExploreRooms,

        /**
         * The screen shown to create a new direct room.
         */
        StartChat,

        /**
         * A screen that shows information about a room member.
         */
        User,

        /**
         * Legacy: screen showing User Settings Flair Tab.
         */
        UserSettingFlair,

        /**
         * Screen showing User Settings Mjolnir (labs) Tab.
         */
        UserSettingMjolnir,

        /**
         * Screen showing User Settings Appearance Tab.
         */
        UserSettingsAppearance,

        /**
         * Screen showing User Settings General Tab.
         */
        UserSettingsGeneral,

        /**
         * Screen showing User Settings Help & About Tab.
         */
        UserSettingsHelpAbout,

        /**
         * Screen showing User Settings Ignored Users Tab.
         */
        UserSettingsIgnoredUsers,

        /**
         * Screen showing User Settings Keyboard Tab.
         */
        UserSettingsKeyboard,

        /**
         * Screen showing User Settings Labs Tab.
         */
        UserSettingsLabs,

        /**
         * Screen showing User Settings Notifications Tab.
         */
        UserSettingsNotifications,

        /**
         * Screen showing User Settings Preferences Tab.
         */
        UserSettingsPreferences,

        /**
         * Screen showing User Settings Security & Privacy Tab.
         */
        UserSettingsSecurityPrivacy,

        /**
         * Screen showing User Settings Sidebar Tab.
         */
        UserSettingsSidebar,

        /**
         * Screen showing User Settings Voice & Video Tab.
         */
        UserSettingsVoiceVideo,

        /**
         * The splash screen.
         */
        Welcome,
    }

    override fun getName() = "$pageview"

    override fun getProperties(): Map<String, Any>? {
        return mutableMapOf<String, Any>().apply {
            put("$current_url", $current_url.name)
            durationMs?.let { put("durationMs", it) }
        }.takeIf { it.isNotEmpty() }
    }
}
