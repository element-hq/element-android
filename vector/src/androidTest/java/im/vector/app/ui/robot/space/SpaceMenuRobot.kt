/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.ui.robot.space

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.internal.viewaction.ClickChildAction
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
import org.hamcrest.Matchers

class SpaceMenuRobot {

    fun openMenu(spaceName: String) {
        waitUntilViewVisible(ViewMatchers.withId(R.id.groupListView))
        onView(ViewMatchers.withId(R.id.groupListView))
                .perform(
                        RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                                ViewMatchers.hasDescendant(Matchers.allOf(ViewMatchers.withId(R.id.groupNameView), ViewMatchers.withText(spaceName))),
                                ClickChildAction.clickChildWithId(R.id.groupTmpLeave)
                        ).atPosition(0)
                )
        waitUntilDialogVisible(ViewMatchers.withId(R.id.spaceNameView))
    }

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
        waitUntilDialogVisible(ViewMatchers.withId(R.id.leaveButton))
        clickOn(R.id.leave_selected)
        waitUntilActivityVisible<SpaceLeaveAdvancedActivity> {
            waitUntilViewVisible(ViewMatchers.withId(R.id.roomList))
        }
        clickOn(R.id.spaceLeaveButton)
        waitUntilViewVisible(ViewMatchers.withId(R.id.groupListView))
    }
}
