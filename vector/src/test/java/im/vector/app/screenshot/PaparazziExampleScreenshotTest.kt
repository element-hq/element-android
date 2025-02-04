/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.screenshot

import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import im.vector.app.R
import im.vector.lib.strings.CommonStrings
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@Ignore("CI failing with NPE on paparazzi.inflate")
class PaparazziExampleScreenshotTest {

    @get:Rule
    val paparazzi = createPaparazziRule()

    @Test
    fun `example paparazzi test`() {
        val view = paparazzi.inflate<ConstraintLayout>(R.layout.item_radio)

        view.findViewById<TextView>(R.id.actionTitle).text = paparazzi.resources.getString(CommonStrings.room_settings_all_messages)
        view.findViewById<ImageView>(R.id.radioIcon).setImageResource(R.drawable.ic_radio_on)

        paparazzi.snapshot(view)
    }
}
