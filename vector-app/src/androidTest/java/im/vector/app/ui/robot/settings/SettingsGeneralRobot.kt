/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.ui.robot.settings

import androidx.test.espresso.Espresso.pressBack
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogNegativeButton
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton
import im.vector.app.espresso.tools.clickOnPreference
import im.vector.lib.strings.CommonStrings

class SettingsGeneralRobot {

    fun crawl() {
        clickOn(CommonStrings.settings_profile_picture)
        clickDialogPositiveButton()
        clickOn(CommonStrings.settings_display_name)
        clickDialogNegativeButton()
        clickOn(CommonStrings.settings_password)
        clickDialogNegativeButton()
        clickOn(CommonStrings.settings_emails_and_phone_numbers_title)
        pressBack()
        clickOn(CommonStrings.settings_discovery_manage)
        clickOn(CommonStrings.add_identity_server)
        pressBack()
        pressBack()
        // Homeserver
        clickOnPreference(CommonStrings.settings_home_server)
        pressBack()
        // Identity server
        clickOnPreference(CommonStrings.settings_identity_server)
        pressBack()
        // Deactivate account
        clickOnPreference(CommonStrings.settings_deactivate_my_account)
        pressBack()
    }
}
