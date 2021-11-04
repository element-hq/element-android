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

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions
import com.adevinta.android.barista.interaction.BaristaClickInteractions
import com.adevinta.android.barista.interaction.BaristaDialogInteractions
import com.adevinta.android.barista.interaction.BaristaListInteractions
import im.vector.app.R
import im.vector.app.waitForView

class RoomSettingsRobot {

    fun crawl() {
        // Room settings
        BaristaListInteractions.clickListItem(R.id.matrixProfileRecyclerView, 3)
        navigateToRoomParameters()
        Espresso.pressBack()

        // Notifications
        BaristaListInteractions.clickListItem(R.id.matrixProfileRecyclerView, 5)
        Espresso.pressBack()

        BaristaVisibilityAssertions.assertDisplayed(R.id.roomProfileAvatarView)

        // People
        BaristaListInteractions.clickListItem(R.id.matrixProfileRecyclerView, 7)
        BaristaVisibilityAssertions.assertDisplayed(R.id.inviteUsersButton)
        navigateToRoomPeople()
        // Fab
        navigateToInvite()
        Espresso.pressBack()
        Espresso.pressBack()

        BaristaVisibilityAssertions.assertDisplayed(R.id.roomProfileAvatarView)

        // Uploads
        BaristaListInteractions.clickListItem(R.id.matrixProfileRecyclerView, 9)
        // File tab
        BaristaClickInteractions.clickOn(R.string.uploads_files_title)
        Thread.sleep(1000)
        Espresso.pressBack()

        BaristaVisibilityAssertions.assertDisplayed(R.id.roomProfileAvatarView)

        // Leave
        BaristaListInteractions.clickListItem(R.id.matrixProfileRecyclerView, 13)
        BaristaDialogInteractions.clickDialogNegativeButton()

        // Advanced
        // Room addresses
        BaristaListInteractions.clickListItem(R.id.matrixProfileRecyclerView, 15)
        Espresso.onView(ViewMatchers.isRoot()).perform(waitForView(ViewMatchers.withText(R.string.room_alias_published_alias_title)))
        Espresso.pressBack()

        // Room permissions
        BaristaListInteractions.clickListItem(R.id.matrixProfileRecyclerView, 17)
        Espresso.onView(ViewMatchers.isRoot()).perform(waitForView(ViewMatchers.withText(R.string.room_permissions_title)))
        BaristaClickInteractions.clickOn(R.string.room_permissions_change_room_avatar)
        BaristaDialogInteractions.clickDialogNegativeButton()
        // Toggle
        BaristaClickInteractions.clickOn(R.string.show_advanced)
        BaristaClickInteractions.clickOn(R.string.hide_advanced)
        Espresso.pressBack()

        // Menu share
        // clickMenu(R.id.roomProfileShareAction)
        // pressBack()
    }

    private fun navigateToRoomParameters() {
        // Room history readability
        BaristaListInteractions.clickListItem(R.id.roomSettingsRecyclerView, 4)
        Espresso.pressBack()

        // Room access
        BaristaListInteractions.clickListItem(R.id.roomSettingsRecyclerView, 6)
        Espresso.pressBack()
    }

    private fun navigateToInvite() {
        BaristaVisibilityAssertions.assertDisplayed(R.id.inviteUsersButton)
        BaristaClickInteractions.clickOn(R.id.inviteUsersButton)
        ViewActions.closeSoftKeyboard()
        Espresso.pressBack()
    }

    private fun navigateToRoomPeople() {
        // Open first user
        BaristaListInteractions.clickListItem(R.id.roomSettingsRecyclerView, 1)
        Thread.sleep(1000)
        BaristaVisibilityAssertions.assertDisplayed(R.id.memberProfilePowerLevelView)

        // Verification
        BaristaListInteractions.clickListItem(R.id.matrixProfileRecyclerView, 1)
        BaristaClickInteractions.clickBack()

        // Role
        BaristaListInteractions.clickListItem(R.id.matrixProfileRecyclerView, 3)
        Thread.sleep(1000)
        BaristaDialogInteractions.clickDialogNegativeButton()
        Thread.sleep(1000)
        BaristaClickInteractions.clickBack()
    }
}
