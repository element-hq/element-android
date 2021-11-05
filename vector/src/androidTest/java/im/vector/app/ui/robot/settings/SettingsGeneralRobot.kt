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

package im.vector.app.ui.robot.settings

import androidx.test.espresso.Espresso
import com.adevinta.android.barista.interaction.BaristaClickInteractions
import com.adevinta.android.barista.interaction.BaristaDialogInteractions
import im.vector.app.R
import im.vector.app.espresso.tools.clickOnPreference

class SettingsGeneralRobot {

    fun crawl() {
        BaristaClickInteractions.clickOn(R.string.settings_profile_picture)
        BaristaDialogInteractions.clickDialogPositiveButton()
        BaristaClickInteractions.clickOn(R.string.settings_display_name)
        BaristaDialogInteractions.clickDialogNegativeButton()
        BaristaClickInteractions.clickOn(R.string.settings_password)
        BaristaDialogInteractions.clickDialogNegativeButton()
        BaristaClickInteractions.clickOn(R.string.settings_emails_and_phone_numbers_title)
        Espresso.pressBack()
        BaristaClickInteractions.clickOn(R.string.settings_discovery_manage)
        BaristaClickInteractions.clickOn(R.string.add_identity_server)
        Espresso.pressBack()
        Espresso.pressBack()
        // Homeserver
        clickOnPreference(R.string.settings_home_server)
        Espresso.pressBack()
        // Identity server
        clickOnPreference(R.string.settings_identity_server)
        Espresso.pressBack()
        // Deactivate account
        clickOnPreference(R.string.settings_deactivate_my_account)
        Espresso.pressBack()
    }
}
