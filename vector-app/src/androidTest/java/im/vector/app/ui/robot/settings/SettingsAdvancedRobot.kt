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
