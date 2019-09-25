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

package im.vector.riotx.features.home.room.detail.timeline

import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.VisibilityState
import im.vector.matrix.android.api.session.room.model.message.*
import im.vector.matrix.android.api.session.room.timeline.Timeline
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.riotx.core.date.VectorDateFormatter
import im.vector.riotx.core.epoxy.LoadingItem_
import im.vector.riotx.core.extensions.localDateTime
import im.vector.riotx.features.home.room.detail.RoomDetailViewState
import im.vector.riotx.features.home.room.detail.timeline.factory.MergedHeaderItemFactory
import im.vector.riotx.features.home.room.detail.timeline.factory.TimelineItemFactory
import im.vector.riotx.features.home.room.detail.timeline.helper.TimelineEventDiffUtilCallback
import im.vector.riotx.features.home.room.detail.timeline.helper.TimelineEventVisibilityStateChangedListener
import im.vector.riotx.features.home.room.detail.timeline.helper.TimelineMediaSizeProvider
import im.vector.riotx.features.home.room.detail.timeline.helper.nextOrNull
import im.vector.riotx.features.home.room.detail.timeline.item.*
import im.vector.riotx.features.media.ImageContentRenderer
import im.vector.riotx.features.media.VideoContentRenderer
import org.threeten.bp.LocalDateTime
import javax.inject.Inject

class TimelineEventController @Inject constructor(private val dateFormatter: VectorDateFormatter,
                                                  private val timelineItemFactory: TimelineItemFactory,
                                                  private val timelineMediaSizeProvider: TimelineMediaSizeProvider,
                                                  private val mergedHeaderItemFactory: MergedHeaderItemFactory,
                                                  @TimelineEventControllerHandler
                                                  private val backgroundHandler: Handler
) : EpoxyController(backgroundHandler, backgroundHandler), Timeline.Listener {

    interface Callback : BaseCallback, ReactionPillCallback, AvatarCallback, UrlClickCallback, ReadReceiptsCallback {
        fun onLoadMore(direction: Timeline.Direction)
        fun onEventInvisible(event: TimelineEvent)
        fun onEventVisible(event: TimelineEvent)
        fun onRoomCreateLinkClicked(url: String)
        fun onEncryptedMessageClicked(informationData: MessageInformationData, view: View)
        fun onImageMessageClicked(messageImageContent: MessageImageContent, mediaData: ImageContentRenderer.Data, view: View)
        fun onVideoMessageClicked(messageVideoContent: MessageVideoContent, mediaData: VideoContentRenderer.Data, view: View)
        fun onFileMessageClicked(eventId: String, messageFileContent: MessageFileContent)
        fun onAudioMessageClicked(messageAudioContent: MessageAudioContent)
        fun onEditedDecorationClicked(informationData: MessageInformationData)
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

    interface ReadReceiptsCallback {
        fun onReadReceiptsClicked(readReceipts: List<ReadReceiptData>)
        fun onReadMarkerLongBound(readMarkerId: String, isDisplayed: Boolean)
    }

    interface UrlClickCallback {
        fun onUrlClicked(url: String): Boolean
        fun onUrlLongClicked(url: String): Boolean
    }

    private var showingForwardLoader = false
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

    fun update(viewState: RoomDetailViewState, readMarkerVisible: Boolean) {
        if (timeline != viewState.timeline) {
            timeline = viewState.timeline
            timeline?.listener = this
            // Clear cache
            synchronized(modelCache) {
                for (i in 0 until modelCache.size) {
                    modelCache[i] = null
                }
            }
        }
        var requestModelBuild = false
        if (eventIdToHighlight != viewState.highlightedEventId) {
            // Clear cache to force a refresh
            synchronized(modelCache) {
                for (i in 0 until modelCache.size) {
                    if (modelCache[i]?.eventId == viewState.highlightedEventId
                        || modelCache[i]?.eventId == eventIdToHighlight) {
                        modelCache[i] = null
                    }
                }
            }
            eventIdToHighlight = viewState.highlightedEventId
            requestModelBuild = true
        }
        if (this.readMarkerVisible != readMarkerVisible) {
            this.readMarkerVisible = readMarkerVisible
            requestModelBuild = true
        }
        if (requestModelBuild) {
            requestModelBuild()
        }
    }

    private var readMarkerVisible: Boolean = false
    private var eventIdToHighlight: String? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        timelineMediaSizeProvider.recyclerView = recyclerView
    }

    override fun buildModels() {
        val timestamp = System.currentTimeMillis()
        showingForwardLoader = LoadingItem_()
                .id("forward_loading_item_$timestamp")
                .setVisibilityStateChangedListener(Timeline.Direction.FORWARDS)
                .addWhen(Timeline.Direction.FORWARDS)

        val timelineModels = getModels()
        add(timelineModels)

        // Avoid displaying two loaders if there is no elements between them
        if (!showingForwardLoader || timelineModels.isNotEmpty()) {
            LoadingItem_()
                    .id("backward_loading_item_$timestamp")
                    .setVisibilityStateChangedListener(Timeline.Direction.BACKWARDS)
                    .addWhen(Timeline.Direction.BACKWARDS)
        }
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
                        val eventModel = if (it == null || mergedHeaderItemFactory.isCollapsed(it.localId)) {
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
        val nextEvent = items.nextOrNull(currentPosition)
        val date = event.root.localDateTime()
        val nextDate = nextEvent?.root?.localDateTime()
        val addDaySeparator = date.toLocalDate() != nextDate?.toLocalDate()
        // Don't show read marker if it's on first item
        val showReadMarker = if (currentPosition == 0 && event.hasReadMarker) {
            false
        } else {
            readMarkerVisible
        }
        val eventModel = timelineItemFactory.create(event, nextEvent, eventIdToHighlight, showReadMarker, callback).also {
            it.id(event.localId)
            it.setOnVisibilityStateChanged(TimelineEventVisibilityStateChangedListener(callback, event))
        }
        val mergedHeaderModel = mergedHeaderItemFactory.create(event,
                                                               nextEvent = nextEvent,
                                                               items = items,
                                                               addDaySeparator = addDaySeparator,
                                                               readMarkerVisible = readMarkerVisible,
                                                               currentPosition = currentPosition,
                                                               eventIdToHighlight = eventIdToHighlight,
                                                               callback = callback
        ) {
            requestModelBuild()
        }
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

    /**
     * Return true if added
     */
    private fun LoadingItem_.addWhen(direction: Timeline.Direction): Boolean {
        val shouldAdd = timeline?.hasMoreToLoad(direction) ?: false
        addIf(shouldAdd, this@TimelineEventController)
        return shouldAdd
    }

    /**
     * Return true if added
     */
    private fun LoadingItem_.setVisibilityStateChangedListener(direction: Timeline.Direction): LoadingItem_ {
        return onVisibilityStateChanged { model, view, visibilityState ->
            if (visibilityState == VisibilityState.VISIBLE) {
                callback?.onLoadMore(direction)
            }
        }
    }

    fun searchPositionOfEvent(eventId: String?): Int? = synchronized(modelCache) {
        // Search in the cache
        if (eventId == null) {
            return null
        }
        var realPosition = 0
        if (showingForwardLoader) {
            realPosition++
        }
        for (i in 0 until modelCache.size) {
            val itemCache = modelCache[i] ?: continue
            if (itemCache.eventId == eventId) {
                return realPosition
            }
            if (itemCache.eventModel != null && !mergedHeaderItemFactory.isCollapsed(itemCache.localId)) {
                realPosition++
            }
            if (itemCache.mergedHeaderModel != null) {
                realPosition++
            }
            if (itemCache.formattedDayModel != null) {
                realPosition++
            }
        }
        return null
    }

    fun isLoadingForward() = showingForwardLoader

    private data class CacheItemData(
            val localId: Long,
            val eventId: String?,
            val eventModel: EpoxyModel<*>? = null,
            val mergedHeaderModel: MergedHeaderItem? = null,
            val formattedDayModel: DaySeparatorItem? = null
    )

}