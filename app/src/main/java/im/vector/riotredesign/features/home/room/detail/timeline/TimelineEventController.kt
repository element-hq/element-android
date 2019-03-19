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

package im.vector.riotredesign.features.home.room.detail.timeline

import android.os.Handler
import android.view.View
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.VisibilityState
import im.vector.matrix.android.api.session.room.timeline.Timeline
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.riotredesign.core.epoxy.LoadingItemModel_
import im.vector.riotredesign.core.epoxy.RiotEpoxyModel
import im.vector.riotredesign.core.extensions.localDateTime
import im.vector.riotredesign.features.home.room.detail.timeline.factory.TimelineItemFactory
import im.vector.riotredesign.features.home.room.detail.timeline.helper.TimelineAsyncHelper
import im.vector.riotredesign.features.home.room.detail.timeline.helper.TimelineDateFormatter
import im.vector.riotredesign.features.home.room.detail.timeline.helper.TimelineEventDiffUtilCallback
import im.vector.riotredesign.features.home.room.detail.timeline.helper.TimelineMediaSizeProvider
import im.vector.riotredesign.features.home.room.detail.timeline.item.DaySeparatorItem_
import im.vector.riotredesign.features.media.MediaContentRenderer

class TimelineEventController(private val dateFormatter: TimelineDateFormatter,
                              private val timelineItemFactory: TimelineItemFactory,
                              private val timelineMediaSizeProvider: TimelineMediaSizeProvider,
                              private val backgroundHandler: Handler = TimelineAsyncHelper.getBackgroundHandler()
) : EpoxyController(backgroundHandler, backgroundHandler), Timeline.Listener {

    private val modelCache = arrayListOf<List<EpoxyModel<*>>>()
    private var currentSnapshot: List<TimelineEvent> = emptyList()

    private val listUpdateCallback = object : ListUpdateCallback {

        @Synchronized
        override fun onChanged(position: Int, count: Int, payload: Any?) {
            (position until (position + count)).forEach {
                modelCache[it] = emptyList()
            }
            requestModelBuild()
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            //no-op
        }

        @Synchronized
        override fun onInserted(position: Int, count: Int) = synchronized(modelCache) {
            if (modelCache.isNotEmpty() && position == modelCache.size) {
                modelCache[position - 1] = emptyList()
            }
            (0 until count).forEach {
                modelCache.add(position, emptyList())
            }
            requestModelBuild()
        }

        override fun onRemoved(position: Int, count: Int) {
            //no-op
        }

    }

    private var timeline: Timeline? = null
    var callback: Callback? = null

    fun setTimeline(timeline: Timeline?) {
        if (this.timeline != timeline) {
            this.timeline = timeline
            this.timeline?.listener = this
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        timelineMediaSizeProvider.recyclerView = recyclerView
    }

    override fun buildModels() {
        LoadingItemModel_()
                .id("forward_loading_item")
                .addWhen(Timeline.Direction.FORWARDS)

        add(getModels())

        LoadingItemModel_()
                .id("backward_loading_item")
                .addWhen(Timeline.Direction.BACKWARDS)
    }

    private fun LoadingItemModel_.addWhen(direction: Timeline.Direction) {
        val shouldAdd = timeline?.let {
            it.hasMoreToLoad(direction) || !it.hasReachedEnd(direction)
        } ?: false
        addIf(shouldAdd, this@TimelineEventController)
    }

    @Synchronized
    private fun getModels(): List<EpoxyModel<*>> {
        (0 until modelCache.size).forEach { position ->
            if (modelCache[position].isEmpty()) {
                modelCache[position] = buildItemModels(position, currentSnapshot)
            }
        }
        return modelCache.flatten()
    }

    private fun buildItemModels(currentPosition: Int, items: List<TimelineEvent>): List<EpoxyModel<*>> {
        val epoxyModels = ArrayList<EpoxyModel<*>>()
        val event = items[currentPosition]
        val nextEvent = if (currentPosition + 1 < items.size) items[currentPosition + 1] else null

        val date = event.root.localDateTime()
        val nextDate = nextEvent?.root?.localDateTime()
        val addDaySeparator = date.toLocalDate() != nextDate?.toLocalDate()

        timelineItemFactory.create(event, nextEvent, callback).also {
            it.id(event.localId)
            it.setOnVisibilityStateChanged(TimelineEventVisibilityStateChangedListener(callback, event))
            epoxyModels.add(it)
        }
        if (addDaySeparator) {
            val formattedDay = dateFormatter.formatMessageDay(date)
            val daySeparatorItem = DaySeparatorItem_().formattedDay(formattedDay).id(formattedDay)
            epoxyModels.add(daySeparatorItem)
        }
        return epoxyModels
    }

    // Timeline.LISTENER ***************************************************************************

    override fun onUpdated(snapshot: List<TimelineEvent>) {
        submitSnapshot(snapshot)
    }

    private fun submitSnapshot(newSnapshot: List<TimelineEvent>) {
        backgroundHandler.post {
            val diffCallback = TimelineEventDiffUtilCallback(currentSnapshot, newSnapshot)
            currentSnapshot = newSnapshot
            val diffResult = DiffUtil.calculateDiff(diffCallback)
            diffResult.dispatchUpdatesTo(listUpdateCallback)
        }
    }


    interface Callback {
        fun onEventVisible(event: TimelineEvent)
        fun onUrlClicked(url: String)
        fun onMediaClicked(mediaData: MediaContentRenderer.Data, view: View)
    }

}

private class TimelineEventVisibilityStateChangedListener(private val callback: TimelineEventController.Callback?,
                                                          private val event: TimelineEvent)
    : RiotEpoxyModel.OnVisibilityStateChangedListener {

    override fun onVisibilityStateChanged(visibilityState: Int) {
        if (visibilityState == VisibilityState.VISIBLE) {
            callback?.onEventVisible(event)
        }
    }


}