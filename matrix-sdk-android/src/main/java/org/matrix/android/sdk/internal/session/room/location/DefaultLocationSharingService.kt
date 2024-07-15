/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.room.location

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.zhuinden.monarchy.Monarchy
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.matrix.android.sdk.api.session.room.location.LocationSharingService
import org.matrix.android.sdk.api.session.room.location.UpdateLiveLocationShareResult
import org.matrix.android.sdk.api.session.room.model.livelocation.LiveLocationShareAggregatedSummary
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toOptional
import org.matrix.android.sdk.internal.database.mapper.LiveLocationShareAggregatedSummaryMapper
import org.matrix.android.sdk.internal.database.model.livelocation.LiveLocationShareAggregatedSummaryEntity
import org.matrix.android.sdk.internal.database.query.findRunningLiveInRoom
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase

internal class DefaultLocationSharingService @AssistedInject constructor(
        @Assisted private val roomId: String,
        @SessionDatabase private val monarchy: Monarchy,
        private val sendStaticLocationTask: SendStaticLocationTask,
        private val sendLiveLocationTask: SendLiveLocationTask,
        private val startLiveLocationShareTask: StartLiveLocationShareTask,
        private val stopLiveLocationShareTask: StopLiveLocationShareTask,
        private val checkIfExistingActiveLiveTask: CheckIfExistingActiveLiveTask,
        private val redactLiveLocationShareTask: RedactLiveLocationShareTask,
        private val liveLocationShareAggregatedSummaryMapper: LiveLocationShareAggregatedSummaryMapper,
) : LocationSharingService {

    @AssistedFactory
    interface Factory {
        fun create(roomId: String): DefaultLocationSharingService
    }

    override suspend fun sendStaticLocation(latitude: Double, longitude: Double, uncertainty: Double?, isUserLocation: Boolean): Cancelable {
        val params = SendStaticLocationTask.Params(
                roomId = roomId,
                latitude = latitude,
                longitude = longitude,
                uncertainty = uncertainty,
                isUserLocation = isUserLocation,
        )
        return sendStaticLocationTask.execute(params)
    }

    override suspend fun sendLiveLocation(beaconInfoEventId: String, latitude: Double, longitude: Double, uncertainty: Double?): Cancelable {
        val params = SendLiveLocationTask.Params(
                beaconInfoEventId = beaconInfoEventId,
                roomId = roomId,
                latitude = latitude,
                longitude = longitude,
                uncertainty = uncertainty,
        )
        return sendLiveLocationTask.execute(params)
    }

    override suspend fun startLiveLocationShare(timeoutMillis: Long): UpdateLiveLocationShareResult {
        // Ensure to stop any active live before starting a new one
        if (checkIfExistingActiveLive()) {
            val result = stopLiveLocationShare()
            if (result is UpdateLiveLocationShareResult.Failure) {
                return result
            }
        }
        val params = StartLiveLocationShareTask.Params(
                roomId = roomId,
                timeoutMillis = timeoutMillis,
        )
        return startLiveLocationShareTask.execute(params)
    }

    private suspend fun checkIfExistingActiveLive(): Boolean {
        val params = CheckIfExistingActiveLiveTask.Params(
                roomId = roomId
        )
        return checkIfExistingActiveLiveTask.execute(params)
    }

    override suspend fun stopLiveLocationShare(): UpdateLiveLocationShareResult {
        val params = StopLiveLocationShareTask.Params(
                roomId = roomId,
        )
        return stopLiveLocationShareTask.execute(params)
    }

    override suspend fun redactLiveLocationShare(beaconInfoEventId: String, reason: String?) {
        val params = RedactLiveLocationShareTask.Params(
                roomId = roomId,
                beaconInfoEventId = beaconInfoEventId,
                reason = reason
        )
        return redactLiveLocationShareTask.execute(params)
    }

    override fun getRunningLiveLocationShareSummaries(): LiveData<List<LiveLocationShareAggregatedSummary>> {
        return monarchy.findAllMappedWithChanges(
                { LiveLocationShareAggregatedSummaryEntity.findRunningLiveInRoom(it, roomId = roomId) },
                liveLocationShareAggregatedSummaryMapper
        )
    }

    override fun getLiveLocationShareSummary(beaconInfoEventId: String): LiveData<Optional<LiveLocationShareAggregatedSummary>> {
        return monarchy
                .findAllMappedWithChanges(
                        { LiveLocationShareAggregatedSummaryEntity.where(it, roomId = roomId, eventId = beaconInfoEventId) },
                        liveLocationShareAggregatedSummaryMapper
                )
                .map {
                    it.firstOrNull().toOptional()
                }
    }
}
