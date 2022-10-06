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
import androidx.lifecycle.asLiveData
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.map
import org.matrix.android.sdk.api.session.room.model.ReadReceipt
import org.matrix.android.sdk.api.session.room.read.ReadService
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.database.RealmInstance
import org.matrix.android.sdk.internal.database.mapper.ReadReceiptsSummaryMapper
import org.matrix.android.sdk.internal.database.model.ReadMarkerEntity
import org.matrix.android.sdk.internal.database.model.ReadReceiptEntity
import org.matrix.android.sdk.internal.database.model.ReadReceiptsSummaryEntity
import org.matrix.android.sdk.internal.database.query.isEventRead
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.util.mapOptional

internal class DefaultReadService @AssistedInject constructor(
        @Assisted private val roomId: String,
        @SessionDatabase private val realmInstance: RealmInstance,
        private val setReadMarkersTask: SetReadMarkersTask,
        private val readReceiptsSummaryMapper: ReadReceiptsSummaryMapper,
        @UserId private val userId: String
) : ReadService {

    @AssistedFactory
    interface Factory {
        fun create(roomId: String): DefaultReadService
    }

    override suspend fun markAsRead(params: ReadService.MarkAsReadParams) {
        val taskParams = SetReadMarkersTask.Params(
                roomId = roomId,
                forceReadMarker = params.forceReadMarker(),
                forceReadReceipt = params.forceReadReceipt()
        )
        setReadMarkersTask.execute(taskParams)
    }

    override suspend fun setReadReceipt(eventId: String) {
        val params = SetReadMarkersTask.Params(roomId, fullyReadEventId = null, readReceiptEventId = eventId)
        setReadMarkersTask.execute(params)
    }

    override suspend fun setReadMarker(fullyReadEventId: String) {
        val params = SetReadMarkersTask.Params(roomId, fullyReadEventId = fullyReadEventId, readReceiptEventId = null)
        setReadMarkersTask.execute(params)
    }

    override fun isEventRead(eventId: String): Boolean {
        val realm = realmInstance.getBlockingRealm()
        return isEventRead(realm, userId, roomId, eventId)
    }

    override fun getReadMarkerLive(): LiveData<Optional<String>> {
        return realmInstance.queryFirst {
            ReadMarkerEntity.where(it, roomId).first()
        }
                .mapOptional { it.eventId }
                .asLiveData()
    }

    override fun getMyReadReceiptLive(): LiveData<Optional<String>> {
        return realmInstance.queryFirst {
            ReadReceiptEntity.where(it, roomId = roomId, userId = userId).first()
        }
                .mapOptional { it.eventId }
                .asLiveData()
    }

    override fun getUserReadReceipt(userId: String): String? {
        val realm = realmInstance.getBlockingRealm()
        return ReadReceiptEntity.where(realm, roomId = roomId, userId = userId)
                .first()
                .find()
                ?.eventId
    }

    override fun getEventReadReceiptsLive(eventId: String): LiveData<List<ReadReceipt>> {
        return realmInstance.queryFirst {
            ReadReceiptsSummaryEntity.where(it, eventId)
        }.map {
            readReceiptsSummaryMapper.map(it.getOrNull())
        }.asLiveData()
    }

    private fun ReadService.MarkAsReadParams.forceReadMarker(): Boolean {
        return this == ReadService.MarkAsReadParams.READ_MARKER || this == ReadService.MarkAsReadParams.BOTH
    }

    private fun ReadService.MarkAsReadParams.forceReadReceipt(): Boolean {
        return this == ReadService.MarkAsReadParams.READ_RECEIPT || this == ReadService.MarkAsReadParams.BOTH
    }
}
