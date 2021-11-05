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
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions.openDrawer
import im.vector.app.EspressoHelper
import im.vector.app.R
import im.vector.app.activityIdlingResource
import im.vector.app.espresso.tools.waitUntilActivityVisible
import im.vector.app.espresso.tools.waitUntilViewVisible
import im.vector.app.features.createdirect.CreateDirectRoomActivity
import im.vector.app.features.home.HomeActivity
import im.vector.app.initialSyncIdlingResource
import im.vector.app.withIdlingResource

class ElementRobot {

    fun login(userId: String) {
        val onboardingRobot = OnboardingRobot()
        onboardingRobot.createAccount(userId = userId)

        withIdlingResource(activityIdlingResource(HomeActivity::class.java)) {
            BaristaVisibilityAssertions.assertDisplayed(R.id.roomListContainer)
            ViewActions.closeSoftKeyboard()
        }

        val activity = EspressoHelper.getCurrentActivity()!!
        val uiSession = (activity as HomeActivity).activeSessionHolder.getActiveSession()

        withIdlingResource(initialSyncIdlingResource(uiSession)) {
            BaristaVisibilityAssertions.assertDisplayed(R.id.roomListContainer)
        }
        waitUntilViewVisible(withId(R.id.bottomNavigationView))
    }

    fun settings(block: SettingsRobot.() -> Unit) {
        openDrawer()
        clickOn(R.id.homeDrawerHeaderSettingsView)
        block(SettingsRobot())
        pressBack()
        waitUntilViewVisible(withId(R.id.bottomNavigationView))
    }

    fun newDirectMessage(block: NewDirectMessageRobot.() -> Unit) {
        clickOn(R.id.bottom_action_people)
        clickOn(R.id.createChatRoomButton)
        waitUntilActivityVisible<CreateDirectRoomActivity>()
        // close keyboard
        pressBack()
        block(NewDirectMessageRobot())
        pressBack()
        waitUntilViewVisible(withId(R.id.bottomNavigationView))
    }

    fun newRoom(block: NewRoomRobot.() -> Unit) {
        clickOn(R.id.bottom_action_rooms)
        RoomListRobot().newRoom { block() }
        waitUntilViewVisible(withId(R.id.bottomNavigationView))
    }

    fun roomList(block: RoomListRobot.() -> Unit) {
        clickOn(R.id.bottom_action_rooms)
        block(RoomListRobot())
        waitUntilViewVisible(withId(R.id.bottomNavigationView))
    }
}
