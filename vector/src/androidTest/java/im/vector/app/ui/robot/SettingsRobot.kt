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

import androidx.test.espresso.Espresso
import com.adevinta.android.barista.interaction.BaristaClickInteractions
import im.vector.app.R

class SettingsRobot {

    fun openGeneral(block: () -> Unit) {
        BaristaClickInteractions.clickOn(R.string.settings_general_title)
        block()
        Espresso.pressBack()
    }

    fun openNotifications(block: () -> Unit) {
        BaristaClickInteractions.clickOn(R.string.settings_notifications)
        block()
        Espresso.pressBack()
    }

    fun openPreferences(block: () -> Unit) {
        BaristaClickInteractions.clickOn(R.string.settings_preferences)
        block()
        Espresso.pressBack()
    }

    fun openVoiceAndVideo(block: () -> Unit) {
        BaristaClickInteractions.clickOn(R.string.preference_voice_and_video)
        block()
        Espresso.pressBack()
    }

    fun openIgnoredUsers(block: () -> Unit) {
        BaristaClickInteractions.clickOn(R.string.settings_ignored_users)
        block()
        Espresso.pressBack()
    }

    fun openSecurityAndPrivacy(block: () -> Unit) {
        BaristaClickInteractions.clickOn(R.string.settings_security_and_privacy)
        block()
        Espresso.pressBack()
    }

    fun openLabs(block: () -> Unit) {
        BaristaClickInteractions.clickOn(R.string.room_settings_labs_pref_title)
        block()
        Espresso.pressBack()
    }

    fun openAdvancedSettings(block: () -> Unit) {
        BaristaClickInteractions.clickOn(R.string.settings_advanced_settings)
        block()
        Espresso.pressBack()
    }

    fun openHelpAbout(block: () -> Unit) {
        BaristaClickInteractions.clickOn(R.string.preference_root_help_about)
        block()
        Espresso.pressBack()
    }
}
