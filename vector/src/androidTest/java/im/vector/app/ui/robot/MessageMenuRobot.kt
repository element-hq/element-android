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

import androidx.test.espresso.Espresso
import com.adevinta.android.barista.interaction.BaristaClickInteractions
import com.adevinta.android.barista.interaction.BaristaListInteractions
import im.vector.app.R

class MessageMenuRobot(
        var autoClosed: Boolean = false
) {

    fun viewSource() {
        BaristaClickInteractions.clickOn(R.string.view_source)
        // wait for library
        Thread.sleep(1000)
        Espresso.pressBack()
        autoClosed = true
    }

    fun editHistory() {
        BaristaClickInteractions.clickOn(R.string.message_view_edit_history)
        Espresso.pressBack()
        autoClosed = true
    }

    fun addQuickReaction(quickReaction: String) {
        BaristaClickInteractions.clickOn(quickReaction)
        autoClosed = true
    }

    fun addReactionFromEmojiPicker() {
        BaristaClickInteractions.clickOn(R.string.message_add_reaction)
        // Wait for emoji to load, it's async now
        Thread.sleep(2000)
        BaristaListInteractions.clickListItem(R.id.emojiRecyclerView, 4)
        autoClosed = true
    }

    fun edit() {
        BaristaClickInteractions.clickOn(R.string.edit)
        autoClosed = true
    }
}
