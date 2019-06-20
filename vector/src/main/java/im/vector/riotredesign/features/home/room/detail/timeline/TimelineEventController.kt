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
import im.vector.matrix.android.api.session.room.model.EditAggregatedSummary
import im.vector.matrix.android.api.session.room.model.message.*
import im.vector.matrix.android.api.session.room.timeline.Timeline
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.riotredesign.core.epoxy.LoadingItem_
import im.vector.riotredesign.core.extensions.localDateTime
import im.vector.riotredesign.features.home.room.detail.timeline.factory.TimelineItemFactory
import im.vector.riotredesign.features.home.room.detail.timeline.helper.*
import im.vector.riotredesign.features.home.room.detail.timeline.item.DaySeparatorItem
import im.vector.riotredesign.features.home.room.detail.timeline.item.DaySeparatorItem_
import im.vector.riotredesign.features.home.room.detail.timeline.item.MergedHeaderItem
import im.vector.riotredesign.features.home.room.detail.timeline.item.MessageInformationData
import im.vector.riotredesign.features.media.ImageContentRenderer
import im.vector.riotredesign.features.media.VideoContentRenderer
import org.threeten.bp.LocalDateTime

class TimelineEventController(private val dateFormatter: TimelineDateFormatter,
                              private val timelineItemFactory: TimelineItemFactory,
                              private val timelineMediaSizeProvider: TimelineMediaSizeProvider,
                              private val backgroundHandler: Handler = TimelineAsyncHelper.getBackgroundHandler()
) : EpoxyController(backgroundHandler, backgroundHandler), Timeline.Listener {

    interface Callback : ReactionPillCallback, AvatarCallback, BaseCallback, UrlClickCallback {
        fun onEventVisible(event: TimelineEvent)
        fun onEncryptedMessageClicked(informationData: MessageInformationData, view: View)
        fun onImageMessageClicked(messageImageContent: MessageImageContent, mediaData: ImageContentRenderer.Data, view: View)
        fun onVideoMessageClicked(messageVideoContent: MessageVideoContent, mediaData: VideoContentRenderer.Data, view: View)
        fun onFileMessageClicked(messageFileContent: MessageFileContent)
        fun onAudioMessageClicked(messageAudioContent: MessageAudioContent)
        fun onEditedDecorationClicked(informationData: MessageInformationData, editAggregatedSummary: EditAggregatedSummary?)
    }

    interface ReactionPillCallback {
        fun onClickOnReactionPill(informationData: MessageInformationData, reaction: String, on: Boolean)
        fun onLongClickOnReactionPill(informationData: MessageInformationData, reaction: String)
    }

    interface BaseCallback {
        fun onEventCellClicked(informationData: MessageInformationData, messageContent: MessageContent?, view: View)
        fun onEventLongClicked(informationData: MessageInformationData, messageContent: MessageContent?, view: View): Boolean
    }

    interface AvatarCallback {
        fun onAvatarClicked(informationData: MessageInformationData)
        fun onMemberNameClicked(informationData: MessageInformationData)
    }

    interface UrlClickCallback {
        fun onUrlClicked(url: String): Boolean
        fun onUrlLongClicked(url: String): Boolean
    }

    private val collapsedEventIds = linkedSetOf<String>()
    private val mergeItemCollapseStates = HashMap<String, Boolean>()
    private val modelCache = arrayListOf<CacheItemData?>()

    private var currentSnapshot: List<TimelineEvent> = emptyList()
    private var inSubmitList: Boolean = false
    private var timeline: Timeline? = null

    var callback: Callback? = null

    private val listUpdateCallback = object : ListUpdateCallback {

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            synchronized(modelCache) {
                assertUpdateCallbacksAllowed()
                (position until (position + count)).forEach {
                    modelCache[it] = null
                }
                requestModelBuild()
            }
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            synchronized(modelCache) {
                assertUpdateCallbacksAllowed()
                val model = modelCache.removeAt(fromPosition)
                modelCache.add(toPosition, model)
                requestModelBuild()
            }
        }

        override fun onInserted(position: Int, count: Int) {
            synchronized(modelCache) {
                assertUpdateCallbacksAllowed()
                (0 until count).forEach {
                    modelCache.add(position, null)
                }
                requestModelBuild()
            }
        }

        override fun onRemoved(position: Int, count: Int) {
            synchronized(modelCache) {
                assertUpdateCallbacksAllowed()
                (0 until count).forEach {
                    modelCache.removeAt(position)
                }
                requestModelBuild()
            }
        }
    }

    init {
        requestModelBuild()
    }

    fun setTimeline(timeline: Timeline?, eventIdToHighlight: String?) {
        if (this.timeline != timeline) {
            this.timeline = timeline
            this.timeline?.listener = this

            // Clear cache
            synchronized(modelCache) {
                for (i in 0 until modelCache.size) {
                    modelCache[i] = null
                }
            }
        }

        if (this.eventIdToHighlight != eventIdToHighlight) {
            // Clear cache to force a refresh
            synchronized(modelCache) {
                for (i in 0 until modelCache.size) {
                    if (modelCache[i]?.eventId == eventIdToHighlight
                            || modelCache[i]?.eventId == this.eventIdToHighlight) {
                        modelCache[i] = null
                    }
                }
            }
            this.eventIdToHighlight = eventIdToHighlight

            requestModelBuild()
        }
    }

    private var eventIdToHighlight: String? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        timelineMediaSizeProvider.recyclerView = recyclerView
    }

    override fun buildModels() {
        LoadingItem_()
                .id("forward_loading_item")
                .addWhen(Timeline.Direction.FORWARDS)

        val timelineModels = getModels()
        add(timelineModels)

        LoadingItem_()
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

    private fun getModels(): List<EpoxyModel<*>> {
        synchronized(modelCache) {
            (0 until modelCache.size).forEach { position ->
                // Should be build if not cached or if cached but contains mergedHeader or formattedDay
                // We then are sure we always have items up to date.
                if (modelCache[position] == null
                        || modelCache[position]?.mergedHeaderModel != null
                        || modelCache[position]?.formattedDayModel != null) {
                    modelCache[position] = buildItemModels(position, currentSnapshot)
                }
            }
            return modelCache
                    .map {
                        val eventModel = if (it == null || collapsedEventIds.contains(it.localId)) {
                            null
                        } else {
                            it.eventModel
                        }
                        listOf(eventModel, it?.mergedHeaderModel, it?.formattedDayModel)
                    }
                    .flatten()
                    .filterNotNull()
        }
    }


    private fun buildItemModels(currentPosition: Int, items: List<TimelineEvent>): CacheItemData {
        val event = items[currentPosition]
        val nextEvent = items.nextDisplayableEvent(currentPosition)
        val date = event.root.localDateTime()
        val nextDate = nextEvent?.root?.localDateTime()
        val addDaySeparator = date.toLocalDate() != nextDate?.toLocalDate()

        val eventModel = timelineItemFactory.create(event, nextEvent, eventIdToHighlight, callback).also {
            it.id(event.localId)
            it.setOnVisibilityStateChanged(TimelineEventVisibilityStateChangedListener(callback, event))
        }
        val mergedHeaderModel = buildMergedHeaderItem(event, nextEvent, items, addDaySeparator, currentPosition)
        val daySeparatorItem = buildDaySeparatorItem(addDaySeparator, date)

        return CacheItemData(event.localId, event.root.eventId, eventModel, mergedHeaderModel, daySeparatorItem)
    }

    private fun buildDaySeparatorItem(addDaySeparator: Boolean, date: LocalDateTime): DaySeparatorItem? {
        return if (addDaySeparator) {
            val formattedDay = dateFormatter.formatMessageDay(date)
            DaySeparatorItem_().formattedDay(formattedDay).id(formattedDay)
        } else {
            null
        }
    }

    // TODO Phase 3 Handle the case where the eventId we have to highlight is merged
    private fun buildMergedHeaderItem(event: TimelineEvent,
                                      nextEvent: TimelineEvent?,
                                      items: List<TimelineEvent>,
                                      addDaySeparator: Boolean,
                                      currentPosition: Int): MergedHeaderItem? {
        return if (!event.canBeMerged() || (nextEvent?.root?.getClearType() == event.root.getClearType() && !addDaySeparator)) {
            null
        } else {
            val prevSameTypeEvents = items.prevSameTypeEvents(currentPosition, 2)
            if (prevSameTypeEvents.isEmpty()) {
                null
            } else {
                val mergedEvents = (prevSameTypeEvents + listOf(event)).asReversed()
                val mergedData = mergedEvents.map { mergedEvent ->
                    val senderAvatar = mergedEvent.senderAvatar()
                    val senderName = mergedEvent.senderName()
                    MergedHeaderItem.Data(
                            userId = mergedEvent.root.senderId ?: "",
                            avatarUrl = senderAvatar,
                            memberName = senderName ?: "",
                            eventId = mergedEvent.localId
                    )
                }
                val mergedEventIds = mergedEvents.map { it.localId }
                // We try to find if one of the item id were used as mergeItemCollapseStates key
                // => handle case where paginating from mergeable events and we get more
                val previousCollapseStateKey = mergedEventIds.intersect(mergeItemCollapseStates.keys).firstOrNull()
                val initialCollapseState = mergeItemCollapseStates.remove(previousCollapseStateKey)
                        ?: true
                val isCollapsed = mergeItemCollapseStates.getOrPut(event.localId) { initialCollapseState }
                if (isCollapsed) {
                    collapsedEventIds.addAll(mergedEventIds)
                } else {
                    collapsedEventIds.removeAll(mergedEventIds)
                }
                val mergeId = mergedEventIds.joinToString(separator = "_") { it }
                MergedHeaderItem(isCollapsed, mergeId, mergedData) {
                    mergeItemCollapseStates[event.localId] = it
                    requestModelBuild()
                }
            }
        }
    }

    private fun LoadingItem_.addWhen(direction: Timeline.Direction) {
        val shouldAdd = timeline?.hasMoreToLoad(direction) ?: false
        addIf(shouldAdd, this@TimelineEventController)
    }

    fun searchPositionOfEvent(eventId: String): Int? {
        synchronized(modelCache) {
            // Search in the cache
            modelCache.forEachIndexed { idx, cacheItemData ->
                if (cacheItemData?.eventId == eventId) {
                    return idx
                }
            }

            return null
        }
    }
}

private data class CacheItemData(
        val localId: String,
        val eventId: String?,
        val eventModel: EpoxyModel<*>? = null,
        val mergedHeaderModel: MergedHeaderItem? = null,
        val formattedDayModel: DaySeparatorItem? = null
)
