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

import androidx.test.espresso.Espresso.pressBack
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions.openDrawer
import im.vector.app.R
import im.vector.app.espresso.tools.waitUntilActivityVisible
import im.vector.app.features.createdirect.CreateDirectRoomActivity
import im.vector.app.features.roomdirectory.RoomDirectoryActivity
import java.lang.Thread.sleep

class ElementRobot {

    fun settings(block: SettingsRobot.() -> Unit) {
        openDrawer()
        clickOn(R.id.homeDrawerHeaderSettingsView)
        block(SettingsRobot())
        pressBack()
    }

    fun newDirectMessage(block: NewDirectMessageRobot.() -> Unit) {
        clickOn(R.id.bottom_action_people)
        clickOn(R.id.createChatRoomButton)
        waitUntilActivityVisible<CreateDirectRoomActivity>()
        // close keyboard
        sleep(1000)
        pressBack()
        block(NewDirectMessageRobot())
        pressBack()
    }

    fun newRoom(block: NewRoomRobot.() -> Unit) {
        clickOn(R.id.bottom_action_rooms)
        clickOn(R.id.createGroupRoomButton)
        sleep(1000)
        waitUntilActivityVisible<RoomDirectoryActivity>()
        BaristaVisibilityAssertions.assertDisplayed(R.id.publicRoomsList)
        block(NewRoomRobot())
        pressBack()
    }
}
