/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
     * @param description description of the live for text fallback
     * @return the result of the update of the live
     */
    suspend fun startLiveLocationShare(timeoutMillis: Long, description: String): UpdateLiveLocationShareResult

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
