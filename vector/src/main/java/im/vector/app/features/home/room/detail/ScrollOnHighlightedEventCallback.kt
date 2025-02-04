/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import im.vector.app.core.platform.DefaultListUpdateCallback
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import java.util.concurrent.atomic.AtomicReference

/**
 * This handles scrolling to an event which wasn't yet loaded when scheduled.
 */
class ScrollOnHighlightedEventCallback(
        private val recyclerView: RecyclerView,
        private val layoutManager: LinearLayoutManager,
        private val timelineEventController: TimelineEventController
) : DefaultListUpdateCallback {

    private val scheduledEventId = AtomicReference<String?>()

    override fun onInserted(position: Int, count: Int) {
        scrollIfNeeded()
    }

    override fun onChanged(position: Int, count: Int, tag: Any?) {
        scrollIfNeeded()
    }

    private fun scrollIfNeeded() {
        val eventId = scheduledEventId.get() ?: return
        val positionToScroll = timelineEventController.searchPositionOfEvent(eventId) ?: return
        recyclerView.stopScroll()
        layoutManager.scrollToPosition(positionToScroll)
        scheduledEventId.set(null)
    }

    fun scheduleScrollTo(eventId: String?) {
        scheduledEventId.set(eventId)
    }
}
