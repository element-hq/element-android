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
import androidx.test.espresso.matcher.ViewMatchers
import com.adevinta.android.barista.interaction.BaristaClickInteractions
import com.adevinta.android.barista.interaction.BaristaDialogInteractions
import im.vector.app.R
import im.vector.app.waitForView

class SettingsPreferencesRobot {

    fun crawl() {
        BaristaClickInteractions.clickOn(R.string.settings_interface_language)
        Espresso.onView(ViewMatchers.isRoot())
                .perform(waitForView(ViewMatchers.withText("Dansk (Danmark)")))
        Espresso.pressBack()
        BaristaClickInteractions.clickOn(R.string.settings_theme)
        BaristaDialogInteractions.clickDialogNegativeButton()
        BaristaClickInteractions.clickOn(R.string.font_size)
        BaristaDialogInteractions.clickDialogNegativeButton()
    }
}
