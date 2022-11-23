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

package org.matrix.android.sdk.api.session.room.location

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.session.room.model.livelocation.LiveLocationShareAggregatedSummary
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.api.util.Optional

/**
 * Manage all location sharing related features.
 */
interface LocationSharingService {
    /**
     * Send a static location event to the room.
     * @param latitude required latitude of the location
     * @param longitude required longitude of the location
     * @param uncertainty Accuracy of the location in meters
     * @param isUserLocation indicates whether the location data corresponds to the user location or not (pinned location)
     */
    suspend fun sendStaticLocation(latitude: Double, longitude: Double, uncertainty: Double?, isUserLocation: Boolean): Cancelable

    /**
     * Send a live location event to the room.
     * To get the beacon info event id, [startLiveLocationShare] must be called before sending live location updates.
     * @param beaconInfoEventId event id of the initial beacon info state event
     * @param latitude required latitude of the location
     * @param longitude required longitude of the location
     * @param uncertainty Accuracy of the location in meters
     */
    suspend fun sendLiveLocation(beaconInfoEventId: String, latitude: Double, longitude: Double, uncertainty: Double?): Cancelable

    /**
     * Starts sharing live location in the room.
     * @param timeoutMillis timeout of the live in milliseconds
     * @return the result of the update of the live
     */
    suspend fun startLiveLocationShare(timeoutMillis: Long): UpdateLiveLocationShareResult

    /**
     * Stops sharing live location in the room.
     * @return the result of the update of the live
     */
    suspend fun stopLiveLocationShare(): UpdateLiveLocationShareResult

    /**
     * Redact (delete) the live associated to the given beacon info event id.
     * @param beaconInfoEventId event id of the initial beacon info state event
     * @param reason Optional reason string
     */
    suspend fun redactLiveLocationShare(beaconInfoEventId: String, reason: String?)

    /**
     * Returns a LiveData on the list of current running live location shares.
     */
    fun getRunningLiveLocationShareSummaries(): LiveData<List<LiveLocationShareAggregatedSummary>>

    /**
     * Returns a LiveData on the live location share summary with the given eventId.
     * @param beaconInfoEventId event id of the initial beacon info state event
     */
    fun getLiveLocationShareSummary(beaconInfoEventId: String): LiveData<Optional<LiveLocationShareAggregatedSummary>>
}
