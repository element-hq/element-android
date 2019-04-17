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
import android.os.Looper
import android.view.View
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.EpoxyModel
import im.vector.matrix.android.api.session.room.model.message.MessageAudioContent
import im.vector.matrix.android.api.session.room.model.message.MessageFileContent
import im.vector.matrix.android.api.session.room.model.message.MessageImageContent
import im.vector.matrix.android.api.session.room.model.message.MessageVideoContent
import im.vector.matrix.android.api.session.room.timeline.Timeline
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.riotredesign.core.epoxy.LoadingItemModel_
import im.vector.riotredesign.core.extensions.localDateTime
import im.vector.riotredesign.features.home.room.detail.timeline.factory.TimelineItemFactory
import im.vector.riotredesign.features.home.room.detail.timeline.helper.TimelineAsyncHelper
import im.vector.riotredesign.features.home.room.detail.timeline.helper.TimelineDateFormatter
import im.vector.riotredesign.features.home.room.detail.timeline.helper.TimelineEventDiffUtilCallback
import im.vector.riotredesign.features.home.room.detail.timeline.helper.TimelineEventVisibilityStateChangedListener
import im.vector.riotredesign.features.home.room.detail.timeline.helper.TimelineMediaSizeProvider
import im.vector.riotredesign.features.home.room.detail.timeline.helper.canBeMerged
import im.vector.riotredesign.features.home.room.detail.timeline.helper.nextDisplayableEvent
import im.vector.riotredesign.features.home.room.detail.timeline.helper.nextSameTypeEvents
import im.vector.riotredesign.features.home.room.detail.timeline.item.DaySeparatorItem_
import im.vector.riotredesign.features.home.room.detail.timeline.item.RoomMemberMergedItem
import im.vector.riotredesign.features.media.ImageContentRenderer
import im.vector.riotredesign.features.media.VideoContentRenderer

class TimelineEventController(private val dateFormatter: TimelineDateFormatter,
                              private val timelineItemFactory: TimelineItemFactory,
                              private val timelineMediaSizeProvider: TimelineMediaSizeProvider,
                              private val backgroundHandler: Handler = TimelineAsyncHelper.getBackgroundHandler()
) : EpoxyController(backgroundHandler, backgroundHandler), Timeline.Listener {

    interface Callback {
        fun onEventsVisible(events: List<TimelineEvent>)
        fun onUrlClicked(url: String)
        fun onImageMessageClicked(messageImageContent: MessageImageContent, mediaData: ImageContentRenderer.Data, view: View)
        fun onVideoMessageClicked(messageVideoContent: MessageVideoContent, mediaData: VideoContentRenderer.Data, view: View)
        fun onFileMessageClicked(messageFileContent: MessageFileContent)
        fun onAudioMessageClicked(messageAudioContent: MessageAudioContent)
    }


    private val modelCache = arrayListOf<CacheItemData?>()
    private var currentSnapshot: List<TimelineEvent> = emptyList()
    private var inSubmitList: Boolean = false
    private var timeline: Timeline? = null

    var callback: Callback? = null

    private val listUpdateCallback = object : ListUpdateCallback {

        @Synchronized
        override fun onChanged(position: Int, count: Int, payload: Any?) {
            assertUpdateCallbacksAllowed()
            (position until (position + count)).forEach {
                modelCache[it] = null
            }
            requestModelBuild()
        }

        @Synchronized
        override fun onMoved(fromPosition: Int, toPosition: Int) {
            assertUpdateCallbacksAllowed()
            val model = modelCache.removeAt(fromPosition)
            modelCache.add(toPosition, model)
            requestModelBuild()
        }

        @Synchronized
        override fun onInserted(position: Int, count: Int) {
            assertUpdateCallbacksAllowed()
            // When adding backwards we need to clear some events
            if (position == modelCache.size) {
                val previousCachedModel = modelCache.getOrNull(position - 1)
                if (previousCachedModel != null) {
                    val numberOfMergedEvents = previousCachedModel.numberOfMergedEvents
                    for (i in 0..numberOfMergedEvents) {
                        modelCache[position - 1 - i] = null
                    }
                }
            }
            (0 until count).forEach {
                modelCache.add(position, null)
            }
            requestModelBuild()
        }

        @Synchronized
        override fun onRemoved(position: Int, count: Int) {
            assertUpdateCallbacksAllowed()
            (0 until count).forEach {
                modelCache.removeAt(position)
            }
            requestModelBuild()
        }
    }

    init {
        requestModelBuild()
    }

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


        val timelineModels = getModels()
        add(timelineModels)

        LoadingItemModel_()
                .id("backward_loading_item")
                .addWhen(Timeline.Direction.BACKWARDS)
    }

    // Timeline.LISTENER ***************************************************************************

    override fun onUpdated(snapshot: List<TimelineEvent>) {
        submitSnapshot(snapshot)
    }

    private fun submitSnapshot(newSnapshot: List<TimelineEvent>) {
        backgroundHandler.post {
            inSubmitList = true
            val diffCallback = TimelineEventDiffUtilCallback(currentSnapshot, newSnapshot)
            currentSnapshot = newSnapshot
            val diffResult = DiffUtil.calculateDiff(diffCallback)
            diffResult.dispatchUpdatesTo(listUpdateCallback)
            inSubmitList = false
        }
    }

    private fun assertUpdateCallbacksAllowed() {
        require(inSubmitList || Looper.myLooper() == backgroundHandler.looper)
    }

    @Synchronized
    private fun getModels(): List<EpoxyModel<*>> {
        (0 until modelCache.size).forEach { position ->
            if (modelCache[position] == null) {
                buildAndCacheItemsAt(position)
            }
        }
        return modelCache
                .map { listOf(it?.eventModel, it?.formattedDayModel) }
                .flatten()
                .filterNotNull()
    }

    private fun buildAndCacheItemsAt(position: Int) {
        val buildItemModelsResult = buildItemModels(position, currentSnapshot)
        modelCache[position] = buildItemModelsResult
        val prevResult = modelCache.getOrNull(position + 1)
        if (prevResult != null && prevResult.eventModel is RoomMemberMergedItem && buildItemModelsResult.eventModel is RoomMemberMergedItem) {
            buildItemModelsResult.eventModel.isCollapsed = prevResult.eventModel.isCollapsed
        }
        for (skipItemPosition in 0 until buildItemModelsResult.numberOfMergedEvents) {
            val dumbModelsResult = CacheItemData(numberOfMergedEvents = buildItemModelsResult.numberOfMergedEvents)
            modelCache[position + 1 + skipItemPosition] = dumbModelsResult
        }
    }

    private fun buildItemModels(currentPosition: Int, items: List<TimelineEvent>): CacheItemData {
        val event = items[currentPosition]
        val mergeableEvents = if (event.canBeMerged()) items.nextSameTypeEvents(currentPosition, minSize = 2) else emptyList()
        val mergedEvents = listOf(event) + mergeableEvents
        val nextDisplayableEvent = items.nextDisplayableEvent(currentPosition + mergeableEvents.size)

        val date = event.root.localDateTime()
        val nextDate = nextDisplayableEvent?.root?.localDateTime()
        val addDaySeparator = date.toLocalDate() != nextDate?.toLocalDate()
        val visibilityStateChangedListener = TimelineEventVisibilityStateChangedListener(callback, mergedEvents)
        val epoxyModelId = mergedEvents.joinToString(separator = "_") { it.localId }

        val eventModel = timelineItemFactory.create(event, mergeableEvents, nextDisplayableEvent, callback, visibilityStateChangedListener).also {
            it.id(epoxyModelId)
        }
        val daySeparatorItem = if (addDaySeparator) {
            val formattedDay = dateFormatter.formatMessageDay(date)
            DaySeparatorItem_().formattedDay(formattedDay).id(formattedDay)
        } else {
            null
        }
        return CacheItemData(eventModel, daySeparatorItem, mergeableEvents.size)
    }

    private fun LoadingItemModel_.addWhen(direction: Timeline.Direction) {
        val shouldAdd = timeline?.hasMoreToLoad(direction) ?: false
        addIf(shouldAdd, this@TimelineEventController)
    }

}

private data class CacheItemData(
        val eventModel: EpoxyModel<*>? = null,
        val formattedDayModel: EpoxyModel<*>? = null,
        val numberOfMergedEvents: Int = 0
)

