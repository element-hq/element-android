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
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.adevinta.android.barista.assertion.BaristaListAssertions
import com.adevinta.android.barista.interaction.BaristaClickInteractions
import com.adevinta.android.barista.interaction.BaristaListInteractions
import im.vector.app.R
import im.vector.app.espresso.tools.waitUntilActivityVisible
import im.vector.app.espresso.tools.waitUntilViewVisible
import im.vector.app.features.home.room.detail.RoomDetailActivity

class CreateNewRoomRobot(
        var createdRoom: Boolean = false
) {

    fun createRoom(block: RoomDetailRobot.() -> Unit) {
        createdRoom = true
        BaristaListAssertions.assertListItemCount(R.id.createRoomForm, 12)
        BaristaListInteractions.clickListItemChild(R.id.createRoomForm, 11, R.id.form_submit_button)
        waitUntilActivityVisible<RoomDetailActivity> {
            waitUntilViewVisible(withId(R.id.composerEditText))
        }
        block(RoomDetailRobot())
        pressBack()
    }

    fun crawl() {
        // Room access bottom sheet
        BaristaClickInteractions.clickOn(R.string.room_settings_room_access_private_title)
        pressBack()
    }
}
