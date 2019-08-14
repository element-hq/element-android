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

import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.room.read.ReadService
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.ReadReceiptEntity
import im.vector.matrix.android.internal.database.query.find
import im.vector.matrix.android.internal.database.query.findLastLiveChunkFromRoom
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith

internal class DefaultReadService @AssistedInject constructor(@Assisted private val roomId: String,
                                                              private val monarchy: Monarchy,
                                                              private val taskExecutor: TaskExecutor,
                                                              private val setReadMarkersTask: SetReadMarkersTask,
                                                              private val credentials: Credentials
) : ReadService {

    @AssistedInject.Factory
    interface Factory {
        fun create(roomId: String): ReadService
    }

    override fun markAllAsRead(callback: MatrixCallback<Unit>) {
        val params = SetReadMarkersTask.Params(roomId, markAllAsRead = true)
        setReadMarkersTask
                .configureWith(params) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    override fun setReadReceipt(eventId: String, callback: MatrixCallback<Unit>) {
        val params = SetReadMarkersTask.Params(roomId, fullyReadEventId = null, readReceiptEventId = eventId)
        setReadMarkersTask
                .configureWith(params) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    override fun setReadMarker(fullyReadEventId: String, callback: MatrixCallback<Unit>) {
        val params = SetReadMarkersTask.Params(roomId, fullyReadEventId = fullyReadEventId, readReceiptEventId = null)
        setReadMarkersTask
                .configureWith(params) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }


    override fun isEventRead(eventId: String): Boolean {
        var isEventRead = false
        monarchy.doWithRealm {
            val readReceipt = ReadReceiptEntity.where(it, roomId, credentials.userId).findFirst()
                              ?: return@doWithRealm
            val liveChunk = ChunkEntity.findLastLiveChunkFromRoom(it, roomId)
                            ?: return@doWithRealm
            val readReceiptIndex = liveChunk.timelineEvents.find(readReceipt.eventId)?.root?.displayIndex
                                   ?: Int.MIN_VALUE
            val eventToCheckIndex = liveChunk.timelineEvents.find(eventId)?.root?.displayIndex
                                    ?: Int.MAX_VALUE
            isEventRead = eventToCheckIndex <= readReceiptIndex
        }
        return isEventRead
    }

}