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
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem
import com.google.android.material.bottomsheet.BottomSheetBehavior
import im.vector.app.R
import im.vector.app.espresso.tools.waitUntilActivityVisible
import im.vector.app.espresso.tools.waitUntilViewVisible
import im.vector.app.features.home.room.detail.timeline.edithistory.ViewEditHistoryBottomSheet
import im.vector.app.features.reactions.EmojiReactionPickerActivity
import im.vector.app.interactWithSheet
import java.lang.Thread.sleep

class MessageMenuRobot(
        var autoClosed: Boolean = false
) {

    fun viewSource() {
        clickOn(R.string.view_source)
        // wait for library
        sleep(1000)
        pressBack()
        autoClosed = true
    }

    fun editHistory() {
        clickOn(R.string.message_view_edit_history)
        interactWithSheet<ViewEditHistoryBottomSheet>(withText(R.string.message_edits), openState = BottomSheetBehavior.STATE_COLLAPSED) {
            pressBack()
        }
        autoClosed = true
    }

    fun addQuickReaction(quickReaction: String) {
        clickOn(quickReaction)
        autoClosed = true
    }

    fun addReactionFromEmojiPicker() {
        clickOn(R.string.message_add_reaction)
        // Wait for emoji to load, it's async now
        waitUntilActivityVisible<EmojiReactionPickerActivity> {
            waitUntilViewVisible(withId(R.id.emojiRecyclerView))
            waitUntilViewVisible(withText("😀"))
        }
        clickListItem(R.id.emojiRecyclerView, 4)
        autoClosed = true
    }

    fun edit() {
        clickOn(R.string.edit)
        autoClosed = true
    }

    fun replyInThread() {
        clickOn(R.string.reply_in_thread)
        autoClosed = true
    }
    fun viewInRoom() {
        clickOn(R.string.view_in_room)
        autoClosed = true
    }
}
