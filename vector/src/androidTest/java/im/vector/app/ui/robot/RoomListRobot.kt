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
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions
import com.adevinta.android.barista.interaction.BaristaClickInteractions
import im.vector.app.R
import im.vector.app.espresso.tools.waitUntilActivityVisible
import im.vector.app.features.roomdirectory.RoomDirectoryActivity

class RoomListRobot {

    fun verifyCreatedRoom() {
        Espresso.onView(ViewMatchers.withId(R.id.roomListView))
                .perform(
                        RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                                ViewMatchers.hasDescendant(ViewMatchers.withText(R.string.room_displayname_empty_room)),
                                ViewActions.longClick()
                        )
                )
        Espresso.pressBack()
    }

    fun newRoom(block: NewRoomRobot.() -> Unit) {
        BaristaClickInteractions.clickOn(R.id.createGroupRoomButton)
        waitUntilActivityVisible<RoomDirectoryActivity> {
            BaristaVisibilityAssertions.assertDisplayed(R.id.publicRoomsList)
        }
        block(NewRoomRobot())
        Espresso.pressBack()
    }
}
