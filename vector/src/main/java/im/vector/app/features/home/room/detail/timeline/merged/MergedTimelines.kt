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

package im.vector.app.features.home.room.detail.timeline.merged

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import kotlin.reflect.KMutableProperty0

/**
 * This can be use to merge timeline tiles from 2 different rooms.
 * Be aware it wont work properly with permalink.
 */
class MergedTimelines(
        private val coroutineScope: CoroutineScope,
        private val mainTimeline: Timeline,
        private val secondaryTimelineParams: SecondaryTimelineParams) : Timeline by mainTimeline {

    data class SecondaryTimelineParams(
            val timeline: Timeline,
            val disableReadReceipts: Boolean = true,
            val shouldFilterTypes: Boolean = false,
            val allowedTypes: List<String> = emptyList()
    )

    private var mainIsInit = false
    private var secondaryIsInit = false
    private val secondaryTimeline = secondaryTimelineParams.timeline

    private val listenersMapping = HashMap<Timeline.Listener, List<ListenerInterceptor>>()
    private val mainTimelineEvents = ArrayList<TimelineEvent>()
    private val secondaryTimelineEvents = ArrayList<TimelineEvent>()
    private val positionsMapping = HashMap<String, Int>()
    private val mergedEvents = ArrayList<TimelineEvent>()

    private val processingSemaphore = Semaphore(1)

    private class ListenerInterceptor(
            var timeline: Timeline?,
            private val wrappedListener: Timeline.Listener,
            private val shouldFilterTypes: Boolean,
            private val allowedTypes: List<String>,
            private val onTimelineUpdate: (List<TimelineEvent>) -> Unit
    ) : Timeline.Listener by wrappedListener {

        override fun onTimelineUpdated(snapshot: List<TimelineEvent>) {
            val filteredEvents = if (shouldFilterTypes) {
                snapshot.filter {
                    allowedTypes.contains(it.root.getClearType())
                }
            } else {
                snapshot
            }
            onTimelineUpdate(filteredEvents)
        }
    }

    override fun addListener(listener: Timeline.Listener): Boolean {
        val mainTimelineListener = ListenerInterceptor(
                timeline = mainTimeline,
                wrappedListener = listener,
                shouldFilterTypes = false,
                allowedTypes = emptyList()) {
            processTimelineUpdates(::mainIsInit, mainTimelineEvents, it)
        }
        val secondaryTimelineListener = ListenerInterceptor(
                timeline = secondaryTimeline,
                wrappedListener = listener,
                shouldFilterTypes = secondaryTimelineParams.shouldFilterTypes,
                allowedTypes = secondaryTimelineParams.allowedTypes) {
            processTimelineUpdates(::secondaryIsInit, secondaryTimelineEvents, it)
        }
        listenersMapping[listener] = listOf(mainTimelineListener, secondaryTimelineListener)
        return mainTimeline.addListener(mainTimelineListener) && secondaryTimeline.addListener(secondaryTimelineListener)
    }

    override fun removeListener(listener: Timeline.Listener): Boolean {
        return listenersMapping.remove(listener)?.let {
            it.forEach { listener ->
                listener.timeline?.removeListener(listener)
                listener.timeline = null
            }
            true
        } ?: false
    }

    override fun removeAllListeners() {
        mainTimeline.removeAllListeners()
        secondaryTimeline.removeAllListeners()
    }

    override fun start() {
        mainTimeline.start()
        secondaryTimeline.start()
    }

    override fun dispose() {
        mainTimeline.dispose()
        secondaryTimeline.dispose()
    }

    override fun restartWithEventId(eventId: String?) {
        mainTimeline.restartWithEventId(eventId)
    }

    override fun hasMoreToLoad(direction: Timeline.Direction): Boolean {
        return mainTimeline.hasMoreToLoad(direction) || secondaryTimeline.hasMoreToLoad(direction)
    }

    override fun paginate(direction: Timeline.Direction, count: Int) {
        mainTimeline.paginate(direction, count)
        secondaryTimeline.paginate(direction, count)
    }

    override fun pendingEventCount(): Int {
        return mainTimeline.pendingEventCount() + secondaryTimeline.pendingEventCount()
    }

    override fun failedToDeliverEventCount(): Int {
        return mainTimeline.pendingEventCount() + secondaryTimeline.pendingEventCount()
    }

    override fun getTimelineEventAtIndex(index: Int): TimelineEvent? {
        return mergedEvents.getOrNull(index)
    }

    override fun getIndexOfEvent(eventId: String?): Int? {
        return positionsMapping[eventId]
    }

    override fun getTimelineEventWithId(eventId: String?): TimelineEvent? {
        return positionsMapping[eventId]?.let {
            getTimelineEventAtIndex(it)
        }
    }

    private fun processTimelineUpdates(isInit: KMutableProperty0<Boolean>, eventsRef: MutableList<TimelineEvent>, newData: List<TimelineEvent>) {
        coroutineScope.launch(Dispatchers.Default) {
            processingSemaphore.withPermit {
                isInit.set(true)
                eventsRef.apply {
                    clear()
                    addAll(newData)
                }
                mergeTimeline()
            }
        }
    }

    private suspend fun mergeTimeline() {
        val merged = mutableListOf<TimelineEvent>()
        val mainItr = mainTimelineEvents.toList().listIterator()
        val secondaryItr = secondaryTimelineEvents.toList().listIterator()
        var index = 0
        var correctedSenderInfo: SenderInfo? = mainTimelineEvents.firstOrNull()?.senderInfo
        if (!mainIsInit || !secondaryIsInit) {
            return
        }
        while (merged.size < mainTimelineEvents.size + secondaryTimelineEvents.size) {
            if (mainItr.hasNext()) {
                val nextMain = mainItr.next()
                correctedSenderInfo = nextMain.senderInfo
                if (secondaryItr.hasNext()) {
                    val nextSecondary = secondaryItr.next()
                    if (nextSecondary.root.originServerTs ?: 0 > nextMain.root.originServerTs ?: 0) {
                        positionsMapping[nextSecondary.eventId] = index
                        merged.add(nextSecondary.correctBeforeMerging(correctedSenderInfo))
                        mainItr.previous()
                    } else {
                        positionsMapping[nextMain.eventId] = index
                        merged.add(nextMain)
                        secondaryItr.previous()
                    }
                } else {
                    positionsMapping[nextMain.eventId] = index
                    merged.add(nextMain)
                }
            } else if (secondaryItr.hasNext()) {
                val nextSecondary = secondaryItr.next()
                positionsMapping[nextSecondary.eventId] = index
                merged.add(nextSecondary.correctBeforeMerging(correctedSenderInfo))
            }
            index++
        }
        mergedEvents.apply {
            clear()
            addAll(merged)
        }
        withContext(Dispatchers.Main) {
            listenersMapping.keys.forEach { listener ->
                tryOrNull { listener.onTimelineUpdated(merged) }
            }
        }
    }

    private fun TimelineEvent.correctBeforeMerging(correctedSenderInfo: SenderInfo?): TimelineEvent {
        return copy(
                senderInfo = correctedSenderInfo ?: senderInfo,
                readReceipts = if (secondaryTimelineParams.disableReadReceipts) emptyList() else readReceipts
        )
    }
}
