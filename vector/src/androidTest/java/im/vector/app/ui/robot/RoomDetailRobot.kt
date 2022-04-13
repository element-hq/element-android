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
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.adevinta.android.barista.interaction.BaristaClickInteractions
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.clickMenu
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu
import com.google.android.material.bottomsheet.BottomSheetBehavior
import im.vector.app.R
import im.vector.app.espresso.tools.waitUntilViewVisible
import im.vector.app.features.home.room.detail.timeline.action.MessageActionsBottomSheet
import im.vector.app.features.home.room.detail.timeline.reactions.ViewReactionsBottomSheet
import im.vector.app.features.reactions.data.EmojiDataSource
import im.vector.app.interactWithSheet
import im.vector.app.withRetry
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
        /* TODO something has changed in the menu :/
        clickMenu(R.id.voice_call)
        pressBack()
         */
        clickMenu(R.id.video_call)
        pressBack()
    }

    fun replyToThread(message: String) {
        openMessageMenu(message) {
            replyInThread()
        }
        val threadMessage = "Hello universe - long message to avoid espresso tapping edited!"
        writeTo(R.id.composerEditText, threadMessage)
        waitUntilViewVisible(withId(R.id.sendButton))
        clickOn(R.id.sendButton)
    }

    fun viewInRoom(message: String) {
        openMessageMenu(message) {
            viewInRoom()
        }
        waitUntilViewVisible(withId(R.id.composerEditText))
    }

    fun crawlMessage(message: String) {
        // Test quick reaction
        val quickReaction = EmojiDataSource.quickEmojis[0] // 👍
        openMessageMenu(message) {
            addQuickReaction(quickReaction)
        }
        waitUntilViewVisible(withText(quickReaction))
        println("Open reactions bottom sheet")
        // Open reactions
        longClickReaction(quickReaction)
        // wait for bottom sheet
        interactWithSheet<ViewReactionsBottomSheet>(withText(R.string.reactions), openState = BottomSheetBehavior.STATE_COLLAPSED) {
            pressBack()
        }
        println("Room Detail Robot: Open reaction from emoji picker")
        // Test add reaction
        openMessageMenu(message) {
            addReactionFromEmojiPicker()
        }
        // Test Edit mode
        openMessageMenu(message) {
            edit()
        }
        // TODO Cancel action
        val edit = "Hello universe - long message to avoid espresso tapping edited!"
        writeTo(R.id.composerEditText, edit)
        // Wait a bit for the keyboard layout to update
        waitUntilViewVisible(withId(R.id.sendButton))
        clickOn(R.id.sendButton)
        // Wait for the UI to update
        waitUntilViewVisible(withText("$edit (edited)"))
        // Open edit history
        openMessageMenu("$edit (edited)") {
            editHistory()
        }
        waitUntilViewVisible(withId(R.id.composerEditText))
    }

    private fun longClickReaction(quickReaction: String) {
        withRetry {
            onView(withText(quickReaction)).perform(longClick())
        }
    }

    fun openMessageMenu(message: String, block: MessageMenuRobot.() -> Unit) {
        onView(withId(R.id.timelineRecyclerView))
                .perform(
                        RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                                ViewMatchers.hasDescendant(withText(message)),
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
        clickMenu(R.id.timeline_setting)
        waitUntilViewVisible(withId(R.id.roomProfileAvatarView))
        sleep(1000)
        block(RoomSettingsRobot())
        pressBack()
    }

    fun openThreadSummaries() {
        clickMenu(R.id.menu_timeline_thread_list)
        waitUntilViewVisible(withId(R.id.threadListRecyclerView))
    }

    fun selectThreadSummariesFilter() {
        clickMenu(R.id.menu_thread_list_filter)
        sleep(1000)
        clickOn(R.id.threadListModalMyThreads)
        pressBack()
    }
}
