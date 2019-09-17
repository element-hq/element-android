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

package im.vector.matrix.android.internal.session.room.read

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.room.read.FullyReadContent
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.ReadMarkerEntity
import im.vector.matrix.android.internal.database.model.ReadReceiptEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.model.TimelineEventEntity
import im.vector.matrix.android.internal.database.query.find
import im.vector.matrix.android.internal.database.query.findLastLiveChunkFromRoom
import im.vector.matrix.android.internal.database.query.latestEvent
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.session.room.send.LocalEchoEventFactory
import im.vector.matrix.android.internal.session.sync.RoomFullyReadHandler
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.util.awaitTransaction
import io.realm.Realm
import timber.log.Timber
import javax.inject.Inject

internal interface SetReadMarkersTask : Task<SetReadMarkersTask.Params, Unit> {

    data class Params(
            val roomId: String,
            val markAllAsRead: Boolean = false,
            val fullyReadEventId: String? = null,
            val readReceiptEventId: String? = null
    )
}

private const val READ_MARKER = "m.fully_read"
private const val READ_RECEIPT = "m.read"

internal class DefaultSetReadMarkersTask @Inject constructor(private val roomAPI: RoomAPI,
                                                             private val credentials: Credentials,
                                                             private val monarchy: Monarchy,
                                                             private val roomFullyReadHandler: RoomFullyReadHandler
) : SetReadMarkersTask {

    override suspend fun execute(params: SetReadMarkersTask.Params) {
        val markers = HashMap<String, String>()
        val fullyReadEventId: String?
        val readReceiptEventId: String?

        Timber.v("Execute set read marker with params: $params")
        if (params.markAllAsRead) {
            val latestSyncedEventId = Realm.getInstance(monarchy.realmConfiguration).use { realm ->
                TimelineEventEntity.latestEvent(realm, roomId = params.roomId, includesSending = false)?.eventId
            }
            fullyReadEventId = latestSyncedEventId
            readReceiptEventId = latestSyncedEventId
        } else {
            fullyReadEventId = params.fullyReadEventId
            readReceiptEventId = params.readReceiptEventId
        }

        if (fullyReadEventId != null && isReadMarkerMoreRecent(params.roomId, fullyReadEventId)) {
            if (LocalEchoEventFactory.isLocalEchoId(fullyReadEventId)) {
                Timber.w("Can't set read marker for local event ${params.fullyReadEventId}")
            } else {
                updateReadMarker(params.roomId, fullyReadEventId)
                markers[READ_MARKER] = fullyReadEventId
            }
        }
        if (readReceiptEventId != null
            && !isEventRead(params.roomId, readReceiptEventId)) {
            if (LocalEchoEventFactory.isLocalEchoId(readReceiptEventId)) {
                Timber.w("Can't set read receipt for local event ${params.fullyReadEventId}")
            } else {
                updateNotificationCountIfNecessary(params.roomId, readReceiptEventId)
                markers[READ_RECEIPT] = readReceiptEventId
            }
        }
        if (markers.isEmpty()) {
            return
        }
        executeRequest<Unit> {
            apiCall = roomAPI.sendReadMarker(params.roomId, markers)
        }
    }


    private fun isReadMarkerMoreRecent(roomId: String, fullyReadEventId: String): Boolean {
        return Realm.getInstance(monarchy.realmConfiguration).use { realm ->
            val readMarkerEntity = ReadMarkerEntity.where(realm, roomId = roomId).findFirst()
            val readMarkerEvent = readMarkerEntity?.timelineEvent?.firstOrNull()
            val eventToCheck = TimelineEventEntity.where(realm, eventId = fullyReadEventId).findFirst()
            val readReceiptIndex = readMarkerEvent?.root?.displayIndex ?: Int.MAX_VALUE
            val eventToCheckIndex = eventToCheck?.root?.displayIndex ?: Int.MIN_VALUE
            eventToCheckIndex > readReceiptIndex
        }
    }

    private suspend fun updateReadMarker(roomId: String, eventId: String) {
        monarchy.awaitTransaction { realm ->
            roomFullyReadHandler.handle(realm, roomId, FullyReadContent(eventId))
        }
    }

    private suspend fun updateNotificationCountIfNecessary(roomId: String, eventId: String) {
        monarchy.awaitTransaction { realm ->
            val isLatestReceived = TimelineEventEntity.latestEvent(realm, roomId = roomId, includesSending = false)?.eventId == eventId
            if (isLatestReceived) {
                val roomSummary = RoomSummaryEntity.where(realm, roomId).findFirst()
                                  ?: return@awaitTransaction
                roomSummary.notificationCount = 0
                roomSummary.highlightCount = 0
            }
        }
    }

    private fun isEventRead(roomId: String, eventId: String): Boolean {
        return Realm.getInstance(monarchy.realmConfiguration).use { realm ->
            val readReceipt = ReadReceiptEntity.where(realm, roomId, credentials.userId).findFirst()
                              ?: return false
            val liveChunk = ChunkEntity.findLastLiveChunkFromRoom(realm, roomId)
                            ?: return false
            val readReceiptIndex = liveChunk.timelineEvents.find(readReceipt.eventId)?.root?.displayIndex
                                   ?: Int.MIN_VALUE
            val eventToCheckIndex = liveChunk.timelineEvents.find(eventId)?.root?.displayIndex
                                    ?: Int.MAX_VALUE
            eventToCheckIndex <= readReceiptIndex
        }
    }

}