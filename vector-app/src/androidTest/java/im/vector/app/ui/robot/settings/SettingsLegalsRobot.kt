/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.ui.robot.settings

import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton
import im.vector.lib.strings.CommonStrings

class SettingsLegalsRobot {

    fun crawl() {
        clickOn(CommonStrings.settings_third_party_notices)
        clickDialogPositiveButton()
    }
}
