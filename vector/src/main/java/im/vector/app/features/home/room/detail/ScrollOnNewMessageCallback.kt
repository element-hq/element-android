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

import androidx.recyclerview.widget.LinearLayoutManager
import im.vector.app.core.platform.DefaultListUpdateCallback
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import java.util.concurrent.CopyOnWriteArrayList

class ScrollOnNewMessageCallback(private val layoutManager: LinearLayoutManager,
                                 private val timelineEventController: TimelineEventController) : DefaultListUpdateCallback {

    private val newTimelineEventIds = CopyOnWriteArrayList<String>()
    private var forceScroll = false

    fun addNewTimelineEventIds(eventIds: List<String>) {
        newTimelineEventIds.addAll(0, eventIds)
    }

    fun forceScrollOnNextUpdate() {
        forceScroll = true
    }

    override fun onInserted(position: Int, count: Int) {
        if (forceScroll) {
            forceScroll = false
            layoutManager.scrollToPosition(position)
            return
        }
        if (layoutManager.findFirstVisibleItemPosition() > 1) {
            return
        }
        val newEventsInRange = newTimelineEventIds.filter {
            val index = timelineEventController.searchPositionOfEvent(it) ?: return@filter false
            index >= position && index <= position + count
        }
        if (newEventsInRange.isNotEmpty()) {
            newTimelineEventIds.removeAll(newEventsInRange)
            layoutManager.scrollToPosition(0)
        }
    }
}
