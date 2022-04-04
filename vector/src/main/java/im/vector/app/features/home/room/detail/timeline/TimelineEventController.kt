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

package im.vector.app.features.home.room.detail.timeline

import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.VisibilityState
import im.vector.app.core.date.DateFormatKind
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.epoxy.LoadingItem_
import im.vector.app.core.extensions.localDateTime
import im.vector.app.core.extensions.nextOrNull
import im.vector.app.core.extensions.prevOrNull
import im.vector.app.features.home.room.detail.JitsiState
import im.vector.app.features.home.room.detail.RoomDetailAction
import im.vector.app.features.home.room.detail.RoomDetailViewState
import im.vector.app.features.home.room.detail.UnreadState
import im.vector.app.features.home.room.detail.timeline.factory.MergedHeaderItemFactory
import im.vector.app.features.home.room.detail.timeline.factory.ReadReceiptsItemFactory
import im.vector.app.features.home.room.detail.timeline.factory.TimelineItemFactory
import im.vector.app.features.home.room.detail.timeline.factory.TimelineItemFactoryParams
import im.vector.app.features.home.room.detail.timeline.helper.ContentDownloadStateTrackerBinder
import im.vector.app.features.home.room.detail.timeline.helper.ContentUploadStateTrackerBinder
import im.vector.app.features.home.room.detail.timeline.helper.ReactionsSummaryFactory
import im.vector.app.features.home.room.detail.timeline.helper.TimelineControllerInterceptorHelper
import im.vector.app.features.home.room.detail.timeline.helper.TimelineEventDiffUtilCallback
import im.vector.app.features.home.room.detail.timeline.helper.TimelineEventVisibilityHelper
import im.vector.app.features.home.room.detail.timeline.helper.TimelineEventVisibilityStateChangedListener
import im.vector.app.features.home.room.detail.timeline.helper.TimelineEventsGroups
import im.vector.app.features.home.room.detail.timeline.helper.TimelineMediaSizeProvider
import im.vector.app.features.home.room.detail.timeline.item.BasedMergedItem
import im.vector.app.features.home.room.detail.timeline.item.DaySeparatorItem
import im.vector.app.features.home.room.detail.timeline.item.DaySeparatorItem_
import im.vector.app.features.home.room.detail.timeline.item.ItemWithEvents
import im.vector.app.features.home.room.detail.timeline.item.MessageInformationData
import im.vector.app.features.home.room.detail.timeline.item.ReactionsSummaryEvents
import im.vector.app.features.home.room.detail.timeline.item.ReadReceiptData
import im.vector.app.features.home.room.detail.timeline.item.ReadReceiptsItem
import im.vector.app.features.home.room.detail.timeline.url.PreviewUrlRetriever
import im.vector.app.features.media.AttachmentData
import im.vector.app.features.media.ImageContentRenderer
import im.vector.app.features.media.VideoContentRenderer
import im.vector.app.features.settings.VectorPreferences
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.ReadReceipt
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.message.MessageAudioContent
import org.matrix.android.sdk.api.session.room.model.message.MessageImageInfoContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVideoContent
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.min
import kotlin.system.measureTimeMillis

class TimelineEventController @Inject constructor(private val dateFormatter: VectorDateFormatter,
                                                  private val vectorPreferences: VectorPreferences,
                                                  private val contentUploadStateTrackerBinder: ContentUploadStateTrackerBinder,
                                                  private val contentDownloadStateTrackerBinder: ContentDownloadStateTrackerBinder,
                                                  private val timelineItemFactory: TimelineItemFactory,
                                                  private val timelineMediaSizeProvider: TimelineMediaSizeProvider,
                                                  private val mergedHeaderItemFactory: MergedHeaderItemFactory,
                                                  private val session: Session,
                                                  @TimelineEventControllerHandler
                                                  private val backgroundHandler: Handler,
                                                  private val timelineEventVisibilityHelper: TimelineEventVisibilityHelper,
                                                  private val readReceiptsItemFactory: ReadReceiptsItemFactory,
                                                  private val reactionListFactory: ReactionsSummaryFactory
) : EpoxyController(backgroundHandler, backgroundHandler), Timeline.Listener, EpoxyController.Interceptor {

    /**
     * This is a partial state of the RoomDetailViewState
     */
    data class PartialState(
            val unreadState: UnreadState = UnreadState.Unknown,
            val highlightedEventId: String? = null,
            val jitsiState: JitsiState = JitsiState(),
            val roomSummary: RoomSummary? = null,
            val rootThreadEventId: String? = null
    ) {

        constructor(state: RoomDetailViewState) : this(
                unreadState = state.unreadState,
                highlightedEventId = state.highlightedEventId,
                jitsiState = state.jitsiState,
                roomSummary = state.asyncRoomSummary(),
                rootThreadEventId = state.rootThreadEventId
        )

        fun isFromThreadTimeline(): Boolean = rootThreadEventId != null
    }

    interface Callback :
            BaseCallback,
            ReactionPillCallback,
            AvatarCallback,
            ThreadCallback,
            UrlClickCallback,
            ReadReceiptsCallback,
            PreviewUrlCallback {
        fun onLoadMore(direction: Timeline.Direction)
        fun onEventInvisible(event: TimelineEvent)
        fun onEventVisible(event: TimelineEvent)
        fun onRoomCreateLinkClicked(url: String)
        fun onEncryptedMessageClicked(informationData: MessageInformationData, view: View)
        fun onImageMessageClicked(messageImageContent: MessageImageInfoContent,
                                  mediaData: ImageContentRenderer.Data,
                                  view: View,
                                  inMemory: List<AttachmentData>)
        fun onVideoMessageClicked(messageVideoContent: MessageVideoContent, mediaData: VideoContentRenderer.Data, view: View)

        //        fun onFileMessageClicked(eventId: String, messageFileContent: MessageFileContent)
//        fun onAudioMessageClicked(messageAudioContent: MessageAudioContent)
        fun onEditedDecorationClicked(informationData: MessageInformationData)

        // TODO move all callbacks to this?
        fun onTimelineItemAction(itemAction: RoomDetailAction)

        // Introduce ViewModel scoped component (or Hilt?)
        fun getPreviewUrlRetriever(): PreviewUrlRetriever

        fun onVoiceControlButtonClicked(eventId: String, messageAudioContent: MessageAudioContent)
        fun onVoiceWaveformTouchedUp(eventId: String, duration: Int, percentage: Float)
        fun onVoiceWaveformMovedTo(eventId: String, duration: Int, percentage: Float)

        fun onAudioSeekBarMovedTo(eventId: String, duration: Int, percentage: Float)

        fun onAddMoreReaction(event: TimelineEvent)
    }

    interface ReactionPillCallback {
        fun onClickOnReactionPill(informationData: MessageInformationData, reaction: String, on: Boolean)
        fun onLongClickOnReactionPill(informationData: MessageInformationData, reaction: String)
    }

    interface BaseCallback {
        fun onEventCellClicked(informationData: MessageInformationData, messageContent: Any?, view: View, isRootThreadEvent: Boolean)
        fun onEventLongClicked(informationData: MessageInformationData, messageContent: Any?, view: View): Boolean
    }

    interface AvatarCallback {
        fun onAvatarClicked(informationData: MessageInformationData)
        fun onMemberNameClicked(informationData: MessageInformationData)
    }

    interface ThreadCallback {
        fun onThreadSummaryClicked(eventId: String, isRootThreadEvent: Boolean): Boolean
    }

    interface ReadReceiptsCallback {
        fun onReadReceiptsClicked(readReceipts: List<ReadReceiptData>)
        fun onReadMarkerVisible()
    }

    interface UrlClickCallback {
        fun onUrlClicked(url: String, title: String): Boolean
        fun onUrlLongClicked(url: String): Boolean
    }

    interface PreviewUrlCallback {
        fun onPreviewUrlClicked(url: String)
        fun onPreviewUrlCloseClicked(eventId: String, url: String)
        fun onPreviewUrlImageClicked(sharedView: View?, mxcUrl: String?, title: String?)
    }

    // Map eventId to adapter position
    private val adapterPositionMapping = HashMap<String, Int>()
    private val timelineEventsGroups = TimelineEventsGroups()
    private val receiptsByEvent = HashMap<String, MutableList<ReadReceipt>>()
    private val modelCache = arrayListOf<CacheItemData?>()
    private var currentSnapshot: List<TimelineEvent> = emptyList()
    private var inSubmitList: Boolean = false
    private var hasReachedInvite: Boolean = false
    private var hasUTD: Boolean = false
    private var positionOfReadMarker: Int? = null
    private var partialState: PartialState = PartialState()

    var callback: Callback? = null
    var timeline: Timeline? = null

    private val listUpdateCallback = object : ListUpdateCallback {

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            synchronized(modelCache) {
                assertUpdateCallbacksAllowed()
                Timber.v("listUpdateCallback.onChanged(position: $position, count: $count). " +
                        "\ncurrentSnapshot has size of ${currentSnapshot.size} items")
                (position until position + count).forEach {
                    // Invalidate cache
                    modelCache[it] = null
                }
                // Also invalidate the first previous displayable event if
                // it's sent by the same user so we are sure we have up to date information.
                val invalidatedSenderId: String? = currentSnapshot.getOrNull(position)?.senderInfo?.userId
                // In some cases onChanged will be called before onRemoved and onInserted so position will be bigger than currentSnapshot.size.
                val prevList = currentSnapshot.subList(0, min(position, currentSnapshot.size))
                val prevDisplayableEventIndex = prevList.indexOfLast {
                    timelineEventVisibilityHelper.shouldShowEvent(
                            timelineEvent = it,
                            highlightedEventId = partialState.highlightedEventId,
                            isFromThreadTimeline = partialState.isFromThreadTimeline(),
                            rootThreadEventId = partialState.rootThreadEventId
                    )
                }
                if (prevDisplayableEventIndex != -1 && currentSnapshot.getOrNull(prevDisplayableEventIndex)?.senderInfo?.userId == invalidatedSenderId) {
                    modelCache[prevDisplayableEventIndex] = null
                }
                requestModelBuild()
            }
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            synchronized(modelCache) {
                assertUpdateCallbacksAllowed()
                Timber.v("listUpdateCallback.onMoved(fromPosition: $fromPosition, toPosition: $toPosition). " +
                        "\ncurrentSnapshot has size of ${currentSnapshot.size} items")
                val model = modelCache.removeAt(fromPosition)
                modelCache.add(toPosition, model)
                requestModelBuild()
            }
        }

        override fun onInserted(position: Int, count: Int) {
            synchronized(modelCache) {
                assertUpdateCallbacksAllowed()
                Timber.v("listUpdateCallback.onInserted(position: $position, count: $count). " +
                        "\ncurrentSnapshot has size of ${currentSnapshot.size} items")
                repeat(count) {
                    modelCache.add(position, null)
                }
                requestModelBuild()
            }
        }

        override fun onRemoved(position: Int, count: Int) {
            synchronized(modelCache) {
                assertUpdateCallbacksAllowed()
                Timber.v("listUpdateCallback.onRemoved(position: $position, count: $count). " +
                        "\ncurrentSnapshot has size of ${currentSnapshot.size} items")
                repeat(count) {
                    modelCache.removeAt(position)
                }
                requestModelBuild()
            }
        }
    }

    private val interceptorHelper = TimelineControllerInterceptorHelper(
            ::positionOfReadMarker,
            adapterPositionMapping
    )

    init {
        addInterceptor(this)
        requestModelBuild()
    }

    override fun intercept(models: MutableList<EpoxyModel<*>>) = synchronized(modelCache) {
        interceptorHelper.intercept(models, partialState.unreadState, timeline, callback)
    }

    fun update(viewState: RoomDetailViewState) {
        val newPartialState = PartialState(viewState)
        if (newPartialState != partialState) {
            partialState = newPartialState
            requestModelBuild()
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        timeline?.addListener(this)
        timelineMediaSizeProvider.recyclerView = recyclerView
        reactionListFactory.onRequestBuild = { requestModelBuild() }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        timelineMediaSizeProvider.recyclerView = null
        contentUploadStateTrackerBinder.clear()
        contentDownloadStateTrackerBinder.clear()
        timeline?.removeListener(this)
        reactionListFactory.onRequestBuild = null
        super.onDetachedFromRecyclerView(recyclerView)
    }

    override fun buildModels() {
        // Don't build anything if membership is not joined
        if (partialState.roomSummary?.membership != Membership.JOIN) {
            return
        }
        val timestamp = System.currentTimeMillis()

        val showingForwardLoader = LoadingItem_()
                .id("forward_loading_item_$timestamp")
                .setVisibilityStateChangedListener(Timeline.Direction.FORWARDS)
                .addWhenLoading(Timeline.Direction.FORWARDS)

        val timelineModels = getModels()
        add(timelineModels)
        if (hasReachedInvite && hasUTD) {
            return
        }
        // Avoid displaying two loaders if there is no elements between them
        val showBackwardsLoader = !showingForwardLoader || timelineModels.isNotEmpty()
        // We can hide the loader but still add the item to controller so it can trigger backwards pagination
        LoadingItem_()
                .id("backward_loading_item_$timestamp")
                .setVisibilityStateChangedListener(Timeline.Direction.BACKWARDS)
                .showLoader(showBackwardsLoader)
                .addWhenLoading(Timeline.Direction.BACKWARDS)
    }

// Timeline.LISTENER ***************************************************************************

    override fun onTimelineUpdated(snapshot: List<TimelineEvent>) {
        submitSnapshot(snapshot)
    }

    private fun submitSnapshot(newSnapshot: List<TimelineEvent>) {
        // Update is triggered on any DB change
        backgroundHandler.post {
            inSubmitList = true
            val diffCallback = TimelineEventDiffUtilCallback(currentSnapshot, newSnapshot)
            currentSnapshot = newSnapshot
            Timber.v("Submit a new snapshot of ${currentSnapshot.size} items.")
            val diffResult = DiffUtil.calculateDiff(diffCallback)
            diffResult.dispatchUpdatesTo(listUpdateCallback)
            requestDelayedModelBuild(0)
            inSubmitList = false
        }
    }

    private fun assertUpdateCallbacksAllowed() {
        require(inSubmitList || Looper.myLooper() == backgroundHandler.looper)
    }

    private fun getModels(): List<EpoxyModel<*>> {
        val timeForBuilding = measureTimeMillis {
            buildCacheItemsIfNeeded()
        }
        Timber.v("Time for building cache items: $timeForBuilding ms")
        return modelCache
                .map { cacheItemData ->
                    val eventModel = if (cacheItemData == null || mergedHeaderItemFactory.isCollapsed(cacheItemData.localId)) {
                        null
                    } else {
                        cacheItemData.eventModel
                    }
                    listOf(
                            cacheItemData?.readReceiptsItem?.takeUnless { mergedHeaderItemFactory.isCollapsed(cacheItemData.localId) },
                            eventModel,
                            cacheItemData?.mergedHeaderModel,
                            cacheItemData?.formattedDayModel?.takeIf { eventModel != null || cacheItemData.mergedHeaderModel != null }
                    )
                }
                .flatten()
                .filterNotNull()
    }

    private fun buildCacheItemsIfNeeded() = synchronized(modelCache) {
        hasUTD = false
        hasReachedInvite = false
        if (modelCache.isEmpty()) {
            return
        }
        val preprocessEventsTiming = measureTimeMillis {
            preprocessReverseEvents()
        }
        Timber.v("Preprocess events took $preprocessEventsTiming ms")
        var numberOfEventsToBuild = 0
        val lastSentEventWithoutReadReceipts = searchLastSentEventWithoutReadReceipts(receiptsByEvent)
        (0 until modelCache.size).forEach { position ->
            val event = currentSnapshot[position]
            val nextEvent = currentSnapshot.nextOrNull(position)
            // Should be build if not cached or if model should be refreshed
            if (modelCache[position] == null || modelCache[position]?.isCacheable(partialState) == false || reactionListFactory.needsRebuild(event)) {
                val prevEvent = currentSnapshot.prevOrNull(position)
                val prevDisplayableEvent = currentSnapshot.subList(0, position).lastOrNull {
                    timelineEventVisibilityHelper.shouldShowEvent(
                            timelineEvent = it,
                            highlightedEventId = partialState.highlightedEventId,
                            isFromThreadTimeline = partialState.isFromThreadTimeline(),
                            rootThreadEventId = partialState.rootThreadEventId)
                }
                val nextDisplayableEvent = currentSnapshot.subList(position + 1, currentSnapshot.size).firstOrNull {
                    timelineEventVisibilityHelper.shouldShowEvent(
                            timelineEvent = it,
                            highlightedEventId = partialState.highlightedEventId,
                            isFromThreadTimeline = partialState.isFromThreadTimeline(),
                            rootThreadEventId = partialState.rootThreadEventId)
                }
                val timelineEventsGroup = timelineEventsGroups.getOrNull(event)
                val params = TimelineItemFactoryParams(
                        event = event,
                        prevEvent = prevEvent,
                        prevDisplayableEvent = prevDisplayableEvent,
                        nextEvent = nextEvent,
                        nextDisplayableEvent = nextDisplayableEvent,
                        partialState = partialState,
                        lastSentEventIdWithoutReadReceipts = lastSentEventWithoutReadReceipts,
                        callback = callback,
                        eventsGroup = timelineEventsGroup,
                        reactionsSummaryEvents = ReactionsSummaryEvents(
                                onAddMoreClicked = { reactionListFactory.onAddMoreClicked(callback, event) },
                                onShowLessClicked = { reactionListFactory.onShowLessClicked(event.eventId) },
                                onShowMoreClicked = { reactionListFactory.onShowMoreClicked(event.eventId) }
                        )
                )
                modelCache[position] = buildCacheItem(params)
                numberOfEventsToBuild++
            }
            val itemCachedData = modelCache[position] ?: return@forEach
            // Then update with additional models if needed
            modelCache[position] = itemCachedData.enrichWithModels(event, nextEvent, position, receiptsByEvent)
        }
        Timber.v("Number of events to rebuild: $numberOfEventsToBuild on ${modelCache.size} total events")
    }

    private fun buildCacheItem(params: TimelineItemFactoryParams): CacheItemData {
        val event = params.event
        if (hasReachedInvite && hasUTD) {
            return CacheItemData(event.localId, event.root.eventId)
        }
        updateUTDStates(event, params.nextEvent)
        val eventModel = timelineItemFactory.create(params).also {
            it.id(event.localId)
            it.setOnVisibilityStateChanged(TimelineEventVisibilityStateChangedListener(callback, event))
        }
        val isCacheable = (eventModel !is ItemWithEvents || eventModel.isCacheable()) && !params.isHighlighted
        return CacheItemData(
                localId = event.localId,
                eventId = event.root.eventId,
                eventModel = eventModel,
                isCacheable = isCacheable
        )
    }

    private fun CacheItemData.enrichWithModels(event: TimelineEvent,
                                               nextEvent: TimelineEvent?,
                                               position: Int,
                                               receiptsByEvents: Map<String, List<ReadReceipt>>): CacheItemData {
        val wantsDateSeparator = wantsDateSeparator(event, nextEvent)
        val mergedHeaderModel = mergedHeaderItemFactory.create(event,
                nextEvent = nextEvent,
                partialState = partialState,
                items = this@TimelineEventController.currentSnapshot,
                addDaySeparator = wantsDateSeparator,
                currentPosition = position,
                eventIdToHighlight = partialState.highlightedEventId,
                callback = callback
        ) {
            requestModelBuild()
        }
        val formattedDayModel = if (wantsDateSeparator) {
            buildDaySeparatorItem(event.root.originServerTs)
        } else {
            null
        }
        val readReceipts = receiptsByEvents[event.eventId].orEmpty()
        return copy(
                readReceiptsItem = readReceiptsItemFactory.create(
                        event.eventId,
                        readReceipts,
                        callback,
                        partialState.isFromThreadTimeline()
                ),
                formattedDayModel = formattedDayModel,
                mergedHeaderModel = mergedHeaderModel
        )
    }

    private fun searchLastSentEventWithoutReadReceipts(receiptsByEvent: Map<String, List<ReadReceipt>>): String? {
        if (timeline?.isLive == false) {
            // If timeline is not live we don't want to show SentStatus
            return null
        }
        for (event in currentSnapshot) {
            // If there is any RR on the event, we stop searching for Sent event
            if (receiptsByEvent[event.eventId]?.isNotEmpty() == true) {
                return null
            }
            // If the event is not shown, we go to the next one
            if (!timelineEventVisibilityHelper.shouldShowEvent(
                            timelineEvent = event,
                            highlightedEventId = partialState.highlightedEventId,
                            isFromThreadTimeline = partialState.isFromThreadTimeline(),
                            rootThreadEventId = partialState.rootThreadEventId
                    )) {
                continue
            }
            // If the event is sent by us, we update the holder with the eventId and stop the search
            if (event.root.senderId == session.myUserId && event.root.sendState.isSent()) {
                return event.eventId
            }
        }
        return null
    }

    private fun preprocessReverseEvents() {
        receiptsByEvent.clear()
        timelineEventsGroups.clear()
        val itr = currentSnapshot.listIterator(currentSnapshot.size)
        var lastShownEventId: String? = null
        while (itr.hasPrevious()) {
            val event = itr.previous()
            timelineEventsGroups.addOrIgnore(event)
            val currentReadReceipts = ArrayList(event.readReceipts).filter {
                it.roomMember.userId != session.myUserId
            }
            if (timelineEventVisibilityHelper.shouldShowEvent(
                            timelineEvent = event,
                            highlightedEventId = partialState.highlightedEventId,
                            isFromThreadTimeline = partialState.isFromThreadTimeline(),
                            rootThreadEventId = partialState.rootThreadEventId)) {
                lastShownEventId = event.eventId
            }
            if (lastShownEventId == null) {
                continue
            }
            val existingReceipts = receiptsByEvent.getOrPut(lastShownEventId) { ArrayList() }
            existingReceipts.addAll(currentReadReceipts)
        }
    }

    private fun buildDaySeparatorItem(originServerTs: Long?): DaySeparatorItem {
        val formattedDay = dateFormatter.format(originServerTs, DateFormatKind.TIMELINE_DAY_DIVIDER)
        return DaySeparatorItem_().formattedDay(formattedDay).id(formattedDay)
    }

    private fun LoadingItem_.setVisibilityStateChangedListener(direction: Timeline.Direction): LoadingItem_ {
        val host = this@TimelineEventController
        return onVisibilityStateChanged { _, _, visibilityState ->
            if (visibilityState == VisibilityState.VISIBLE) {
                host.callback?.onLoadMore(direction)
            }
        }
    }

    private fun updateUTDStates(event: TimelineEvent, nextEvent: TimelineEvent?) {
        if (vectorPreferences.labShowCompleteHistoryInEncryptedRoom()) {
            return
        }
        if (event.root.type == EventType.STATE_ROOM_MEMBER &&
                event.root.stateKey == session.myUserId) {
            val content = event.root.content.toModel<RoomMemberContent>()
            if (content?.membership == Membership.INVITE) {
                hasReachedInvite = true
            } else if (content?.membership == Membership.JOIN) {
                val prevContent = event.root.resolvedPrevContent().toModel<RoomMemberContent>()
                if (prevContent?.membership?.isActive() == false) {
                    hasReachedInvite = true
                }
            }
        }
        if (nextEvent?.root?.getClearType() == EventType.ENCRYPTED) {
            hasUTD = true
        }
    }

    private fun wantsDateSeparator(event: TimelineEvent, nextEvent: TimelineEvent?): Boolean {
        return if (hasReachedInvite && hasUTD) {
            true
        } else {
            val date = event.root.localDateTime()
            val nextDate = nextEvent?.root?.localDateTime()
            date.toLocalDate() != nextDate?.toLocalDate()
        }
    }

    /**
     * Return true if added
     */
    private fun LoadingItem_.addWhenLoading(direction: Timeline.Direction): Boolean {
        val host = this@TimelineEventController
        val shouldAdd = host.timeline?.hasMoreToLoad(direction) ?: false
        addIf(shouldAdd, host)
        return shouldAdd
    }

    fun searchPositionOfEvent(eventId: String?): Int? = synchronized(modelCache) {
        return adapterPositionMapping[eventId]
    }

    fun getPositionOfReadMarker(): Int? = synchronized(modelCache) {
        return positionOfReadMarker
    }

    private data class CacheItemData(
            val localId: Long,
            val eventId: String?,
            val readReceiptsItem: ReadReceiptsItem? = null,
            val eventModel: EpoxyModel<*>? = null,
            val mergedHeaderModel: BasedMergedItem<*>? = null,
            val formattedDayModel: DaySeparatorItem? = null,
            private val isCacheable: Boolean = true
    ) {
        fun isCacheable(partialState: PartialState): Boolean {
            return isCacheable && partialState.highlightedEventId != eventId
        }
    }
}
