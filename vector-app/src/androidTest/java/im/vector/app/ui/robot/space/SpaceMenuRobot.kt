/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.ui.robot.space

import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import im.vector.app.R
import im.vector.app.clickOnSheet
import im.vector.app.espresso.tools.waitUntilActivityVisible
import im.vector.app.espresso.tools.waitUntilDialogVisible
import im.vector.app.espresso.tools.waitUntilViewVisible
import im.vector.app.features.invite.InviteUsersToRoomActivity
import im.vector.app.features.roomprofile.RoomProfileActivity
import im.vector.app.features.spaces.SpaceExploreActivity
import im.vector.app.features.spaces.leave.SpaceLeaveAdvancedActivity
import im.vector.app.features.spaces.manage.SpaceManageActivity

class SpaceMenuRobot {

    fun invitePeople() = apply {
        clickOnSheet(R.id.invitePeople)
        waitUntilDialogVisible(ViewMatchers.withId(R.id.inviteByMxidButton))
        clickOn(R.id.inviteByMxidButton)
        waitUntilActivityVisible<InviteUsersToRoomActivity> {
            waitUntilViewVisible(ViewMatchers.withId(R.id.userListRecyclerView))
        }
        // close keyboard
        Espresso.pressBack()
        // close invite view
        Espresso.pressBack()
    }

    fun spaceMembers() {
        clickOnSheet(R.id.showMemberList)
        waitUntilActivityVisible<RoomProfileActivity> {
            waitUntilViewVisible(ViewMatchers.withId(R.id.roomSettingsRecyclerView))
        }
        Espresso.pressBack()
    }

    fun spaceSettings(block: SpaceSettingsRobot.() -> Unit) {
        clickOnSheet(R.id.spaceSettings)
        waitUntilActivityVisible<SpaceManageActivity> {
            waitUntilViewVisible(ViewMatchers.withId(R.id.roomSettingsRecyclerView))
        }
        block(SpaceSettingsRobot())
    }

    fun exploreRooms() {
        clickOnSheet(R.id.exploreRooms)
        waitUntilActivityVisible<SpaceExploreActivity> {
            waitUntilViewVisible(ViewMatchers.withId(R.id.spaceDirectoryList))
        }
        Espresso.pressBack()
    }

    fun addRoom() = apply {
        clickOnSheet(R.id.addRooms)
        waitUntilActivityVisible<SpaceManageActivity> {
            waitUntilViewVisible(ViewMatchers.withId(R.id.roomList))
        }
        Espresso.pressBack()
    }

    fun addSpace() = apply {
        clickOnSheet(R.id.addSpaces)
        waitUntilActivityVisible<SpaceManageActivity> {
            waitUntilViewVisible(ViewMatchers.withId(R.id.roomList))
        }
        Espresso.pressBack()
    }

    fun leaveSpace() {
        clickOnSheet(R.id.leaveSpace)
        waitUntilActivityVisible<SpaceLeaveAdvancedActivity> {
            waitUntilViewVisible(ViewMatchers.withId(R.id.roomList))
            clickOn(R.id.spaceLeaveSelectAll)
            clickOn(R.id.spaceLeaveButton)
        }
    }
}
