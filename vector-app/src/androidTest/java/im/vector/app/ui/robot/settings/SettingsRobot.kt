/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
