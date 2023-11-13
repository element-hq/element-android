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

import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import im.vector.app.R
import im.vector.app.features.home.room.list.UnreadCounterBadgeView
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@Ignore("CI failing with NPE on paparazzi.inflate")
class RoomItemScreenshotTest {

    @get:Rule
    val paparazzi = createPaparazziRule()

    @Test
    fun `item room test`() {
        val view = paparazzi.inflate<ConstraintLayout>(R.layout.item_room)

        view.findViewById<View>(R.id.roomUnreadIndicator).isVisible = true
        view.findViewById<TextView>(R.id.roomNameView).text = "Room name"
        view.findViewById<TextView>(R.id.roomLastEventTimeView).text = "12:34"
        view.findViewById<TextView>(R.id.subtitleView).text = "Latest message"
        view.findViewById<View>(R.id.roomDraftBadge).isVisible = true
        view.findViewById<UnreadCounterBadgeView>(R.id.roomUnreadCounterBadgeView).let {
            it.isVisible = true
            it.render(UnreadCounterBadgeView.State.Count(8, false))
        }

        paparazzi.snapshot(view)
    }

    @Test
    fun `item room two line and highlight test`() {
        val view = paparazzi.inflate<ConstraintLayout>(R.layout.item_room)

        view.findViewById<View>(R.id.roomUnreadIndicator).isVisible = true
        view.findViewById<TextView>(R.id.roomNameView).text = "Room name"
        view.findViewById<TextView>(R.id.roomLastEventTimeView).text = "23:59"
        view.findViewById<TextView>(R.id.subtitleView).text = "Latest message\nOn two lines"
        view.findViewById<View>(R.id.roomDraftBadge).isVisible = true
        view.findViewById<UnreadCounterBadgeView>(R.id.roomUnreadCounterBadgeView).let {
            it.isVisible = true
            it.render(UnreadCounterBadgeView.State.Count(88, true))
        }

        paparazzi.snapshot(view)
    }
}
