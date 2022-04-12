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

package org.matrix.android.sdk.internal.session.sync.handler.room

import io.realm.Realm
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.internal.database.model.ReadReceiptEntity
import org.matrix.android.sdk.internal.database.model.ReadReceiptsSummaryEntity
import org.matrix.android.sdk.internal.database.query.createUnmanaged
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.session.sync.RoomSyncEphemeralTemporaryStore
import org.matrix.android.sdk.internal.session.sync.SyncResponsePostTreatmentAggregator
import timber.log.Timber
import javax.inject.Inject

// the receipts dictionaries
// key   : $EventId
// value : dict key $UserId
//              value dict key ts
//                    dict value ts value
internal typealias ReadReceiptContent = Map<String, Map<String, Map<String, Map<String, Double>>>>

private const val READ_KEY = "m.read"
private const val TIMESTAMP_KEY = "ts"

internal class ReadReceiptHandler @Inject constructor(
        private val roomSyncEphemeralTemporaryStore: RoomSyncEphemeralTemporaryStore
) {

    companion object {

        fun createContent(userId: String, eventId: String): ReadReceiptContent {
            return mapOf(
                    eventId to mapOf(
                            READ_KEY to mapOf(
                                    userId to mapOf(
                                            TIMESTAMP_KEY to System.currentTimeMillis().toDouble()
                                    )
                            )
                    )
            )
        }
    }

    fun handle(realm: Realm,
               roomId: String,
               content: ReadReceiptContent?,
               isInitialSync: Boolean,
               aggregator: SyncResponsePostTreatmentAggregator?) {
        content ?: return

        try {
            handleReadReceiptContent(realm, roomId, content, isInitialSync, aggregator)
        } catch (exception: Exception) {
            Timber.e("Fail to handle read receipt for room $roomId")
        }
    }

    private fun handleReadReceiptContent(realm: Realm,
                                         roomId: String,
                                         content: ReadReceiptContent,
                                         isInitialSync: Boolean,
                                         aggregator: SyncResponsePostTreatmentAggregator?) {
        if (isInitialSync) {
            initialSyncStrategy(realm, roomId, content)
        } else {
            incrementalSyncStrategy(realm, roomId, content, aggregator)
        }
    }

    private fun initialSyncStrategy(realm: Realm, roomId: String, content: ReadReceiptContent) {
        val readReceiptSummaries = ArrayList<ReadReceiptsSummaryEntity>()
        for ((eventId, receiptDict) in content) {
            val userIdsDict = receiptDict[READ_KEY] ?: continue
            val readReceiptsSummary = ReadReceiptsSummaryEntity(eventId = eventId, roomId = roomId)

            for ((userId, paramsDict) in userIdsDict) {
                val ts = paramsDict[TIMESTAMP_KEY] ?: 0.0
                val receiptEntity = ReadReceiptEntity.createUnmanaged(roomId, eventId, userId, ts)
                readReceiptsSummary.readReceipts.add(receiptEntity)
            }
            readReceiptSummaries.add(readReceiptsSummary)
        }
        realm.insertOrUpdate(readReceiptSummaries)
    }

    private fun incrementalSyncStrategy(realm: Realm,
                                        roomId: String,
                                        content: ReadReceiptContent,
                                        aggregator: SyncResponsePostTreatmentAggregator?) {
        // First check if we have data from init sync to handle
        getContentFromInitSync(roomId)?.let {
            Timber.w("INIT_SYNC Insert during incremental sync RR for room $roomId")
            doIncrementalSyncStrategy(realm, roomId, it)
            aggregator?.ephemeralFilesToDelete?.add(roomId)
        }

        doIncrementalSyncStrategy(realm, roomId, content)
    }

    private fun doIncrementalSyncStrategy(realm: Realm, roomId: String, content: ReadReceiptContent) {
        for ((eventId, receiptDict) in content) {
            val userIdsDict = receiptDict[READ_KEY] ?: continue
            val readReceiptsSummary = ReadReceiptsSummaryEntity.where(realm, eventId).findFirst()
                    ?: realm.createObject(ReadReceiptsSummaryEntity::class.java, eventId).apply {
                        this.roomId = roomId
                    }

            for ((userId, paramsDict) in userIdsDict) {
                val ts = paramsDict[TIMESTAMP_KEY] ?: 0.0
                val receiptEntity = ReadReceiptEntity.getOrCreate(realm, roomId, userId)
                // ensure new ts is superior to the previous one
                if (ts > receiptEntity.originServerTs) {
                    ReadReceiptsSummaryEntity.where(realm, receiptEntity.eventId).findFirst()?.also {
                        it.readReceipts.remove(receiptEntity)
                    }
                    receiptEntity.eventId = eventId
                    receiptEntity.originServerTs = ts
                    readReceiptsSummary.readReceipts.add(receiptEntity)
                }
            }
        }
    }

    fun getContentFromInitSync(roomId: String): ReadReceiptContent? {
        val dataFromFile = roomSyncEphemeralTemporaryStore.read(roomId)

        dataFromFile ?: return null

        @Suppress("UNCHECKED_CAST")
        val content = dataFromFile
                .events
                ?.firstOrNull { it.type == EventType.RECEIPT }
                ?.content as? ReadReceiptContent

        if (content == null) {
            // We can delete the file now
            roomSyncEphemeralTemporaryStore.delete(roomId)
        }

        return content
    }

    fun onContentFromInitSyncHandled(roomId: String) {
        roomSyncEphemeralTemporaryStore.delete(roomId)
    }
}
