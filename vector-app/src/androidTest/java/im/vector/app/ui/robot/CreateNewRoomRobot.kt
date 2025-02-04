/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.ui.robot

import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.adevinta.android.barista.assertion.BaristaListAssertions
import com.adevinta.android.barista.interaction.BaristaClickInteractions
import com.adevinta.android.barista.interaction.BaristaListInteractions
import im.vector.app.R
import im.vector.app.espresso.tools.waitUntilActivityVisible
import im.vector.app.espresso.tools.waitUntilViewVisible
import im.vector.app.features.home.room.detail.RoomDetailActivity
import im.vector.lib.strings.CommonStrings
import org.hamcrest.CoreMatchers.allOf

class CreateNewRoomRobot(
        var createdRoom: Boolean = false
) {

    fun createRoom(roomName: String? = null, block: RoomDetailRobot.() -> Unit) {
        createdRoom = true
        BaristaListAssertions.assertListItemCount(R.id.createRoomForm, 12)
        roomName?.let {
            onView(allOf(withId(R.id.formTextInputTextInputEditText), withHint(CommonStrings.create_room_name_hint)))
                    .perform(replaceText(roomName))
            closeSoftKeyboard()
        }
        BaristaListInteractions.clickListItemChild(R.id.createRoomForm, 11, R.id.form_submit_button)
        waitUntilActivityVisible<RoomDetailActivity> {
            waitUntilViewVisible(withId(R.id.composerEditText))
        }
        block(RoomDetailRobot())
        pressBack()
    }

    fun crawl() {
        // Room access bottom sheet
        BaristaClickInteractions.clickOn(CommonStrings.room_settings_room_access_private_title)
        pressBack()
    }
}
