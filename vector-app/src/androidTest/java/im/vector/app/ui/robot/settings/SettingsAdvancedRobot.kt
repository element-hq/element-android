/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.ui.robot.settings

import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import im.vector.app.espresso.tools.clickOnPreference
import im.vector.app.espresso.tools.waitUntilViewVisible
import im.vector.lib.strings.CommonStrings

class SettingsAdvancedRobot {

    fun crawl() {
        clickOnPreference(CommonStrings.settings_notifications_targets)
        pressBack()

        clickOnPreference(CommonStrings.settings_push_rules)
        pressBack()
    }

    fun toggleDeveloperMode() {
        clickOn(CommonStrings.settings_developer_mode_summary)
    }

    fun crawlDeveloperOptions() {
        clickOnPreference(CommonStrings.settings_account_data)
        waitUntilViewVisible(withText("m.push_rules"))
        clickOn("m.push_rules")
        pressBack()
        pressBack()
        clickOnPreference(CommonStrings.settings_key_requests)
        pressBack()
    }
}
