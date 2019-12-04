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
 *
 */

package im.vector.matrix.android.internal.session.sync

import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.internal.crypto.DefaultCryptoService
import im.vector.matrix.android.internal.database.helper.add
import im.vector.matrix.android.internal.database.helper.deleteOnCascade
import im.vector.matrix.android.internal.database.helper.lastStateIndex
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.query.create
import im.vector.matrix.android.internal.database.query.find
import im.vector.matrix.android.internal.database.query.findLastLiveChunkFromRoom
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.room.timeline.PaginationDirection
import im.vector.matrix.android.internal.session.user.UserEntityFactory
import io.realm.Realm
import io.realm.kotlin.createObject
import timber.log.Timber
import javax.inject.Inject

internal class ChunkEntityFactory @Inject constructor(private val cryptoService: DefaultCryptoService) {

    fun create(realm: Realm,
               roomId: String,
               eventList: List<Event>,
               prevToken: String? = null,
               isLimited: Boolean = true,
               isInitialSync: Boolean): ChunkEntity {
        return if (isInitialSync) {
            initialSyncStrategy(realm, roomId, eventList, prevToken)
        } else {
            incrementalSyncStrategy(realm, roomId, eventList, prevToken, isLimited)
        }
    }

    private fun initialSyncStrategy(realm: Realm,
                                    roomId: String,
                                    eventList: List<Event>,
                                    prevToken: String?): ChunkEntity {
        val chunkEntity = ChunkEntity.create(realm, roomId, prevToken = prevToken).apply {
            isLastForward = true
        }
        for (event in eventList) {
            chunkEntity.add(realm, roomId, event, PaginationDirection.FORWARDS)
            // Give info to crypto module
            cryptoService.onLiveEvent(roomId, event)
            UserEntityFactory.createOrNull(event)?.also {
                realm.insertOrUpdate(it)
            }
        }
        return chunkEntity
    }

    private fun incrementalSyncStrategy(realm: Realm,
                                        roomId: String,
                                        eventList: List<Event>,
                                        prevToken: String? = null,
                                        isLimited: Boolean = true): ChunkEntity {
        val lastChunk = ChunkEntity.findLastLiveChunkFromRoom(realm, roomId)
        val stateIndexOffset = lastChunk?.lastStateIndex(PaginationDirection.FORWARDS) ?: 0
        val chunkEntity = if (isLimited || lastChunk == null) {
            ChunkEntity.create(realm, roomId, prevToken = prevToken).apply {
                isLastForward = true
            }
        } else {
            lastChunk
        }
        if (lastChunk != chunkEntity) {
            lastChunk?.deleteOnCascade()
        }
        for (event in eventList) {
            chunkEntity.add(realm, roomId, event, PaginationDirection.FORWARDS, stateIndexOffset)
            // Give info to crypto module
            cryptoService.onLiveEvent(roomId, event)
            // Try to remove local echo
            event.unsignedData?.transactionId?.also {
                val roomEntity = RoomEntity.where(realm, roomId).findFirst()
                val sendingEventEntity = roomEntity?.sendingTimelineEvents?.find(it)
                if (sendingEventEntity != null) {
                    Timber.v("Remove local echo for tx:$it")
                    roomEntity.sendingTimelineEvents.remove(sendingEventEntity)
                } else {
                    Timber.v("Can't find corresponding local echo for tx:$it")
                }
            }
            UserEntityFactory.createOrNull(event)?.also {
                realm.insertOrUpdate(it)
            }
        }
        return chunkEntity
    }

}
