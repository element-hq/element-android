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
import im.vector.matrix.android.api.session.room.read.ReadService
import im.vector.matrix.android.api.util.suspendCallback
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.ReadReceiptEntity
import im.vector.matrix.android.internal.database.query.find
import im.vector.matrix.android.internal.database.query.findLastLiveChunkFromRoom
import im.vector.matrix.android.internal.database.query.latestEvent
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.fetchCopied
import javax.inject.Inject

internal class DefaultReadService @Inject constructor(private val roomId: String,
                                                      private val monarchy: Monarchy,
                                                      private val taskExecutor: TaskExecutor,
                                                      private val setReadMarkersTask: SetReadMarkersTask,
                                                      private val credentials: Credentials) : ReadService {

    override suspend fun markAllAsRead() {
        //TODO shouldn't it be latest synced event?
        val latestEvent = getLatestEvent()
        val params = SetReadMarkersTask.Params(roomId, fullyReadEventId = latestEvent?.eventId, readReceiptEventId = latestEvent?.eventId)
        suspendCallback<Unit> {
            setReadMarkersTask.configureWith(params).dispatchTo(it).executeBy(taskExecutor)
        }
    }

    override suspend fun setReadReceipt(eventId: String) {
        val params = SetReadMarkersTask.Params(roomId, fullyReadEventId = null, readReceiptEventId = eventId)
        suspendCallback<Unit> {
            setReadMarkersTask.configureWith(params).dispatchTo(it).executeBy(taskExecutor)
        }
    }

    override suspend fun setReadMarker(fullyReadEventId: String) {
        val params = SetReadMarkersTask.Params(roomId, fullyReadEventId = fullyReadEventId, readReceiptEventId = null)
        suspendCallback<Unit> {
            setReadMarkersTask.configureWith(params).dispatchTo(it).executeBy(taskExecutor)
        }
    }

    private fun getLatestEvent(): EventEntity? {
        return monarchy.fetchCopied { EventEntity.latestEvent(it, roomId) }
    }

    override fun isEventRead(eventId: String): Boolean {
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