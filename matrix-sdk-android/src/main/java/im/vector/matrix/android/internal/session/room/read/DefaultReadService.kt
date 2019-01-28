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
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.room.read.ReadService
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.query.latestEvent
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.fetchManaged

internal class DefaultReadService(private val roomId: String,
                                  private val monarchy: Monarchy,
                                  private val setReadMarkersTask: SetReadMarkersTask,
                                  private val taskExecutor: TaskExecutor) : ReadService {

    override fun markLatestAsRead(callback: MatrixCallback<Void>) {
        val lastEvent = getLatestEvent()
        val params = SetReadMarkersTask.Params(roomId, fullyReadEventId = null, readReceiptEventId = lastEvent?.eventId)
        setReadMarkersTask.configureWith(params).executeBy(taskExecutor)
    }

    override fun markAllAsRead(callback: MatrixCallback<Void>) {
        val lastEvent = getLatestEvent()
        val params = SetReadMarkersTask.Params(roomId, fullyReadEventId = lastEvent?.eventId, readReceiptEventId = null)
        setReadMarkersTask.configureWith(params).executeBy(taskExecutor)
    }

    override fun setReadReceipt(eventId: String, callback: MatrixCallback<Void>) {
        val params = SetReadMarkersTask.Params(roomId, fullyReadEventId = null, readReceiptEventId = eventId)
        setReadMarkersTask.configureWith(params).executeBy(taskExecutor)
    }

    override fun setReadMarkers(fullyReadEventId: String, readReceiptEventId: String?, callback: MatrixCallback<Void>) {
        val params = SetReadMarkersTask.Params(roomId, fullyReadEventId = fullyReadEventId, readReceiptEventId = readReceiptEventId)
        setReadMarkersTask.configureWith(params).executeBy(taskExecutor)
    }

    private fun getLatestEvent(): EventEntity? {
        return monarchy.fetchManaged { EventEntity.latestEvent(it, roomId) }
    }

}