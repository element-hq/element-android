/*
 * Copyright 2019 New Vector Ltd
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

package org.matrix.android.sdk.internal.session.room.read

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.room.model.ReadReceipt
import org.matrix.android.sdk.api.session.room.read.ReadService
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toOptional
import org.matrix.android.sdk.internal.database.mapper.ReadReceiptsSummaryMapper
import org.matrix.android.sdk.internal.database.model.ReadMarkerEntity
import org.matrix.android.sdk.internal.database.model.ReadReceiptEntity
import org.matrix.android.sdk.internal.database.model.ReadReceiptsSummaryEntity
import org.matrix.android.sdk.internal.database.query.isEventRead
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.configureWith

internal class DefaultReadService @AssistedInject constructor(
        @Assisted private val roomId: String,
        @SessionDatabase private val monarchy: Monarchy,
        private val taskExecutor: TaskExecutor,
        private val setReadMarkersTask: SetReadMarkersTask,
        private val readReceiptsSummaryMapper: ReadReceiptsSummaryMapper,
        @UserId private val userId: String
) : ReadService {

    @AssistedInject.Factory
    interface Factory {
        fun create(roomId: String): ReadService
    }

    override fun markAsRead(params: ReadService.MarkAsReadParams, callback: MatrixCallback<Unit>) {
        val taskParams = SetReadMarkersTask.Params(
                roomId = roomId,
                forceReadMarker = params.forceReadMarker(),
                forceReadReceipt = params.forceReadReceipt()
        )
        setReadMarkersTask
                .configureWith(taskParams) {
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
        return isEventRead(monarchy.realmConfiguration, userId, roomId, eventId)
    }

    override fun getReadMarkerLive(): LiveData<Optional<String>> {
        val liveRealmData = monarchy.findAllMappedWithChanges(
                { ReadMarkerEntity.where(it, roomId) },
                { it.eventId }
        )
        return Transformations.map(liveRealmData) {
            it.firstOrNull().toOptional()
        }
    }

    override fun getMyReadReceiptLive(): LiveData<Optional<String>> {
        val liveRealmData = monarchy.findAllMappedWithChanges(
                { ReadReceiptEntity.where(it, roomId = roomId, userId = userId) },
                { it.eventId }
        )
        return Transformations.map(liveRealmData) {
            it.firstOrNull().toOptional()
        }
    }

    override fun getEventReadReceiptsLive(eventId: String): LiveData<List<ReadReceipt>> {
        val liveRealmData = monarchy.findAllMappedWithChanges(
                { ReadReceiptsSummaryEntity.where(it, eventId) },
                { readReceiptsSummaryMapper.map(it) }
        )
        return Transformations.map(liveRealmData) {
            it.firstOrNull().orEmpty()
        }
    }

    private fun ReadService.MarkAsReadParams.forceReadMarker(): Boolean {
        return this == ReadService.MarkAsReadParams.READ_MARKER || this == ReadService.MarkAsReadParams.BOTH
    }

    private fun ReadService.MarkAsReadParams.forceReadReceipt(): Boolean {
        return this == ReadService.MarkAsReadParams.READ_RECEIPT || this == ReadService.MarkAsReadParams.BOTH
    }
}
