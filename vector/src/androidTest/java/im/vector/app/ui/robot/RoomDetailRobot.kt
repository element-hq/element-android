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
import com.adevinta.android.barista.interaction.BaristaClickInteractions
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions
import com.adevinta.android.barista.interaction.BaristaListInteractions
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions
import im.vector.app.R
import im.vector.app.waitForView

class RoomDetailRobot {

    fun postMessage(content: String) {
        BaristaEditTextInteractions.writeTo(R.id.composerEditText, content)
        BaristaClickInteractions.clickOn(R.id.sendButton)
    }

    fun crawl() {
        BaristaClickInteractions.clickOn(R.id.attachmentButton)
        BaristaClickInteractions.clickBack()

        // Menu
        BaristaMenuClickInteractions.openMenu()
        Espresso.pressBack()
        BaristaMenuClickInteractions.clickMenu(R.id.voice_call)
        Espresso.pressBack()
        BaristaMenuClickInteractions.clickMenu(R.id.video_call)
        Espresso.pressBack()
        BaristaMenuClickInteractions.clickMenu(R.id.search)
        Espresso.pressBack()
        // Long click on the message
        longClickOnMessageTest()
    }

    private fun longClickOnMessageTest() {
        // Test quick reaction
        longClickOnMessage()
        // Add quick reaction
        BaristaClickInteractions.clickOn("\uD83D\uDC4D️") // 👍

        Thread.sleep(1000)

        // Open reactions
        BaristaClickInteractions.longClickOn("\uD83D\uDC4D️") // 👍
        Espresso.pressBack()

        // Test add reaction
        longClickOnMessage()
        BaristaClickInteractions.clickOn(R.string.message_add_reaction)
        // Filter
        // TODO clickMenu(R.id.search)
        // Wait for emoji to load, it's async now
        Thread.sleep(2000)
        BaristaListInteractions.clickListItem(R.id.emojiRecyclerView, 4)
        Thread.sleep(2000)

        // Test Edit mode
        longClickOnMessage()
        BaristaClickInteractions.clickOn(R.string.edit)
        // TODO Cancel action
        BaristaEditTextInteractions.writeTo(R.id.composerEditText, "Hello universe!")
        // Wait a bit for the keyboard layout to update
        Thread.sleep(30)
        BaristaClickInteractions.clickOn(R.id.sendButton)
        // Wait for the UI to update
        Thread.sleep(1000)
        // Open edit history
        longClickOnMessage("Hello universe! (edited)")
        BaristaClickInteractions.clickOn(R.string.message_view_edit_history)
        Espresso.pressBack()
    }

    private fun longClickOnMessage(text: String = "Hello world!") {
        Espresso.onView(ViewMatchers.withId(R.id.timelineRecyclerView))
                .perform(
                        RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                                ViewMatchers.hasDescendant(ViewMatchers.withText(text)),
                                ViewActions.longClick()
                        )
                )
    }

    fun openSettings(block: RoomSettingsRobot.() -> Unit) {
        BaristaClickInteractions.clickOn(R.id.roomToolbarTitleView)
        waitForView(ViewMatchers.withId(R.id.roomProfileAvatarView))
        block(RoomSettingsRobot())
        Espresso.pressBack()
    }
}
