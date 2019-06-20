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

package im.vector.riotredesign.features.home.room.detail

import androidx.recyclerview.widget.LinearLayoutManager
import im.vector.riotredesign.core.platform.DefaultListUpdateCallback
import im.vector.riotredesign.features.home.room.detail.timeline.TimelineEventController
import java.util.concurrent.atomic.AtomicReference

class ScrollOnHighlightedEventCallback(private val layoutManager: LinearLayoutManager,
                                       private val timelineEventController: TimelineEventController) : DefaultListUpdateCallback {

    private val scheduledEventId = AtomicReference<String?>()

    override fun onChanged(position: Int, count: Int, tag: Any?) {
        val eventId = scheduledEventId.get() ?: return

        val positionToScroll = timelineEventController.searchPositionOfEvent(eventId)

        if (positionToScroll != null) {
            val firstVisibleItem = layoutManager.findFirstCompletelyVisibleItemPosition()
            val lastVisibleItem = layoutManager.findLastCompletelyVisibleItemPosition()

            // Do not scroll it item is already visible
            if (positionToScroll !in firstVisibleItem..lastVisibleItem) {
                // Note: Offset will be from the bottom, since the layoutManager is reversed
                layoutManager.scrollToPositionWithOffset(positionToScroll, 120)
            }
            scheduledEventId.set(null)
        }
    }

    fun scheduleScrollTo(eventId: String?) {
        scheduledEventId.set(eventId)
    }
}