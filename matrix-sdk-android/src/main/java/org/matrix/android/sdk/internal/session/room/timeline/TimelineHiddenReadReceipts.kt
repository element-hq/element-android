/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.timeline

import android.util.SparseArray
import io.realm.OrderedRealmCollectionChangeListener
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.RealmResults
import org.matrix.android.sdk.api.session.room.model.ReadReceipt
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import org.matrix.android.sdk.internal.database.mapper.ReadReceiptsSummaryMapper
import org.matrix.android.sdk.internal.database.model.ReadReceiptsSummaryEntity
import org.matrix.android.sdk.internal.database.model.ReadReceiptsSummaryEntityFields
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntityFields
import org.matrix.android.sdk.internal.database.query.TimelineEventFilter
import org.matrix.android.sdk.internal.database.query.whereInRoom

/**
 * This class is responsible for handling the read receipts for hidden events (check [TimelineSettings] to see filtering).
 * When an hidden event has read receipts, we want to transfer these read receipts on the first older displayed event.
 * It has to be used in [DefaultTimeline] and we should call the [start] and [dispose] methods to properly handle realm subscription.
 */
internal class TimelineHiddenReadReceipts constructor(private val readReceiptsSummaryMapper: ReadReceiptsSummaryMapper,
                                                      private val roomId: String,
                                                      private val settings: TimelineSettings) {

    interface Delegate {
        fun rebuildEvent(eventId: String, readReceipts: List<ReadReceipt>): Boolean
        fun onReadReceiptsUpdated()
    }

    private val correctedReadReceiptsEventByIndex = SparseArray<String>()
    private val correctedReadReceiptsByEvent = HashMap<String, MutableList<ReadReceipt>>()

    private lateinit var hiddenReadReceipts: RealmResults<ReadReceiptsSummaryEntity>
    private lateinit var nonFilteredEvents: RealmResults<TimelineEventEntity>
    private lateinit var filteredEvents: RealmResults<TimelineEventEntity>
    private lateinit var delegate: Delegate

    private val hiddenReadReceiptsListener = OrderedRealmCollectionChangeListener<RealmResults<ReadReceiptsSummaryEntity>> { collection, changeSet ->
        if (!collection.isLoaded || !collection.isValid) {
            return@OrderedRealmCollectionChangeListener
        }
        var hasChange = false
        // Deletion here means we don't have any readReceipts for the given hidden events
        changeSet.deletions.forEach {
            val eventId = correctedReadReceiptsEventByIndex.get(it, "")
            val timelineEvent = filteredEvents.where()
                    .equalTo(TimelineEventEntityFields.EVENT_ID, eventId)
                    .findFirst()

            // We are rebuilding the corresponding event with only his own RR
            val readReceipts = readReceiptsSummaryMapper.map(timelineEvent?.readReceipts)
            hasChange = delegate.rebuildEvent(eventId, readReceipts) || hasChange
        }
        correctedReadReceiptsEventByIndex.clear()
        correctedReadReceiptsByEvent.clear()
        for (index in 0 until hiddenReadReceipts.size) {
            val summary = hiddenReadReceipts[index] ?: continue
            val timelineEvent = summary.timelineEvent?.firstOrNull() ?: continue
            val isLoaded = nonFilteredEvents.where()
                    .equalTo(TimelineEventEntityFields.EVENT_ID, timelineEvent.eventId).findFirst() != null
            val displayIndex = timelineEvent.displayIndex

            if (isLoaded) {
                // Then we are looking for the first displayable event after the hidden one
                val firstDisplayedEvent = filteredEvents.where()
                        .lessThanOrEqualTo(TimelineEventEntityFields.DISPLAY_INDEX, displayIndex)
                        .findFirst()

                // If we find one, we should
                if (firstDisplayedEvent != null) {
                    correctedReadReceiptsEventByIndex.put(index, firstDisplayedEvent.eventId)
                    correctedReadReceiptsByEvent
                            .getOrPut(firstDisplayedEvent.eventId, {
                                ArrayList(readReceiptsSummaryMapper.map(firstDisplayedEvent.readReceipts))
                            })
                            .addAll(readReceiptsSummaryMapper.map(summary))
                }
            }
        }
        if (correctedReadReceiptsByEvent.isNotEmpty()) {
            correctedReadReceiptsByEvent.forEach { (eventId, correctedReadReceipts) ->
                val sortedReadReceipts = correctedReadReceipts.sortedByDescending {
                    it.originServerTs
                }
                hasChange = delegate.rebuildEvent(eventId, sortedReadReceipts) || hasChange
            }
        }
        if (hasChange) {
            delegate.onReadReceiptsUpdated()
        }
    }

    /**
     * Start the realm query subscription. Has to be called on an HandlerThread
     */
    fun start(realm: Realm,
              filteredEvents: RealmResults<TimelineEventEntity>,
              nonFilteredEvents: RealmResults<TimelineEventEntity>,
              delegate: Delegate) {
        this.filteredEvents = filteredEvents
        this.nonFilteredEvents = nonFilteredEvents
        this.delegate = delegate
        // We are looking for read receipts set on hidden events.
        // We only accept those with a timelineEvent (so coming from pagination/sync).
        this.hiddenReadReceipts = ReadReceiptsSummaryEntity.whereInRoom(realm, roomId)
                .isNotEmpty(ReadReceiptsSummaryEntityFields.TIMELINE_EVENT)
                .isNotEmpty(ReadReceiptsSummaryEntityFields.READ_RECEIPTS.`$`)
                .filterReceiptsWithSettings()
                .findAllAsync()
                .also { it.addChangeListener(hiddenReadReceiptsListener) }
    }

    /**
     * Dispose the realm query subscription. Has to be called on an HandlerThread
     */
    fun dispose() {
        if (this::hiddenReadReceipts.isInitialized) {
            this.hiddenReadReceipts.removeAllChangeListeners()
        }
    }

    /**
     * Return the current corrected [ReadReceipt] list for an event, or null
     */
    fun correctedReadReceipts(eventId: String?): List<ReadReceipt>? {
        return correctedReadReceiptsByEvent[eventId]
    }

    /**
     * We are looking for receipts related to filtered events. So, it's the opposite of [DefaultTimeline.filterEventsWithSettings] method.
     */
    private fun RealmQuery<ReadReceiptsSummaryEntity>.filterReceiptsWithSettings(): RealmQuery<ReadReceiptsSummaryEntity> {
        beginGroup()
        var needOr = false
        if (settings.filters.filterTypes) {
            beginGroup()
            // Events: A, B, C, D, (E and S1), F, G, (H and S1), I
            // Allowed: A, B, C, (E and S1), G, (H and S2)
            // Result: D, F, H, I
            settings.filters.allowedTypes.forEachIndexed { index, filter ->
                if (filter.stateKey == null) {
                    notEqualTo("${ReadReceiptsSummaryEntityFields.TIMELINE_EVENT}.${TimelineEventEntityFields.ROOT.TYPE}", filter.eventType)
                } else {
                    beginGroup()
                    notEqualTo("${ReadReceiptsSummaryEntityFields.TIMELINE_EVENT}.${TimelineEventEntityFields.ROOT.TYPE}", filter.eventType)
                    or()
                    notEqualTo("${ReadReceiptsSummaryEntityFields.TIMELINE_EVENT}.${TimelineEventEntityFields.ROOT.STATE_KEY}", filter.stateKey)
                    endGroup()
                }
                if (index != settings.filters.allowedTypes.size - 1) {
                    and()
                }
            }
            endGroup()
            needOr = true
        }
        if (settings.filters.filterUseless) {
            if (needOr) or()
            equalTo("${ReadReceiptsSummaryEntityFields.TIMELINE_EVENT}.${TimelineEventEntityFields.ROOT.IS_USELESS}", true)
            needOr = true
        }
        if (settings.filters.filterEdits) {
            if (needOr) or()
            like("${ReadReceiptsSummaryEntityFields.TIMELINE_EVENT}.${TimelineEventEntityFields.ROOT.CONTENT}", TimelineEventFilter.Content.EDIT)
            or()
            like("${ReadReceiptsSummaryEntityFields.TIMELINE_EVENT}.${TimelineEventEntityFields.ROOT.CONTENT}", TimelineEventFilter.Content.RESPONSE)
            needOr = true
        }
        if (settings.filters.filterRedacted) {
            if (needOr) or()
            like("${ReadReceiptsSummaryEntityFields.TIMELINE_EVENT}.${TimelineEventEntityFields.ROOT.UNSIGNED_DATA}", TimelineEventFilter.Unsigned.REDACTED)
        }
        endGroup()
        return this
    }
}
