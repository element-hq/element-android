/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotx.features.home.room.detail

import androidx.recyclerview.widget.LinearLayoutManager
import im.vector.matrix.android.api.session.room.timeline.Timeline
import im.vector.riotx.core.platform.DefaultListUpdateCallback
import im.vector.riotx.features.home.room.detail.timeline.TimelineEventController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference

/**
 * This handles scrolling to an event which wasn't yet loaded when scheduled.
 */
class ScrollOnHighlightedEventCallback(private val layoutManager: LinearLayoutManager,
                                       private val timelineEventController: TimelineEventController) : DefaultListUpdateCallback {

    private val scheduledEventId = AtomicReference<String?>()

    var timeline: Timeline? = null

    override fun onInserted(position: Int, count: Int) {
        scrollIfNeeded()
    }

    override fun onChanged(position: Int, count: Int, tag: Any?) {
        scrollIfNeeded()
    }

    private fun scrollIfNeeded() {
        val eventId = scheduledEventId.get() ?: return
        val nonNullTimeline = timeline ?: return
        val correctedEventId = nonNullTimeline.getFirstDisplayableEventId(eventId)
        val positionToScroll = timelineEventController.searchPositionOfEvent(correctedEventId)
        if (positionToScroll != null) {
            val firstVisibleItem = layoutManager.findFirstCompletelyVisibleItemPosition()
            val lastVisibleItem = layoutManager.findLastCompletelyVisibleItemPosition()

            // Do not scroll it item is already visible
            if (positionToScroll !in firstVisibleItem..lastVisibleItem) {
                Timber.v("Scroll to $positionToScroll")
                // Note: Offset will be from the bottom, since the layoutManager is reversed
                layoutManager.scrollToPosition(positionToScroll)
            }
            scheduledEventId.set(null)
        }
    }

    fun scheduleScrollTo(eventId: String?) {
        scheduledEventId.set(eventId)
    }
}