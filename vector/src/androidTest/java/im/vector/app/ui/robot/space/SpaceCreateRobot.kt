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
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import im.vector.app.R
import im.vector.app.espresso.tools.waitUntilActivityVisible
import im.vector.app.espresso.tools.waitUntilDialogVisible
import im.vector.app.espresso.tools.waitUntilViewVisible
import im.vector.app.features.home.HomeActivity
import im.vector.app.features.spaces.manage.SpaceManageActivity
import java.util.UUID

class SpaceCreateRobot {

    fun crawl() {
        // public
        clickOn(R.id.publicButton)
        waitUntilViewVisible(withId(R.id.recyclerView))
        onView(ViewMatchers.withHint(R.string.create_room_name_hint)).perform(ViewActions.replaceText(UUID.randomUUID().toString()))
        clickOn(R.id.nextButton)
        waitUntilViewVisible(withId(R.id.recyclerView))
        pressBack()
        pressBack()

        // private
        clickOn(R.id.privateButton)
        waitUntilViewVisible(withId(R.id.recyclerView))
        clickOn(R.id.nextButton)

        waitUntilViewVisible(withId(R.id.teammatesButton))
        // me and teammates
        clickOn(R.id.teammatesButton)
        waitUntilViewVisible(withId(R.id.recyclerView))
        clickOn(R.id.nextButton)
        pressBack()
        pressBack()

        // just me
        waitUntilViewVisible(withId(R.id.justMeButton))
        clickOn(R.id.justMeButton)
        waitUntilActivityVisible<SpaceManageActivity> {
            waitUntilViewVisible(withId(R.id.roomList))
        }

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

    fun createPublicSpace(spaceName: String) {
        clickOn(R.id.publicButton)
        waitUntilViewVisible(withId(R.id.recyclerView))
        onView(ViewMatchers.withHint(R.string.create_room_name_hint)).perform(ViewActions.replaceText(spaceName))
        clickOn(R.id.nextButton)
        waitUntilViewVisible(withId(R.id.recyclerView))
        clickOn(R.id.nextButton)
        waitUntilDialogVisible(withId(R.id.inviteByMxidButton))
        // close invite dialog
        pressBack()
        waitUntilViewVisible(withId(R.id.timelineRecyclerView))
        // close room
        pressBack()
        waitUntilViewVisible(withId(R.id.roomListContainer))
    }
}
