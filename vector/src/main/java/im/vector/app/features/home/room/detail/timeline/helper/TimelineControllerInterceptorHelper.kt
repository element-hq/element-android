/*
 * Copyright (c) 2021 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.home.room.detail.timeline.helper

import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.VisibilityState
import im.vector.app.core.epoxy.LoadingItem_
import im.vector.app.core.epoxy.TimelineEmptyItem_
import im.vector.app.core.resources.UserPreferencesProvider
import im.vector.app.features.call.webrtc.WebRtcCallManager
import im.vector.app.features.home.room.detail.UnreadState
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.item.CallTileTimelineItem
import im.vector.app.features.home.room.detail.timeline.item.DaySeparatorItem
import im.vector.app.features.home.room.detail.timeline.item.ItemWithEvents
import im.vector.app.features.home.room.detail.timeline.item.TimelineReadMarkerItem_
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import kotlin.reflect.KMutableProperty0

private const val DEFAULT_PREFETCH_THRESHOLD = 30

class TimelineControllerInterceptorHelper(private val positionOfReadMarker: KMutableProperty0<Int?>,
                                          private val adapterPositionMapping: MutableMap<String, Int>,
                                          private val userPreferencesProvider: UserPreferencesProvider,
                                          private val callManager: WebRtcCallManager
) {

    private var previousModelsSize = 0

    // Update position when we are building new items
    fun intercept(
            models: MutableList<EpoxyModel<*>>,
            unreadState: UnreadState,
            timeline: Timeline?,
            callback: TimelineEventController.Callback?
    ) {
        positionOfReadMarker.set(null)
        adapterPositionMapping.clear()
        val callIds = mutableSetOf<String>()

        // Add some prefetch loader if needed
        models.addBackwardPrefetchIfNeeded(timeline, callback)
        models.addForwardPrefetchIfNeeded(timeline, callback)

        val modelsIterator = models.listIterator()
        val showHiddenEvents = userPreferencesProvider.shouldShowHiddenEvents()
        var index = 0
        val firstUnreadEventId = (unreadState as? UnreadState.HasUnread)?.firstUnreadEventId
        var atLeastOneVisibleItemSinceLastDaySeparator = false
        var atLeastOneVisibleItemsBeforeReadMarker = false
        var appendReadMarker = false

        // Then iterate on models so we have the exact positions in the adapter
        modelsIterator.forEach { epoxyModel ->
            if (epoxyModel is ItemWithEvents) {
                if (epoxyModel.isVisible()) {
                    atLeastOneVisibleItemSinceLastDaySeparator = true
                    atLeastOneVisibleItemsBeforeReadMarker = true
                }
                epoxyModel.getEventIds().forEach { eventId ->
                    adapterPositionMapping[eventId] = index
                    appendReadMarker = appendReadMarker
                            || (epoxyModel.canAppendReadMarker() && eventId == firstUnreadEventId && atLeastOneVisibleItemsBeforeReadMarker)
                }
            }
            if (epoxyModel is DaySeparatorItem) {
                if (!atLeastOneVisibleItemSinceLastDaySeparator) {
                    modelsIterator.remove()
                    return@forEach
                }
                atLeastOneVisibleItemSinceLastDaySeparator = false
            } else if (epoxyModel is CallTileTimelineItem) {
                val hasBeenRemoved = modelsIterator.removeCallItemIfNeeded(epoxyModel, callIds, showHiddenEvents)
                if (!hasBeenRemoved) {
                    atLeastOneVisibleItemSinceLastDaySeparator = true
                }
            }
            if (appendReadMarker) {
                modelsIterator.addReadMarkerItem(callback)
                index++
                positionOfReadMarker.set(index)
                appendReadMarker = false
            }
            index++
        }
        previousModelsSize = models.size
    }

    private fun MutableListIterator<EpoxyModel<*>>.addReadMarkerItem(callback: TimelineEventController.Callback?) {
        val readMarker = TimelineReadMarkerItem_()
                .also {
                    it.id("read_marker")
                    it.setOnVisibilityStateChanged(ReadMarkerVisibilityStateChangedListener(callback))
                }
        add(readMarker)
    }

    private fun MutableListIterator<EpoxyModel<*>>.removeCallItemIfNeeded(
            epoxyModel: CallTileTimelineItem,
            callIds: MutableSet<String>,
            showHiddenEvents: Boolean
    ): Boolean {
        val callId = epoxyModel.attributes.callId
        // We should remove the call tile if we already have one for this call or
        // if this is an active call tile without an actual call (which can happen with permalink)
        val shouldRemoveCallItem = callIds.contains(callId)
                || (!callManager.getAdvertisedCalls().contains(callId) && epoxyModel.attributes.callStatus.isActive())
        val removed = shouldRemoveCallItem && !showHiddenEvents
        if (removed) {
            remove()
            val emptyItem = TimelineEmptyItem_()
                    .id(epoxyModel.id())
                    .eventId(epoxyModel.attributes.informationData.eventId)
                    .notBlank(false)
            add(emptyItem)
        }
        callIds.add(callId)
        return removed
    }

    private fun MutableList<EpoxyModel<*>>.addBackwardPrefetchIfNeeded(timeline: Timeline?, callback: TimelineEventController.Callback?) {
        val shouldAddBackwardPrefetch = timeline?.hasMoreToLoad(Timeline.Direction.BACKWARDS) ?: false
        if (shouldAddBackwardPrefetch) {
            val indexOfPrefetchBackward = (previousModelsSize - 1)
                    .coerceAtMost(size - DEFAULT_PREFETCH_THRESHOLD)
                    .coerceAtLeast(0)

            val loadingItem = LoadingItem_()
                    .id("prefetch_backward_loading${System.currentTimeMillis()}")
                    .showLoader(false)
                    .setVisibilityStateChangedListener(Timeline.Direction.BACKWARDS, callback)

            add(indexOfPrefetchBackward, loadingItem)
        }
    }

    private fun MutableList<EpoxyModel<*>>.addForwardPrefetchIfNeeded(timeline: Timeline?, callback: TimelineEventController.Callback?) {
        val shouldAddForwardPrefetch = timeline?.hasMoreToLoad(Timeline.Direction.FORWARDS) ?: false
        if (shouldAddForwardPrefetch) {
            val indexOfPrefetchForward = DEFAULT_PREFETCH_THRESHOLD.coerceAtMost(size - 1)
            val loadingItem = LoadingItem_()
                    .id("prefetch_forward_loading${System.currentTimeMillis()}")
                    .showLoader(false)
                    .setVisibilityStateChangedListener(Timeline.Direction.FORWARDS, callback)
            add(indexOfPrefetchForward, loadingItem)
        }
    }

    private fun LoadingItem_.setVisibilityStateChangedListener(
            direction: Timeline.Direction,
            callback: TimelineEventController.Callback?
    ): LoadingItem_ {
        return onVisibilityStateChanged { _, _, visibilityState ->
            if (visibilityState == VisibilityState.VISIBLE) {
                callback?.onLoadMore(direction)
            }
        }
    }
}
