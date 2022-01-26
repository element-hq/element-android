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

package im.vector.app.ui.robot

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.internal.viewaction.ClickChildAction.clickChildWithId
import im.vector.app.EspressoHelper
import im.vector.app.R
import im.vector.app.espresso.tools.waitUntilActivityVisible
import im.vector.app.espresso.tools.waitUntilDialogVisible
import im.vector.app.espresso.tools.waitUntilViewVisible
import im.vector.app.features.home.HomeActivity
import im.vector.app.features.invite.InviteUsersToRoomActivity
import im.vector.app.features.roomprofile.RoomProfileActivity
import im.vector.app.features.roomprofile.settings.joinrule.RoomJoinRuleActivity
import im.vector.app.features.spaces.SpaceCreationActivity
import im.vector.app.features.spaces.SpaceExploreActivity
import im.vector.app.features.spaces.leave.SpaceLeaveAdvancedActivity
import im.vector.app.features.spaces.manage.SpaceManageActivity
import org.hamcrest.Matchers.allOf
import java.util.UUID

class SpaceRobot {

    fun createSpace() {
        clickOn(R.string.add_space)
        waitUntilActivityVisible<SpaceCreationActivity> {
            waitUntilViewVisible(withId(R.id.privateButton))
        }
        crawlCreate()

        onView(withId(R.id.roomList))
                .perform(
                        RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                                ViewMatchers.hasDescendant(withText(R.string.room_displayname_empty_room)),
                                click()
                        ).atPosition(0)
                )
        clickOn(R.id.spaceAddRoomSaveItem)
        waitUntilActivityVisible<HomeActivity> {
            waitUntilViewVisible(withId(R.id.roomListContainer))
        }
    }

    private fun crawlCreate() {
        //public
        clickOn(R.id.publicButton)
        waitUntilViewVisible(withId(R.id.recyclerView))
        onView(withHint(R.string.create_room_name_hint)).perform(replaceText(UUID.randomUUID().toString()))
        clickOn(R.id.nextButton)
        waitUntilViewVisible(withId(R.id.recyclerView))
        pressBack()
        pressBack()

        //private
        clickOn(R.id.privateButton)
        waitUntilViewVisible(withId(R.id.recyclerView))
        clickOn(R.id.nextButton)

        waitUntilViewVisible(withId(R.id.teammatesButton))
        //me and teammates
        clickOn(R.id.teammatesButton)
        waitUntilViewVisible(withId(R.id.recyclerView))
        clickOn(R.id.nextButton)
        pressBack()
        pressBack()

        //just me
        waitUntilViewVisible(withId(R.id.justMeButton))
        clickOn(R.id.justMeButton)
        waitUntilActivityVisible<SpaceManageActivity> {
            waitUntilViewVisible(withId(R.id.roomList))
        }
    }

    fun openSpaceMenu() {
        waitUntilViewVisible(withId(R.id.groupListView))
        onView(withId(R.id.groupListView))
                .perform(
                        RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                                ViewMatchers.hasDescendant(allOf(withId(R.id.groupTmpLeave), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))),
                                clickChildWithId(R.id.groupTmpLeave)
                        ).atPosition(0)
                )
        waitUntilDialogVisible(withId(R.id.spaceNameView))
    }

    fun invitePeople() {
        clickOn(R.id.invitePeople)
        waitUntilDialogVisible(withId(R.id.inviteByMxidButton))
        clickOn(R.id.inviteByMxidButton)
        waitUntilActivityVisible<InviteUsersToRoomActivity> {
            waitUntilViewVisible(withId(R.id.userListRecyclerView))
        }
        EspressoHelper.getCurrentActivity()!!.finish()
        //close invite dialog
        pressBack()
    }

    fun spaceMembers() {
        clickOn(R.id.showMemberList)
        waitUntilActivityVisible<RoomProfileActivity> {
            waitUntilViewVisible(withId(R.id.roomSettingsRecyclerView))
        }
        pressBack()
    }

    fun spaceSettings() {
        clickOn(R.id.spaceSettings)
        waitUntilActivityVisible<SpaceManageActivity>() {
            waitUntilViewVisible(withId(R.id.roomSettingsRecyclerView))
        }
        crawlSettings()
    }

    private fun crawlSettings() {
        onView(withId(R.id.roomSettingsRecyclerView))
                .perform(
                        RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                                ViewMatchers.hasDescendant(withText(R.string.room_settings_space_access_title)),
                                click()
                        )
                )

        waitUntilActivityVisible<RoomJoinRuleActivity>() {
            waitUntilViewVisible(withId(R.id.genericRecyclerView))
        }

        pressBack()

        onView(withId(R.id.roomSettingsRecyclerView))
                .perform(
                        RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                                ViewMatchers.hasDescendant(withText(R.string.space_settings_manage_rooms)),
                                click()
                        )
                )

        waitUntilViewVisible(withId(R.id.roomList))
        pressBack()

        onView(withId(R.id.roomSettingsRecyclerView))
                .perform(
                        RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                                ViewMatchers.hasDescendant(withText(R.string.space_settings_permissions_title)),
                                click()
                        )
                )

        waitUntilViewVisible(withId(R.id.roomSettingsRecyclerView))
        pressBack()
        pressBack()
    }

    fun exploreRooms() {
        clickOn(R.id.exploreRooms)
        waitUntilActivityVisible<SpaceExploreActivity> {
            waitUntilViewVisible(withId(R.id.spaceDirectoryList))
        }
        pressBack()
    }

    fun addRoom() {
        clickOn(R.id.addRooms)
        waitUntilActivityVisible<SpaceManageActivity> {
            waitUntilViewVisible(withId(R.id.roomList))
        }
        pressBack()
    }

    fun addSpace() {
        clickOn(R.id.addSpaces)
        waitUntilActivityVisible<SpaceManageActivity> {
            waitUntilViewVisible(withId(R.id.roomList))
        }
        pressBack()
    }

    fun leaveSpace() {
        clickOn(R.id.leaveSpace)
        waitUntilDialogVisible(withId(R.id.leaveButton))
        clickOn(R.id.leave_selected)
        waitUntilActivityVisible<SpaceLeaveAdvancedActivity> {
            waitUntilViewVisible(withId(R.id.roomList))
        }
        clickOn(R.id.spaceLeaveButton)
        waitUntilViewVisible(withId(R.id.groupListView))
    }
}
