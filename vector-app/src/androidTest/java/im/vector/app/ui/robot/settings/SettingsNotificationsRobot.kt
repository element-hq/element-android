/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.ui.robot.settings

import androidx.test.espresso.Espresso.pressBack
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
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
        // Email notification. No Emails are configured so we show go to the screen to add email
        clickOnPreference(R.string.settings_notification_emails_no_emails)
        assertDisplayed(R.string.settings_emails_and_phone_numbers_title)
        pressBack()

        // Display the notification method change dialog
        clickOnPreference(R.string.settings_notification_method)
        pressBack()

        clickOnPreference(R.string.settings_notification_troubleshoot)
        // Give time for the tests to perform
        Thread.sleep(12_000)
        pressBack()
    }
}
