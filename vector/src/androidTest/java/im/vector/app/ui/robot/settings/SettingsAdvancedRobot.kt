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
import im.vector.app.R
import im.vector.app.espresso.tools.clickOnPreference

class SettingsAdvancedRobot {

    fun crawl() {
        clickOnPreference(R.string.settings_notifications_targets)
        Espresso.pressBack()

        clickOnPreference(R.string.settings_push_rules)
        Espresso.pressBack()

        /* TODO P2 test developer screens
    // Enable developer mode
    clickOnSwitchPreference("SETTINGS_DEVELOPER_MODE_PREFERENCE_KEY")

    clickOnPreference(R.string.settings_account_data)
    clickOn("m.push_rules")
    pressBack()
    pressBack()
    clickOnPreference(R.string.settings_key_requests)
    pressBack()

    // Disable developer mode
    clickOnSwitchPreference("SETTINGS_DEVELOPER_MODE_PREFERENCE_KEY")
     */
    }
}
