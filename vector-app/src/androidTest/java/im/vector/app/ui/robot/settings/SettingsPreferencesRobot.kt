/*
 * Copyright 2021-2024 New Vector Ltd.
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
import im.vector.lib.strings.CommonStrings

class SettingsPreferencesRobot {

    fun crawl() {
        clickOn(CommonStrings.settings_interface_language)
        waitUntilViewVisible(withText("Dansk (Danmark)"))
        pressBack()
        clickOn(CommonStrings.settings_theme)
        clickDialogNegativeButton()
        clickOn(CommonStrings.font_size)
        waitUntilViewVisible(withId(R.id.fons_scale_recycler))
        pressBack()
    }
}
