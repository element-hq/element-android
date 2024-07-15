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

import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import im.vector.app.clickOnAndGoBack
import im.vector.lib.strings.CommonStrings

class SettingsRobot {

    fun toggleDeveloperMode() {
        advancedSettings {
            toggleDeveloperMode()
        }
    }

    fun general(block: SettingsGeneralRobot.() -> Unit) {
        clickOnAndGoBack(CommonStrings.settings_general_title) { block(SettingsGeneralRobot()) }
    }

    fun notifications(block: SettingsNotificationsRobot.() -> Unit) {
        clickOnAndGoBack(CommonStrings.settings_notifications) { block(SettingsNotificationsRobot()) }
    }

    fun preferences(block: SettingsPreferencesRobot.() -> Unit) {
        clickOnAndGoBack(CommonStrings.settings_preferences) { block(SettingsPreferencesRobot()) }
    }

    fun voiceAndVideo(block: () -> Unit = {}) {
        clickOnAndGoBack(CommonStrings.preference_voice_and_video) { block() }
    }

    fun securityAndPrivacy(block: SettingsSecurityRobot.() -> Unit) {
        clickOnAndGoBack(CommonStrings.settings_security_and_privacy) { block(SettingsSecurityRobot()) }
    }

    fun labs(shouldGoBack: Boolean = true, block: () -> Unit = {}) {
        if (shouldGoBack) {
            clickOnAndGoBack(CommonStrings.room_settings_labs_pref_title) { block() }
        } else {
            clickOn(CommonStrings.room_settings_labs_pref_title)
            block()
        }
    }

    fun advancedSettings(block: SettingsAdvancedRobot.() -> Unit) {
        clickOnAndGoBack(CommonStrings.settings_advanced_settings) {
            block(SettingsAdvancedRobot())
        }
    }

    fun helpAndAbout(block: SettingsHelpRobot.() -> Unit) {
        clickOnAndGoBack(CommonStrings.preference_root_help_about) { block(SettingsHelpRobot()) }
    }

    fun legals(block: SettingsLegalsRobot.() -> Unit) {
        clickOnAndGoBack(CommonStrings.preference_root_legals) { block(SettingsLegalsRobot()) }
    }
}
