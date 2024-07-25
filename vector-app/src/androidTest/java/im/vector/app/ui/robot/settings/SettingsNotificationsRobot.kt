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
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import im.vector.app.espresso.tools.clickOnPreference
import im.vector.lib.strings.CommonStrings

class SettingsNotificationsRobot {

    fun crawl() {
        clickOn(CommonStrings.settings_notification_default)
        pressBack()
        clickOn(CommonStrings.settings_notification_mentions_and_keywords)
        // TODO Test adding a keyword?
        pressBack()
        clickOn(CommonStrings.settings_notification_other)
        pressBack()

        /*
        clickOn(CommonStrings.settings_noisy_notifications_preferences)
        TODO Cannot go back
        pressBack()
        clickOn(CommonStrings.settings_silent_notifications_preferences)
        pressBack()
        clickOn(CommonStrings.settings_call_notifications_preferences)
        pressBack()
         */
        // Email notification. No Emails are configured so we show go to the screen to add email
        clickOnPreference(CommonStrings.settings_notification_emails_no_emails)
        assertDisplayed(CommonStrings.settings_emails_and_phone_numbers_title)
        pressBack()

        // Display the notification method change dialog
        clickOnPreference(CommonStrings.settings_notification_method)
        pressBack()

        clickOnPreference(CommonStrings.settings_notification_troubleshoot)
        // Give time for the tests to perform
        Thread.sleep(12_000)
        pressBack()
    }
}
