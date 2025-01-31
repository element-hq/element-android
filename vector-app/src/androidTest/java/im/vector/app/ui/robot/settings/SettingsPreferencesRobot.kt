/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.ui.robot.settings

import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogNegativeButton
import im.vector.app.R
import im.vector.app.espresso.tools.waitUntilViewVisible

class SettingsPreferencesRobot {

    fun crawl() {
        clickOn(R.string.settings_interface_language)
        waitUntilViewVisible(withText("Dansk (Danmark)"))
        pressBack()
        clickOn(R.string.settings_theme)
        clickDialogNegativeButton()
        clickOn(R.string.font_size)
        waitUntilViewVisible(withId(R.id.fons_scale_recycler))
        pressBack()
    }
}
