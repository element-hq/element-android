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
