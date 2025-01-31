/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.ui.robot.settings

import androidx.test.espresso.Espresso
import im.vector.app.R
import im.vector.app.espresso.tools.clickOnPreference

class SettingsSecurityRobot {

    fun crawl() {
        clickOnPreference(R.string.settings_active_sessions_show_all)
        Espresso.pressBack()

        clickOnPreference(R.string.encryption_message_recovery)
        // TODO go deeper here
        Espresso.pressBack()
        /* Cannot exit
        clickOnPreference(R.string.encryption_export_e2e_room_keys)
        pressBack()
         */

        clickOnPreference(R.string.settings_opt_in_of_analytics)
        Espresso.pressBack()

        clickOnPreference(R.string.settings_ignored_users)
        Espresso.pressBack()
    }
}
