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
import im.vector.app.features.call.webrtc.WebRtcCallManager
import im.vector.app.features.home.room.detail.UnreadState
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.item.CallTileTimelineItem
import im.vector.app.features.home.room.detail.timeline.item.ItemWithEvents
import im.vector.app.features.home.room.detail.timeline.item.TimelineReadMarkerItem_
import im.vector.app.features.settings.VectorPreferences
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import kotlin.reflect.KMutableProperty0

private const val DEFAULT_PREFETCH_THRESHOLD = 30

class TimelineControllerInterceptorHelper(private val positionOfReadMarker: KMutableProperty0<Int?>,
                                          private val adapterPositionMapping: MutableMap<String, Int>,
                                          private val vectorPreferences: VectorPreferences,
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
        val showHiddenEvents = vectorPreferences.shouldShowHiddenEvents()
        var index = 0
        val firstUnreadEventId = (unreadState as? UnreadState.HasUnread)?.firstUnreadEventId
        // Then iterate on models so we have the exact positions in the adapter
        modelsIterator.forEach { epoxyModel ->
            if (epoxyModel is ItemWithEvents) {
                epoxyModel.getEventIds().forEach { eventId ->
                    adapterPositionMapping[eventId] = index
                    if (eventId == firstUnreadEventId) {
                        modelsIterator.addReadMarkerItem(callback)
                        index++
                        positionOfReadMarker.set(index)
                    }
                }
            }
            if (epoxyModel is CallTileTimelineItem) {
                modelsIterator.removeCallItemIfNeeded(epoxyModel, callIds, showHiddenEvents)
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
        // Use next as we still have some process to do before the next iterator loop
        next()
    }

    private fun MutableListIterator<EpoxyModel<*>>.removeCallItemIfNeeded(
            epoxyModel: CallTileTimelineItem,
            callIds: MutableSet<String>,
            showHiddenEvents: Boolean
    ) {
        val callId = epoxyModel.attributes.callId
        // We should remove the call tile if we already have one for this call or
        // if this is an active call tile without an actual call (which can happen with permalink)
        val shouldRemoveCallItem = callIds.contains(callId)
                || (!callManager.getAdvertisedCalls().contains(callId) && epoxyModel.attributes.callStatus.isActive())
        if (shouldRemoveCallItem && !showHiddenEvents) {
            remove()
            val emptyItem = TimelineEmptyItem_()
                    .id(epoxyModel.id())
                    .eventId(epoxyModel.attributes.informationData.eventId)
            add(emptyItem)
        }
        callIds.add(callId)
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
