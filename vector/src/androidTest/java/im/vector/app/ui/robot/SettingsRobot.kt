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

package im.vector.app.ui.robot

import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.matcher.ViewMatchers
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogNegativeButton
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton
import im.vector.app.BuildConfig
import im.vector.app.R
import im.vector.app.espresso.tools.clickOnPreference
import im.vector.app.waitForView

class SettingsRobot {

    fun general(block: SettingsGeneralRobot.() -> Unit) {
        clickOnAndGoBack(R.string.settings_general_title) { block(SettingsGeneralRobot()) }
    }

    fun notifications(block: SettingsNotificationsRobot.() -> Unit) {
        clickOnAndGoBack(R.string.settings_notifications) { block(SettingsNotificationsRobot()) }
    }

    fun preferences(block: SettingsPreferencesRobot.() -> Unit) {
        clickOnAndGoBack(R.string.settings_preferences) { block(SettingsPreferencesRobot()) }
    }

    fun voiceAndVideo(block: () -> Unit = {}) {
        clickOnAndGoBack(R.string.preference_voice_and_video) { block() }
    }

    fun ignoredUsers(block: () -> Unit = {}) {
        clickOnAndGoBack(R.string.settings_ignored_users) { block() }
    }

    fun securityAndPrivacy(block: SettingsSecurityRobot.() -> Unit) {
        clickOnAndGoBack(R.string.settings_security_and_privacy) { block(SettingsSecurityRobot()) }
    }

    fun labs(block: () -> Unit = {}) {
        clickOnAndGoBack(R.string.room_settings_labs_pref_title) { block() }
    }

    fun advancedSettings(block: SettingsAdvancedRobot.() -> Unit) {
        clickOnAndGoBack(R.string.settings_advanced_settings) { block(SettingsAdvancedRobot()) }
    }

    fun helpAndAbout(block: SettingsHelpRobot.() -> Unit) {
        clickOnAndGoBack(R.string.preference_root_help_about) { block(SettingsHelpRobot()) }
    }
}

class SettingsGeneralRobot {

    fun crawl() {
        clickOn(R.string.settings_profile_picture)
        clickDialogPositiveButton()
        clickOn(R.string.settings_display_name)
        clickDialogNegativeButton()
        clickOn(R.string.settings_password)
        clickDialogNegativeButton()
        clickOn(R.string.settings_emails_and_phone_numbers_title)
        pressBack()
        clickOn(R.string.settings_discovery_manage)
        clickOn(R.string.add_identity_server)
        pressBack()
        pressBack()
        // Homeserver
        clickOnPreference(R.string.settings_home_server)
        pressBack()
        // Identity server
        clickOnPreference(R.string.settings_identity_server)
        pressBack()
        // Deactivate account
        clickOnPreference(R.string.settings_deactivate_my_account)
        pressBack()
    }
}

class SettingsNotificationsRobot {

    fun crawl() {
        if (BuildConfig.USE_NOTIFICATION_SETTINGS_V2) {
            clickOn(R.string.settings_notification_default)
            pressBack()
            clickOn(R.string.settings_notification_mentions_and_keywords)
            // TODO Test adding a keyword?
            pressBack()
            clickOn(R.string.settings_notification_other)
            pressBack()
        } else {
            clickOn(R.string.settings_notification_advanced)
            pressBack()
        }
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

class SettingsHelpRobot {

    fun crawl() {
        /*
        clickOn(R.string.settings_app_info_link_title)
        Cannot go back...
        pressBack()
        clickOn(R.string.settings_copyright)
        pressBack()
        clickOn(R.string.settings_app_term_conditions)
        pressBack()
        clickOn(R.string.settings_privacy_policy)
        pressBack()
         */
        clickOn(R.string.settings_third_party_notices)
        clickDialogPositiveButton()
    }
}

class SettingsAdvancedRobot {

    fun crawl() {
        clickOnPreference(R.string.settings_notifications_targets)
        pressBack()

        clickOnPreference(R.string.settings_push_rules)
        pressBack()

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

class SettingsSecurityRobot {

    fun crawl() {
        clickOnPreference(R.string.settings_active_sessions_show_all)
        pressBack()

        clickOnPreference(R.string.encryption_message_recovery)
        // TODO go deeper here
        pressBack()
        /* Cannot exit
        clickOnPreference(R.string.encryption_export_e2e_room_keys)
        pressBack()
         */
    }
}

class SettingsPreferencesRobot {

    fun crawl() {
        clickOn(R.string.settings_interface_language)
        onView(ViewMatchers.isRoot())
                .perform(waitForView(ViewMatchers.withText("Dansk (Danmark)")))
        pressBack()
        clickOn(R.string.settings_theme)
        clickDialogNegativeButton()
        clickOn(R.string.font_size)
        clickDialogNegativeButton()
    }
}

fun clickOnAndGoBack(@StringRes name: Int, block: () -> Unit) {
    clickOn(name)
    block()
    pressBack()
}
