/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.aggregation.livelocation

import androidx.work.ExistingWorkPolicy
import io.realm.Realm
import io.realm.RealmList
import org.matrix.android.sdk.api.extensions.orTrue
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageBeaconInfoContent
import org.matrix.android.sdk.api.session.room.model.message.MessageBeaconLocationDataContent
import org.matrix.android.sdk.internal.database.mapper.ContentMapper
import org.matrix.android.sdk.internal.database.model.livelocation.LiveLocationShareAggregatedSummaryEntity
import org.matrix.android.sdk.internal.database.query.findActiveLiveInRoomForUser
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.di.SessionId
import org.matrix.android.sdk.internal.di.WorkManagerProvider
import org.matrix.android.sdk.internal.util.time.Clock
import org.matrix.android.sdk.internal.worker.WorkerParamsFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Aggregates all live location sharing related events in local database.
 */
internal class LiveLocationAggregationProcessor @Inject constructor(
        @SessionId private val sessionId: String,
        private val workManagerProvider: WorkManagerProvider,
        private val clock: Clock,
) {

    /**
     * Handle the content of a beacon info.
     * @return true if it has been processed, false if ignored.
     */
    fun handleBeaconInfo(realm: Realm, event: Event, content: MessageBeaconInfoContent, roomId: String, isLocalEcho: Boolean): Boolean {
        if (event.senderId.isNullOrEmpty() || isLocalEcho) {
            return false
        }

        val isLive = content.isLive.orTrue()
        val targetEventId = if (isLive) {
            event.eventId
        } else {
            // when live is set to false, we use the id of the event that should have been replaced
            event.unsignedData?.replacesState
        }

        if (targetEventId.isNullOrEmpty()) {
            Timber.w("no target event id found for the beacon content")
            return false
        }

        val aggregatedSummary = LiveLocationShareAggregatedSummaryEntity.getOrCreate(
                realm = realm,
                roomId = roomId,
                eventId = targetEventId
        )

        if (!isLive && !event.eventId.isNullOrEmpty()) {
            // in this case, the received event is a new state event related to the previous one
            addRelatedEventId(event.eventId, aggregatedSummary)
        }

        // remote event can stay with isLive == true while the local summary is no more active
        val isActive = aggregatedSummary.isActive.orTrue() && isLive
        val endOfLiveTimestampMillis = content.getBestTimestampMillis()?.let { it + (content.timeout ?: 0) }
        Timber.d("updating summary of id=$targetEventId with isActive=$isActive and endTimestamp=$endOfLiveTimestampMillis")

        aggregatedSummary.startOfLiveTimestampMillis = content.getBestTimestampMillis()
        aggregatedSummary.endOfLiveTimestampMillis = endOfLiveTimestampMillis
        aggregatedSummary.isActive = isActive
        aggregatedSummary.userId = event.senderId

        deactivateAllPreviousBeacons(realm, roomId, event.senderId, targetEventId, content.getBestTimestampMillis() ?: 0)

        if (isActive) {
            scheduleDeactivationAfterTimeout(targetEventId, roomId, endOfLiveTimestampMillis)
        } else {
            cancelDeactivationAfterTimeout(targetEventId, roomId)
        }

        return true
    }

    private fun scheduleDeactivationAfterTimeout(eventId: String, roomId: String, endOfLiveTimestampMillis: Long?) {
        endOfLiveTimestampMillis ?: return

        val workParams = DeactivateLiveLocationShareWorker.Params(sessionId = sessionId, eventId = eventId, roomId = roomId)
        val workData = WorkerParamsFactory.toData(workParams)
        val workName = DeactivateLiveLocationShareWorker.getWorkName(eventId = eventId, roomId = roomId)
        val workDelayMillis = (endOfLiveTimestampMillis - clock.epochMillis()).coerceAtLeast(0)
        Timber.d("scheduling deactivation of $eventId after $workDelayMillis millis")
        val workRequest = workManagerProvider.matrixOneTimeWorkRequestBuilder<DeactivateLiveLocationShareWorker>()
                .setInitialDelay(workDelayMillis, TimeUnit.MILLISECONDS)
                .setInputData(workData)
                .build()

        workManagerProvider.workManager.enqueueUniqueWork(
                workName,
                ExistingWorkPolicy.REPLACE,
                workRequest
        )
    }

    private fun cancelDeactivationAfterTimeout(eventId: String, roomId: String) {
        val workName = DeactivateLiveLocationShareWorker.getWorkName(eventId = eventId, roomId = roomId)
        workManagerProvider.workManager.cancelUniqueWork(workName)
    }

    /**
     * Handle the content of a beacon location data.
     * @return true if it has been processed, false if ignored.
     */
    fun handleBeaconLocationData(
            realm: Realm,
            event: Event,
            content: MessageBeaconLocationDataContent,
            roomId: String,
            relatedEventId: String?,
            isLocalEcho: Boolean
    ): Boolean {
        if (event.senderId.isNullOrEmpty() || isLocalEcho) {
            return false
        }

        if (relatedEventId.isNullOrEmpty()) {
            Timber.w("no related event id found for the live location content")
            return false
        }

        val aggregatedSummary = LiveLocationShareAggregatedSummaryEntity.getOrCreate(
                realm = realm,
                roomId = roomId,
                eventId = relatedEventId
        )

        if (!event.eventId.isNullOrEmpty()) {
            addRelatedEventId(event.eventId, aggregatedSummary)
        }

        val updatedLocationTimestamp = content.getBestTimestampMillis() ?: 0
        val currentLocationTimestamp = ContentMapper
                .map(aggregatedSummary.lastLocationContent)
                .toModel<MessageBeaconLocationDataContent>()
                ?.getBestTimestampMillis()
                ?: 0

        return if (updatedLocationTimestamp.isMoreRecentThan(currentLocationTimestamp)) {
            Timber.d("updating last location of the summary of id=$relatedEventId")
            aggregatedSummary.lastLocationContent = ContentMapper.map(content.toContent())
            true
        } else {
            false
        }
    }

    private fun addRelatedEventId(
            eventId: String,
            aggregatedSummary: LiveLocationShareAggregatedSummaryEntity
    ) {
        Timber.d("adding related event id $eventId to summary of id ${aggregatedSummary.eventId}")
        val updatedEventIds = aggregatedSummary.relatedEventIds.toMutableList().also {
            it.add(eventId)
        }
        aggregatedSummary.relatedEventIds = RealmList(*updatedEventIds.toTypedArray())
    }

    private fun deactivateAllPreviousBeacons(
            realm: Realm,
            roomId: String,
            userId: String,
            currentEventId: String,
            currentEventTimestamp: Long
    ) {
        LiveLocationShareAggregatedSummaryEntity
                .findActiveLiveInRoomForUser(
                        realm = realm,
                        roomId = roomId,
                        userId = userId,
                        ignoredEventId = currentEventId,
                        startOfLiveTimestampThreshold = currentEventTimestamp
                )
                .forEach { it.isActive = false }
    }

    private fun Long.isMoreRecentThan(timestamp: Long) = this > timestamp
}
