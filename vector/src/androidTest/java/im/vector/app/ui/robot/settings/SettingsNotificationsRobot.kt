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
import im.vector.app.R
import im.vector.app.espresso.tools.clickOnPreference

class SettingsNotificationsRobot {

    fun crawl() {
        clickOn(R.string.settings_notification_default)
        pressBack()
        clickOn(R.string.settings_notification_mentions_and_keywords)
        // TODO Test adding a keyword?
        pressBack()
        clickOn(R.string.settings_notification_other)
        pressBack()

        /*
        clickOn(R.string.settings_noisy_notifications_preferences)
        TODO Cannot go back
        pressBack()
        clickOn(R.string.settings_silent_notifications_preferences)
        pressBack()
        clickOn(R.string.settings_call_notifications_preferences)
        pressBack()
         */
        clickOnPreference(R.string.settings_notification_troubleshoot)
        pressBack()
    }
}
