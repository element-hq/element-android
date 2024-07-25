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

package org.matrix.android.sdk.internal.session.room.read

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.zhuinden.monarchy.Monarchy
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.session.room.model.ReadReceipt
import org.matrix.android.sdk.api.session.room.read.ReadService
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toOptional
import org.matrix.android.sdk.internal.database.mapper.ReadReceiptsSummaryMapper
import org.matrix.android.sdk.internal.database.model.ReadMarkerEntity
import org.matrix.android.sdk.internal.database.model.ReadReceiptEntity
import org.matrix.android.sdk.internal.database.model.ReadReceiptsSummaryEntity
import org.matrix.android.sdk.internal.database.query.forMainTimelineWhere
import org.matrix.android.sdk.internal.database.query.isEventRead
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.homeserver.HomeServerCapabilitiesDataSource

internal class DefaultReadService @AssistedInject constructor(
        @Assisted private val roomId: String,
        @SessionDatabase private val monarchy: Monarchy,
        private val setReadMarkersTask: SetReadMarkersTask,
        private val readReceiptsSummaryMapper: ReadReceiptsSummaryMapper,
        @UserId private val userId: String,
        private val homeServerCapabilitiesDataSource: HomeServerCapabilitiesDataSource,
        private val matrixCoroutineDispatchers: MatrixCoroutineDispatchers,
) : ReadService {

    @AssistedFactory
    interface Factory {
        fun create(roomId: String): DefaultReadService
    }

    override suspend fun markAsRead(params: ReadService.MarkAsReadParams, mainTimeLineOnly: Boolean) {
        val readReceiptThreadId = if (homeServerCapabilitiesDataSource.getHomeServerCapabilities()?.canUseThreadReadReceiptsAndNotifications == true) {
            if (mainTimeLineOnly) ReadService.THREAD_ID_MAIN else null
        } else {
            null
        }
        val taskParams = SetReadMarkersTask.Params(
                roomId = roomId,
                forceReadMarker = params.forceReadMarker(),
                forceReadReceipt = params.forceReadReceipt(),
                readReceiptThreadId = readReceiptThreadId
        )
        setReadMarkersTask.execute(taskParams)
    }

    override suspend fun setReadReceipt(eventId: String, threadId: String) = withContext(matrixCoroutineDispatchers.io) {
        val readReceiptThreadId = if (homeServerCapabilitiesDataSource.getHomeServerCapabilities()?.canUseThreadReadReceiptsAndNotifications == true) {
            threadId
        } else {
            null
        }
        val params = SetReadMarkersTask.Params(roomId, fullyReadEventId = null, readReceiptEventId = eventId, readReceiptThreadId = readReceiptThreadId)
        setReadMarkersTask.execute(params)
    }

    override suspend fun setReadMarker(fullyReadEventId: String) {
        val params = SetReadMarkersTask.Params(roomId, fullyReadEventId = fullyReadEventId, readReceiptEventId = null)
        setReadMarkersTask.execute(params)
    }

    override fun isEventRead(eventId: String): Boolean {
        val shouldCheckIfReadInEventsThread = homeServerCapabilitiesDataSource.getHomeServerCapabilities()?.canUseThreadReadReceiptsAndNotifications == true
        return isEventRead(monarchy.realmConfiguration, userId, roomId, eventId, shouldCheckIfReadInEventsThread)
    }

    override fun getReadMarkerLive(): LiveData<Optional<String>> {
        val liveRealmData = monarchy.findAllMappedWithChanges(
                { ReadMarkerEntity.where(it, roomId) },
                { it.eventId }
        )
        return liveRealmData.map {
            it.firstOrNull().toOptional()
        }
    }

    override fun getMyReadReceiptLive(threadId: String?): LiveData<Optional<String>> {
        val liveRealmData = monarchy.findAllMappedWithChanges(
                { ReadReceiptEntity.where(it, roomId = roomId, userId = userId, threadId = threadId) },
                { it.eventId }
        )
        return liveRealmData.map {
            it.firstOrNull().toOptional()
        }
    }

    override fun getUserReadReceipt(userId: String): String? {
        var eventId: String? = null
        monarchy.doWithRealm {
            eventId = ReadReceiptEntity.forMainTimelineWhere(it, roomId = roomId, userId = userId)
                    .findFirst()
                    ?.eventId
        }

        return eventId
    }

    override fun getEventReadReceiptsLive(eventId: String): LiveData<List<ReadReceipt>> {
        val liveRealmData = monarchy.findAllMappedWithChanges(
                { ReadReceiptsSummaryEntity.where(it, eventId) },
                { readReceiptsSummaryMapper.map(it) }
        )
        return liveRealmData.map {
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
