/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.ui.robot

import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import im.vector.app.R
import im.vector.app.espresso.tools.waitUntilViewVisible
import im.vector.app.features.DefaultVectorFeatures
import im.vector.app.features.VectorFeatures
import im.vector.app.ui.robot.settings.labs.LabFeaturesPreferences

class NewRoomRobot(
        var createdRoom: Boolean = false,
        private val labsPreferences: LabFeaturesPreferences
) {
    private val features: VectorFeatures = DefaultVectorFeatures()

    fun createNewRoom(block: CreateNewRoomRobot.() -> Unit) {
        clickOn(R.string.create_new_room)
        waitUntilViewVisible(withId(R.id.createRoomForm))
        val createNewRoomRobot = CreateNewRoomRobot()
        block(createNewRoomRobot)
        createdRoom = createNewRoomRobot.createdRoom
        if (!createNewRoomRobot.createdRoom) {
            pressBack()
        }
    }
}
