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
    }
}
