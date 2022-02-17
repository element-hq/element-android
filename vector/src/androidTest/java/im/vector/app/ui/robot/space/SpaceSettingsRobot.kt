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
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import im.vector.app.R
import im.vector.app.espresso.tools.waitUntilActivityVisible
import im.vector.app.espresso.tools.waitUntilViewVisible
import im.vector.app.features.roomprofile.settings.joinrule.RoomJoinRuleActivity

class SpaceSettingsRobot {
    fun crawl() {
        Espresso.onView(ViewMatchers.withId(R.id.roomSettingsRecyclerView))
                .perform(
                        RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                                ViewMatchers.hasDescendant(ViewMatchers.withText(R.string.room_settings_space_access_title)),
                                ViewActions.click()
                        )
                )

        waitUntilActivityVisible<RoomJoinRuleActivity> {
            waitUntilViewVisible(ViewMatchers.withId(R.id.genericRecyclerView))
        }

        Espresso.pressBack()

        Espresso.onView(ViewMatchers.withId(R.id.roomSettingsRecyclerView))
                .perform(
                        RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                                ViewMatchers.hasDescendant(ViewMatchers.withText(R.string.space_settings_manage_rooms)),
                                ViewActions.click()
                        )
                )

        waitUntilViewVisible(ViewMatchers.withId(R.id.roomList))
        Espresso.pressBack()

        Espresso.onView(ViewMatchers.withId(R.id.roomSettingsRecyclerView))
                .perform(
                        RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                                ViewMatchers.hasDescendant(ViewMatchers.withText(R.string.space_settings_permissions_title)),
                                ViewActions.click()
                        )
                )

        waitUntilViewVisible(ViewMatchers.withId(R.id.roomSettingsRecyclerView))
        Espresso.pressBack()
        Espresso.pressBack()
    }
}
