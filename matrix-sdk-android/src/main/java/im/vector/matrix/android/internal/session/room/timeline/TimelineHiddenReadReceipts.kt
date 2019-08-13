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

package im.vector.matrix.android.internal.session.room.timeline

import android.util.SparseArray
import im.vector.matrix.android.api.session.room.model.ReadReceipt
import im.vector.matrix.android.api.session.room.timeline.TimelineSettings
import im.vector.matrix.android.internal.database.mapper.ReadReceiptsSummaryMapper
import im.vector.matrix.android.internal.database.model.ReadReceiptsSummaryEntity
import im.vector.matrix.android.internal.database.model.ReadReceiptsSummaryEntityFields
import im.vector.matrix.android.internal.database.model.TimelineEventEntity
import im.vector.matrix.android.internal.database.model.TimelineEventEntityFields
import im.vector.matrix.android.internal.database.query.whereInRoom
import io.realm.OrderedRealmCollectionChangeListener
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.RealmResults

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
    private lateinit var liveEvents: RealmResults<TimelineEventEntity>
    private lateinit var delegate: Delegate

    private val hiddenReadReceiptsListener = OrderedRealmCollectionChangeListener<RealmResults<ReadReceiptsSummaryEntity>> { collection, changeSet ->
        var hasChange = false
        changeSet.deletions.forEach {
            val eventId = correctedReadReceiptsEventByIndex[it]
            val timelineEvent = liveEvents.where().equalTo(TimelineEventEntityFields.EVENT_ID, eventId).findFirst()
            val readReceipts = readReceiptsSummaryMapper.map(timelineEvent?.readReceipts)
            hasChange = hasChange || delegate.rebuildEvent(eventId, readReceipts)
        }
        correctedReadReceiptsEventByIndex.clear()
        correctedReadReceiptsByEvent.clear()
        hiddenReadReceipts.forEachIndexed { index, summary ->
            val timelineEvent = summary?.timelineEvent?.firstOrNull()
            val displayIndex = timelineEvent?.root?.displayIndex
            if (displayIndex != null) {
                val firstDisplayedEvent = liveEvents.where()
                        .lessThanOrEqualTo(TimelineEventEntityFields.ROOT.DISPLAY_INDEX, displayIndex)
                        .findFirst()

                if (firstDisplayedEvent != null) {
                    correctedReadReceiptsEventByIndex.put(index, firstDisplayedEvent.eventId)
                    correctedReadReceiptsByEvent.getOrPut(firstDisplayedEvent.eventId, {
                        readReceiptsSummaryMapper.map(firstDisplayedEvent.readReceipts).toMutableList()
                    }).addAll(
                            readReceiptsSummaryMapper.map(summary)
                    )
                }
            }
        }
        if (correctedReadReceiptsByEvent.isNotEmpty()) {
            correctedReadReceiptsByEvent.forEach { (eventId, correctedReadReceipts) ->
                val sortedReadReceipts = correctedReadReceipts.sortedByDescending {
                    it.originServerTs
                }
                hasChange = hasChange || delegate.rebuildEvent(eventId, sortedReadReceipts)
            }
        }
        if (hasChange) {
            delegate.onReadReceiptsUpdated()
        }
    }


    fun start(realm: Realm, liveEvents: RealmResults<TimelineEventEntity>, delegate: Delegate) {
        this.liveEvents = liveEvents
        this.delegate = delegate
        this.hiddenReadReceipts = ReadReceiptsSummaryEntity.whereInRoom(realm, roomId)
                .isNotEmpty(ReadReceiptsSummaryEntityFields.TIMELINE_EVENT)
                .isNotEmpty(ReadReceiptsSummaryEntityFields.READ_RECEIPTS.`$`)
                .filterReceiptsWithSettings()
                .findAllAsync()
                .also { it.addChangeListener(hiddenReadReceiptsListener) }
    }

    fun dispose() {
        this.hiddenReadReceipts?.removeAllChangeListeners()
    }

    fun correctedReadReceipts(eventId: String?): List<ReadReceipt>? {
        return correctedReadReceiptsByEvent[eventId]
    }


    /**
     * We are looking for receipts related to filtered events. So, it's the opposite of [filterEventsWithSettings] method.
     */
    private fun RealmQuery<ReadReceiptsSummaryEntity>.filterReceiptsWithSettings(): RealmQuery<ReadReceiptsSummaryEntity> {
        beginGroup()
        if (settings.filterTypes) {
            not().`in`("${ReadReceiptsSummaryEntityFields.TIMELINE_EVENT}.${TimelineEventEntityFields.ROOT.TYPE}", settings.allowedTypes.toTypedArray())
        }
        if (settings.filterTypes && settings.filterEdits) {
            or()
        }
        if (settings.filterEdits) {
            like("${ReadReceiptsSummaryEntityFields.TIMELINE_EVENT}.${TimelineEventEntityFields.ROOT.CONTENT}", EDIT_FILTER_LIKE)
        }
        endGroup()
        return this
    }


}