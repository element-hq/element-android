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

import arrow.core.Try
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.ReadReceiptEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.query.find
import im.vector.matrix.android.internal.database.query.findLastLiveChunkFromRoom
import im.vector.matrix.android.internal.database.query.latestEvent
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.session.room.send.LocalEchoEventFactory
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.util.tryTransactionAsync
import timber.log.Timber
import javax.inject.Inject

internal interface SetReadMarkersTask : Task<SetReadMarkersTask.Params, Unit> {

    data class Params(
            val roomId: String,
            val fullyReadEventId: String?,
            val readReceiptEventId: String?
    )
}

private const val READ_MARKER = "m.fully_read"
private const val READ_RECEIPT = "m.read"

internal class DefaultSetReadMarkersTask @Inject constructor(private val roomAPI: RoomAPI,
                                                             private val credentials: Credentials,
                                                             private val monarchy: Monarchy
) : SetReadMarkersTask {

    override suspend fun execute(params: SetReadMarkersTask.Params): Try<Unit> {
        val markers = HashMap<String, String>()
        if (params.fullyReadEventId != null) {
            if (LocalEchoEventFactory.isLocalEchoId(params.fullyReadEventId)) {
                Timber.w("Can't set read marker for local event ${params.fullyReadEventId}")
            } else {
                markers[READ_MARKER] = params.fullyReadEventId
            }
        }
        if (params.readReceiptEventId != null
                && !isEventRead(params.roomId, params.readReceiptEventId)) {

            if (LocalEchoEventFactory.isLocalEchoId(params.readReceiptEventId)) {
                Timber.w("Can't set read marker for local event ${params.fullyReadEventId}")
            } else {
                updateNotificationCountIfNecessary(params.roomId, params.readReceiptEventId)
                markers[READ_RECEIPT] = params.readReceiptEventId
            }
        }
        return if (markers.isEmpty()) {
            Try.just(Unit)
        } else {
            executeRequest {
                apiCall = roomAPI.sendReadMarker(params.roomId, markers)
            }
        }
    }

    private fun updateNotificationCountIfNecessary(roomId: String, eventId: String) {
        monarchy.tryTransactionAsync { realm ->
            val isLatestReceived = EventEntity.latestEvent(realm, roomId)?.eventId == eventId
            if (isLatestReceived) {
                val roomSummary = RoomSummaryEntity.where(realm, roomId).findFirst()
                        ?: return@tryTransactionAsync
                roomSummary.notificationCount = 0
                roomSummary.highlightCount = 0
            }
        }
    }

    private fun isEventRead(roomId: String, eventId: String): Boolean {
        var isEventRead = false
        monarchy.doWithRealm {
            val readReceipt = ReadReceiptEntity.where(it, roomId, credentials.userId).findFirst()
                    ?: return@doWithRealm
            val liveChunk = ChunkEntity.findLastLiveChunkFromRoom(it, roomId)
                    ?: return@doWithRealm
            val readReceiptIndex = liveChunk.events.find(readReceipt.eventId)?.displayIndex
                    ?: Int.MIN_VALUE
            val eventToCheckIndex = liveChunk.events.find(eventId)?.displayIndex
                    ?: Int.MAX_VALUE
            isEventRead = eventToCheckIndex <= readReceiptIndex
        }
        return isEventRead
    }

}