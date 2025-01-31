/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.ui.robot

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import im.vector.app.R
import im.vector.app.espresso.tools.waitUntilActivityVisible
import im.vector.app.espresso.tools.waitUntilDialogVisible
import im.vector.app.features.roomdirectory.RoomDirectoryActivity
import im.vector.app.ui.robot.settings.labs.LabFeaturesPreferences

class RoomListRobot(private val labsPreferences: LabFeaturesPreferences) {

    fun openRoom(roomName: String, block: RoomDetailRobot.() -> Unit) {
        clickOn(roomName)
        block(RoomDetailRobot())
        pressBack()
    }

    fun verifyCreatedRoom() {
        onView(ViewMatchers.withId(R.id.roomListView))
                .perform(
                        RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                                ViewMatchers.hasDescendant(withText(R.string.room_displayname_empty_room)),
                                ViewActions.longClick()
                        )
                )
        pressBack()
    }

    fun newRoom(block: NewRoomRobot.() -> Unit) {
        if (labsPreferences.isNewAppLayoutEnabled) {
            clickOn(R.id.newLayoutCreateChatButton)
            waitUntilDialogVisible(ViewMatchers.withId(R.id.create_room))
            clickOn(R.id.create_room)
        } else {
            clickOn(R.id.createGroupRoomButton)
            waitUntilActivityVisible<RoomDirectoryActivity> {
                BaristaVisibilityAssertions.assertDisplayed(R.id.publicRoomsList)
            }
        }
        val newRoomRobot = NewRoomRobot(false, labsPreferences)
        block(newRoomRobot)
        if (!newRoomRobot.createdRoom) {
            pressBack()
        }
    }
}
