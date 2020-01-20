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
import im.vector.riotx.core.platform.DefaultListUpdateCallback
import im.vector.riotx.features.home.room.detail.timeline.TimelineEventController
import im.vector.riotx.features.home.room.detail.timeline.item.BaseEventItem
import timber.log.Timber

class ScrollOnNewMessageCallback(private val layoutManager: LinearLayoutManager,
                                 private val timelineEventController: TimelineEventController) : DefaultListUpdateCallback {

    private val newTimelineEventIds = HashSet<String>()

    fun addNewTimelineEventIds(eventIds: List<String>){
        newTimelineEventIds.addAll(eventIds)
    }

    override fun onInserted(position: Int, count: Int) {
        Timber.v("On inserted $count count at position: $position")
        if(layoutManager.findFirstVisibleItemPosition() != position ){
            return
        }
        val firstNewItem = timelineEventController.adapter.getModelAtPosition(position) as? BaseEventItem ?: return
        val firstNewItemIds = firstNewItem.getEventIds()
        if(newTimelineEventIds.intersect(firstNewItemIds).isNotEmpty()){
            Timber.v("Should scroll to position: $position")
            newTimelineEventIds.clear()
            layoutManager.scrollToPosition(position)
        }
    }
}
