/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import im.vector.app.espresso.tools.waitUntilViewVisible
import im.vector.app.features.home.HomeActivity
import im.vector.app.features.home.room.detail.RoomDetailActivity
import im.vector.app.features.spaces.manage.SpaceManageActivity
import im.vector.lib.strings.CommonStrings

class SpaceCreateRobot {

    fun createAndCrawl(name: String) {
        // public
        clickOn(R.id.publicButton)
        waitUntilViewVisible(withId(R.id.recyclerView))
        onView(ViewMatchers.withHint(CommonStrings.create_room_name_hint)).perform(ViewActions.replaceText(name))
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
                                ViewMatchers.hasDescendant(withText(CommonStrings.room_displayname_empty_room)),
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
        onView(ViewMatchers.withHint(CommonStrings.create_room_name_hint)).perform(ViewActions.replaceText(spaceName))
        clickOn(R.id.nextButton)
        waitUntilViewVisible(withId(R.id.recyclerView))
        clickOn(R.id.nextButton)
//        waitUntilActivityVisible<RoomDetailActivity> {
//            waitUntilDialogVisible(withId(R.id.inviteByMxidButton))
//        }
//        // close invite dialog
//        pressBack()
        waitUntilActivityVisible<RoomDetailActivity> {
            pressBack()
        }
//        waitUntilViewVisible(withId(R.id.timelineRecyclerView))
        // close room
//        pressBack()
        waitUntilViewVisible(withId(R.id.roomListContainer))
    }
}
