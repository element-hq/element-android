/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.ui.robot

import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogNegativeButton
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem
import im.vector.app.R
import im.vector.app.espresso.tools.waitUntilActivityVisible
import im.vector.app.espresso.tools.waitUntilDialogVisible
import im.vector.app.espresso.tools.waitUntilViewVisible
import im.vector.app.features.roommemberprofile.RoomMemberProfileActivity
import im.vector.lib.strings.CommonStrings

class RoomSettingsRobot {

    fun crawl() {
        // Room settings
        clickListItem(R.id.matrixProfileRecyclerView, 4)
        navigateToRoomParameters()
        pressBack()

        // Notifications
        clickListItem(R.id.matrixProfileRecyclerView, 6)
        pressBack()

        assertDisplayed(R.id.roomProfileAvatarView)

        // People
        clickListItem(R.id.matrixProfileRecyclerView, 8)
        assertDisplayed(R.id.inviteUsersButton)
        navigateToRoomPeople()
        // Fab
        navigateToInvite()
        pressBack()
        pressBack()

        assertDisplayed(R.id.roomProfileAvatarView)

        // Uploads
        clickListItem(R.id.matrixProfileRecyclerView, 10)
        // File tab
        clickOn(CommonStrings.uploads_files_title)
        waitUntilViewVisible(withText(CommonStrings.uploads_media_title))
        pressBack()
        waitUntilViewVisible(withId(R.id.matrixProfileRecyclerView))

        assertDisplayed(R.id.roomProfileAvatarView)

        // Leave
        leaveRoom {
            negativeAction()
        }

        // Advanced
        // Room addresses

        clickListItem(R.id.matrixProfileRecyclerView, 16)
        waitUntilViewVisible(withText(CommonStrings.room_alias_published_alias_title))
        pressBack()

        // Room permissions
        clickListItem(R.id.matrixProfileRecyclerView, 18)
        waitUntilViewVisible(withText(CommonStrings.room_permissions_change_room_avatar))
        clickOn(CommonStrings.room_permissions_change_room_avatar)
        waitUntilDialogVisible(withId(android.R.id.button2))
        clickDialogNegativeButton()
        waitUntilViewVisible(withText(CommonStrings.room_permissions_title))
        // Toggle
        clickOn(CommonStrings.show_advanced)
        clickOn(CommonStrings.hide_advanced)
        pressBack()

        // Menu share
        // clickMenu(R.id.roomProfileShareAction)
        // pressBack()
    }

    private fun leaveRoom(block: DialogRobot.() -> Unit) {
        clickListItem(R.id.matrixProfileRecyclerView, 14)
        waitUntilDialogVisible(withId(android.R.id.button2))
        val dialogRobot = DialogRobot()
        block(dialogRobot)
        if (dialogRobot.returnedToPreviousScreen) {
            waitUntilViewVisible(withId(R.id.matrixProfileRecyclerView))
        }
    }

    private fun navigateToRoomParameters() {
        // Room history readability
        clickListItem(R.id.roomSettingsRecyclerView, 4)
        pressBack()

        // Room access
        clickListItem(R.id.roomSettingsRecyclerView, 6)
        pressBack()
    }

    private fun navigateToInvite() {
        assertDisplayed(R.id.inviteUsersButton)
        clickOn(R.id.inviteUsersButton)
        ViewActions.closeSoftKeyboard()
        pressBack()
    }

    private fun navigateToRoomPeople() {
        // Open first user
        clickListItem(R.id.roomSettingsRecyclerView, 1)
        waitUntilActivityVisible<RoomMemberProfileActivity> {
            waitUntilViewVisible(withId(R.id.memberProfilePowerLevelView))
        }

        // Verification
        clickListItem(R.id.matrixProfileRecyclerView, 1)
        waitUntilViewVisible(withId(R.id.bottomSheetRecyclerView))
        pressBack()
        waitUntilViewVisible(withId(R.id.matrixProfileRecyclerView))

        // Role
        clickListItem(R.id.matrixProfileRecyclerView, 3)
        waitUntilDialogVisible(withId(android.R.id.button2))
        clickDialogNegativeButton()
        waitUntilViewVisible(withId(R.id.matrixProfileRecyclerView))
        pressBack()
    }
}
