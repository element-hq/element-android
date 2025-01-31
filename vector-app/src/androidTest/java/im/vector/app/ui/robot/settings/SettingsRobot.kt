/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.ui.robot.settings

import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import im.vector.app.R
import im.vector.app.clickOnAndGoBack

class SettingsRobot {

    fun toggleDeveloperMode() {
        advancedSettings {
            toggleDeveloperMode()
        }
    }

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

    fun securityAndPrivacy(block: SettingsSecurityRobot.() -> Unit) {
        clickOnAndGoBack(R.string.settings_security_and_privacy) { block(SettingsSecurityRobot()) }
    }

    fun labs(shouldGoBack: Boolean = true, block: () -> Unit = {}) {
        if (shouldGoBack) {
            clickOnAndGoBack(R.string.room_settings_labs_pref_title) { block() }
        } else {
            clickOn(R.string.room_settings_labs_pref_title)
            block()
        }
    }

    fun advancedSettings(block: SettingsAdvancedRobot.() -> Unit) {
        clickOnAndGoBack(R.string.settings_advanced_settings) {
            block(SettingsAdvancedRobot())
        }
    }

    fun helpAndAbout(block: SettingsHelpRobot.() -> Unit) {
        clickOnAndGoBack(R.string.preference_root_help_about) { block(SettingsHelpRobot()) }
    }

    fun legals(block: SettingsLegalsRobot.() -> Unit) {
        clickOnAndGoBack(R.string.preference_root_legals) { block(SettingsLegalsRobot()) }
    }
}
