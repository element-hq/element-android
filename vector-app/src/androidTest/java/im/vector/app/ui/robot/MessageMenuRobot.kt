/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.ui.robot

import androidx.test.espresso.Espresso.closeSoftKeyboard
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
import im.vector.lib.strings.CommonStrings
import java.lang.Thread.sleep

class MessageMenuRobot(
        var autoClosed: Boolean = false
) {

    fun viewSource() {
        clickOn(CommonStrings.view_source)
        // wait for library
        sleep(1000)
        pressBack()
        autoClosed = true
    }

    fun editHistory() {
        clickOn(CommonStrings.message_view_edit_history)
        interactWithSheet<ViewEditHistoryBottomSheet>(withText(CommonStrings.message_edits), openState = BottomSheetBehavior.STATE_COLLAPSED) {
            pressBack()
        }
        autoClosed = true
    }

    fun addQuickReaction(quickReaction: String) {
        clickOn(quickReaction)
        autoClosed = true
    }

    fun addReactionFromEmojiPicker() {
        clickOn(CommonStrings.message_add_reaction)
        // Wait for emoji to load, it's async now
        waitUntilActivityVisible<EmojiReactionPickerActivity> {
            closeSoftKeyboard()
            waitUntilViewVisible(withId(R.id.emojiRecyclerView))
            waitUntilViewVisible(withText("😀"))
        }
        clickListItem(R.id.emojiRecyclerView, 4)
        autoClosed = true
    }

    fun edit() {
        clickOn(CommonStrings.edit)
        autoClosed = true
    }

    fun replyInThread() {
        clickOn(CommonStrings.reply_in_thread)
        autoClosed = true
    }

    fun viewInRoom() {
        clickOn(CommonStrings.view_in_room)
        autoClosed = true
    }
}
