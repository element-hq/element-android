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
 * Triggered when the user clicks/taps/activates a UI element.
 */
data class Interaction(
        /**
         * The index of the element, if its in a list of elements.
         */
        val index: Int? = null,
        /**
         * The manner with which the user activated the UI element.
         */
        val interactionType: InteractionType? = null,
        /**
         * The unique name of this element.
         */
        val name: Name,
) : VectorAnalyticsEvent {

    enum class Name {
        /**
         * User tapped on Add to Home button on Room Details screen.
         */
        MobileRoomAddHome,

        /**
         * User tapped on Leave Room button on Room Details screen.
         */
        MobileRoomLeave,

        /**
         * User tapped on Threads button on Room screen.
         */
        MobileRoomThreadListButton,

        /**
         * User tapped on a thread summary item on Room screen.
         */
        MobileRoomThreadSummaryItem,

        /**
         * User tapped on the filter button on ThreadList screen.
         */
        MobileThreadListFilterItem,

        /**
         * User selected a thread on ThreadList screen.
         */
        MobileThreadListThreadItem,

        /**
         * User tapped the already selected space from the space list.
         */
        SpacePanelSelectedSpace,

        /**
         * User tapped an unselected space from the space list -> space
         * switching should occur.
         */
        SpacePanelSwitchSpace,

        /**
         * User clicked the create room button in the add existing room to space
         * dialog in Element Web/Desktop.
         */
        WebAddExistingToSpaceDialogCreateRoomButton,

        /**
         * User clicked the create room button in the home page of Element
         * Web/Desktop.
         */
        WebHomeCreateRoomButton,

        /**
         * User interacted with pin to sidebar checkboxes in the quick settings
         * menu of Element Web/Desktop.
         */
        WebQuickSettingsPinToSidebarCheckbox,

        /**
         * User interacted with the theme dropdown in the quick settings menu of
         * Element Web/Desktop.
         */
        WebQuickSettingsThemeDropdown,

        /**
         * User accessed the room invite flow using the button at the top of the
         * room member list in the right panel of Element Web/Desktop.
         */
        WebRightPanelMemberListInviteButton,

        /**
         * User accessed room member list using the 'People' button in the right
         * panel room summary card of Element Web/Desktop.
         */
        WebRightPanelRoomInfoPeopleButton,

        /**
         * User accessed room settings using the 'Settings' button in the right
         * panel room summary card of Element Web/Desktop.
         */
        WebRightPanelRoomInfoSettingsButton,

        /**
         * User accessed room member list using the back button in the right
         * panel user info card of Element Web/Desktop.
         */
        WebRightPanelRoomUserInfoBackButton,

        /**
         * User invited someone to room by clicking invite on the right panel
         * user info card in Element Web/Desktop.
         */
        WebRightPanelRoomUserInfoInviteButton,

        /**
         * User clicked the threads 'show' filter dropdown in the threads panel
         * in Element Web/Desktop.
         */
        WebRightPanelThreadPanelFilterDropdown,

        /**
         * User clicked the create room button in the room directory of Element
         * Web/Desktop.
         */
        WebRoomDirectoryCreateRoomButton,

        /**
         * User clicked the Threads button in the top right of a room in Element
         * Web/Desktop.
         */
        WebRoomHeaderButtonsThreadsButton,

        /**
         * User adjusted their favourites using the context menu on the header
         * of a room in Element Web/Desktop.
         */
        WebRoomHeaderContextMenuFavouriteToggle,

        /**
         * User accessed the room invite flow using the context menu on the
         * header of a room in Element Web/Desktop.
         */
        WebRoomHeaderContextMenuInviteItem,

        /**
         * User interacted with leave action in the context menu on the header
         * of a room in Element Web/Desktop.
         */
        WebRoomHeaderContextMenuLeaveItem,

        /**
         * User accessed their room notification settings via the context menu
         * on the header of a room in Element Web/Desktop.
         */
        WebRoomHeaderContextMenuNotificationsItem,

        /**
         * User accessed room member list using the context menu on the header
         * of a room in Element Web/Desktop.
         */
        WebRoomHeaderContextMenuPeopleItem,

        /**
         * User accessed room settings using the context menu on the header of a
         * room in Element Web/Desktop.
         */
        WebRoomHeaderContextMenuSettingsItem,

        /**
         * User clicked the create room button in the + context menu of the room
         * list header in Element Web/Desktop.
         */
        WebRoomListHeaderPlusMenuCreateRoomItem,

        /**
         * User clicked the explore rooms button in the + context menu of the
         * room list header in Element Web/Desktop.
         */
        WebRoomListHeaderPlusMenuExploreRoomsItem,

        /**
         * User adjusted their favourites using the context menu on a room tile
         * in the room list in Element Web/Desktop.
         */
        WebRoomListRoomTileContextMenuFavouriteToggle,

        /**
         * User accessed the room invite flow using the context menu on a room
         * tile in the room list in Element Web/Desktop.
         */
        WebRoomListRoomTileContextMenuInviteItem,

        /**
         * User interacted with leave action in the context menu on a room tile
         * in the room list in Element Web/Desktop.
         */
        WebRoomListRoomTileContextMenuLeaveItem,

        /**
         * User accessed room settings using the context menu on a room tile in
         * the room list in Element Web/Desktop.
         */
        WebRoomListRoomTileContextMenuSettingsItem,

        /**
         * User accessed their room notification settings via the context menu
         * on a room tile in the room list in Element Web/Desktop.
         */
        WebRoomListRoomTileNotificationsMenu,

        /**
         * User clicked the create room button in the + context menu of the
         * rooms sublist in Element Web/Desktop.
         */
        WebRoomListRoomsSublistPlusMenuCreateRoomItem,

        /**
         * User clicked the explore rooms button in the + context menu of the
         * rooms sublist in Element Web/Desktop.
         */
        WebRoomListRoomsSublistPlusMenuExploreRoomsItem,

        /**
         * User interacted with leave action in the general tab of the room
         * settings dialog in Element Web/Desktop.
         */
        WebRoomSettingsLeaveButton,

        /**
         * User interacted with the prompt to create a new room when adjusting
         * security settings in an existing room in Element Web/Desktop.
         */
        WebRoomSettingsSecurityTabCreateNewRoomButton,

        /**
         * User clicked a thread summary in the timeline of a room in Element
         * Web/Desktop.
         */
        WebRoomTimelineThreadSummaryButton,

        /**
         * User interacted with the theme radio selector in the Appearance tab
         * of Settings in Element Web/Desktop.
         */
        WebSettingsAppearanceTabThemeSelector,

        /**
         * User interacted with the pre-built space checkboxes in the Sidebar
         * tab of Settings in Element Web/Desktop.
         */
        WebSettingsSidebarTabSpacesCheckbox,

        /**
         * User clicked the explore rooms button in the context menu of a space
         * in Element Web/Desktop.
         */
        WebSpaceContextMenuExploreRoomsItem,

        /**
         * User clicked the home button in the context menu of a space in
         * Element Web/Desktop.
         */
        WebSpaceContextMenuHomeItem,

        /**
         * User clicked the new room button in the context menu of a space in
         * Element Web/Desktop.
         */
        WebSpaceContextMenuNewRoomItem,

        /**
         * User clicked the new room button in the context menu on the space
         * home in Element Web/Desktop.
         */
        WebSpaceHomeCreateRoomButton,

        /**
         * User clicked the back button on a Thread view going back to the
         * Threads Panel of Element Web/Desktop.
         */
        WebThreadViewBackButton,

        /**
         * User selected a thread in the Threads panel in Element Web/Desktop.
         */
        WebThreadsPanelThreadItem,

        /**
         * User clicked the theme toggle button in the user menu of Element
         * Web/Desktop.
         */
        WebUserMenuThemeToggleButton,
    }

    enum class InteractionType {
        Keyboard,
        Pointer,
        Touch,
    }

    override fun getName() = "Interaction"

    override fun getProperties(): Map<String, Any>? {
        return mutableMapOf<String, Any>().apply {
            index?.let { put("index", it) }
            interactionType?.let { put("interactionType", it.name) }
            put("name", name.name)
        }.takeIf { it.isNotEmpty() }
    }
}
