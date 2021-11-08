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
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.adevinta.android.barista.interaction.BaristaClickInteractions
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaClickInteractions.longClickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.clickMenu
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu
import im.vector.app.R
import im.vector.app.espresso.tools.waitUntilViewVisible
import im.vector.app.waitForView
import java.lang.Thread.sleep

class RoomDetailRobot {

    fun postMessage(content: String) {
        writeTo(R.id.composerEditText, content)
        clickOn(R.id.sendButton)
    }

    fun crawl() {
        clickOn(R.id.attachmentButton)
        BaristaClickInteractions.clickBack()

        // Menu
        openMenu()
        pressBack()
        clickMenu(R.id.voice_call)
        pressBack()
        clickMenu(R.id.video_call)
        pressBack()
        clickMenu(R.id.search)
        pressBack()
        // Long click on the message
        longClickOnMessageTest()
    }

    private fun longClickOnMessageTest() {
        // Test quick reaction
        longClickOnMessage()
        waitUntilViewVisible(withId(R.id.bottomSheetRecyclerView))
        // Add quick reaction
        clickOn("\uD83D\uDC4D️") // 👍
        waitUntilViewVisible(withId(R.id.composerEditText))

        // Open reactions
        longClickOn("\uD83D\uDC4D️") // 👍
        // wait for bottom sheet
        pressBack()

        // Test add reaction
        longClickOnMessage()
        waitUntilViewVisible(withId(R.id.bottomSheetRecyclerView))
        clickOn(R.string.message_add_reaction)
        // Filter
        // TODO clickMenu(R.id.search)
        // Wait for emoji to load, it's async now
        sleep(2000)
        clickListItem(R.id.emojiRecyclerView, 4)
        waitUntilViewVisible(withId(R.id.composerEditText))

        // Test Edit mode
        longClickOnMessage()
        waitUntilViewVisible(withId(R.id.bottomSheetRecyclerView))
        clickOn(R.string.edit)
        waitUntilViewVisible(withId(R.id.composerEditText))
        // TODO Cancel action
        writeTo(R.id.composerEditText, "Hello universe!")
        // Wait a bit for the keyboard layout to update
        waitUntilViewVisible(withId(R.id.sendButton))
        clickOn(R.id.sendButton)
        // Wait for the UI to update
        sleep(5000)
        // Open edit history
        longClickOnMessage("Hello universe! (edited)")
        waitUntilViewVisible(withId(R.id.bottomSheetRecyclerView))
        clickOn(R.string.message_view_edit_history)
        pressBack()
        waitUntilViewVisible(withId(R.id.composerEditText))
    }

    private fun longClickOnMessage(text: String = "Hello world!") {
        Espresso.onView(withId(R.id.timelineRecyclerView))
                .perform(
                        RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                                ViewMatchers.hasDescendant(ViewMatchers.withText(text)),
                                ViewActions.longClick()
                        )
                )
    }

    fun openSettings(block: RoomSettingsRobot.() -> Unit) {
        clickOn(R.id.roomToolbarTitleView)
        waitForView(withId(R.id.roomProfileAvatarView))
        sleep(1000)
        block(RoomSettingsRobot())
        pressBack()
    }
}
