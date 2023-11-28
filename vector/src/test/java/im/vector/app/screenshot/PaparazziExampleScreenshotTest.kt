/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.screenshot

import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import im.vector.app.R
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

        view.findViewById<TextView>(R.id.actionTitle).text = paparazzi.resources.getString(R.string.room_settings_all_messages)
        view.findViewById<ImageView>(R.id.radioIcon).setImageResource(R.drawable.ic_radio_on)

        paparazzi.snapshot(view)
    }
}
