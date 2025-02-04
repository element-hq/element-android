/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
