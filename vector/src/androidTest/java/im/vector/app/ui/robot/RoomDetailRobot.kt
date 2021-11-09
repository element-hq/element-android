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
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.adevinta.android.barista.interaction.BaristaClickInteractions
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaClickInteractions.longClickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.clickMenu
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu
import im.vector.app.R
import im.vector.app.espresso.tools.waitUntilViewVisible
import im.vector.app.features.home.room.detail.timeline.action.MessageActionsBottomSheet
import im.vector.app.features.reactions.data.EmojiDataSource
import im.vector.app.interactWithSheet
import im.vector.app.waitForView
import java.lang.Thread.sleep

class RoomDetailRobot {

    fun postMessage(content: String) {
        writeTo(R.id.composerEditText, content)
        waitUntilViewVisible(withId(R.id.sendButton))
        clickOn(R.id.sendButton)
        waitUntilViewVisible(withText(content))
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
    }

    fun crawlMessage(message: String) {
        // Test quick reaction
        val quickReaction = EmojiDataSource.quickEmojis[0] // 👍
        openMessageMenu(message) {
            addQuickReaction(quickReaction)
        }
        // Open reactions
        longClickOn(quickReaction)
        // wait for bottom sheet
        pressBack()
        // Test add reaction
        openMessageMenu(message) {
            addReactionFromEmojiPicker()
        }
        // Test Edit mode
        openMessageMenu(message) {
            edit()
        }
        // TODO Cancel action
        writeTo(R.id.composerEditText, "Hello universe!")
        // Wait a bit for the keyboard layout to update
        waitUntilViewVisible(withId(R.id.sendButton))
        clickOn(R.id.sendButton)
        // Wait for the UI to update
        waitUntilViewVisible(withText("Hello universe! (edited)"))
        // Open edit history
        openMessageMenu("Hello universe! (edited)") {
            editHistory()
        }
    }

    fun openMessageMenu(message: String, block: MessageMenuRobot.() -> Unit) {
        onView(withId(R.id.timelineRecyclerView))
                .perform(
                        RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                                ViewMatchers.hasDescendant(ViewMatchers.withText(message)),
                                ViewActions.longClick()
                        )
                )
        interactWithSheet<MessageActionsBottomSheet>(contentMatcher = withId(R.id.bottomSheetRecyclerView)) {
            val messageMenuRobot = MessageMenuRobot()
            block(messageMenuRobot)
            if (!messageMenuRobot.autoClosed) {
                pressBack()
            }
        }
    }

    fun openSettings(block: RoomSettingsRobot.() -> Unit) {
        clickOn(R.id.roomToolbarTitleView)
        waitForView(withId(R.id.roomProfileAvatarView))
        sleep(1000)
        block(RoomSettingsRobot())
        pressBack()
    }
}
