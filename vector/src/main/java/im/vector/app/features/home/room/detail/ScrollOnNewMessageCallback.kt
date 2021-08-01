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

package im.vector.app.features.home.room.detail

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import im.vector.app.core.platform.DefaultListUpdateCallback
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.item.ItemWithEvents
import org.matrix.android.sdk.api.extensions.tryOrNull
import java.util.concurrent.CopyOnWriteArrayList

class ScrollOnNewMessageCallback(private val layoutManager: LinearLayoutManager,
                                 private val timelineEventController: TimelineEventController,
                                 private val parentView: View) : DefaultListUpdateCallback {

    private val newTimelineEventIds = CopyOnWriteArrayList<String>()
    private var forceScroll = false
    var initialForceScroll = false
    var initialForceScrollEventId: String? = null
        get() = field ?: timelineEventController.timeline?.getInitialEventId()

    fun addNewTimelineEventIds(eventIds: List<String>) {
        // Disable initial force scroll
        initialForceScroll = false
        // Update force scroll id when sticking to the bottom - TODO try this if staying at bottom is not reliable as well
        /*
        if (eventIds.isNotEmpty()) {
            initialForceScrollEventId.let {
                if (it != null && it == timelineEventController.timeline?.getInitialEventId()) {
                    initialForceScrollEventId = eventIds[0]
                }
            }
        }
         */
        newTimelineEventIds.addAll(0, eventIds)
    }

    fun forceScrollOnNextUpdate() {
        forceScroll = true
    }

    override fun onInserted(position: Int, count: Int) {
        if (initialForceScroll) {
            var scrollToEvent = initialForceScrollEventId
            if (initialForceScrollEventId == null) {
                scrollToEvent = timelineEventController.timeline?.getInitialEventId()
            }
            if (scrollToEvent == null) {
                layoutManager.scrollToPositionWithOffset(0, 0)
            } else {
                timelineEventController.searchPositionOfEvent(scrollToEvent)?.let {
                    // Scroll such that the scrolled-to event is moved up 1/4 of the screen
                    layoutManager.scrollToPositionWithOffset(it, parentView.measuredHeight / 4)
                }
            }
            return
        }
        if (position != 0) {
            return
        }
        if (forceScroll) {
            forceScroll = false
            layoutManager.scrollToPositionWithOffset(0, 0)
            return
        }
        if (layoutManager.findFirstVisibleItemPosition() > 1) {
            return
        }
        val firstNewItem = tryOrNull {
            timelineEventController.adapter.getModelAtPosition(position)
        } as? ItemWithEvents ?: return
        val firstNewItemIds = firstNewItem.getEventIds().firstOrNull() ?: return
        val indexOfFirstNewItem = newTimelineEventIds.indexOf(firstNewItemIds)
        if (indexOfFirstNewItem != -1) {
            while (newTimelineEventIds.lastOrNull() != firstNewItemIds) {
                newTimelineEventIds.removeLastOrNull()
            }
            layoutManager.scrollToPositionWithOffset(0, 0)
        }
    }
}
