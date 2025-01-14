/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.matrix.android.sdk.api.session.room.location.LocationSharingService
import org.matrix.android.sdk.api.session.room.location.UpdateLiveLocationShareResult
import org.matrix.android.sdk.api.session.room.model.livelocation.LiveLocationShareAggregatedSummary
import org.matrix.android.sdk.api.util.Optional

class FakeLocationSharingService : LocationSharingService by mockk() {

    fun givenRunningLiveLocationShareSummariesReturns(
            summaries: List<LiveLocationShareAggregatedSummary>
    ): LiveData<List<LiveLocationShareAggregatedSummary>> {
        return MutableLiveData(summaries).also {
            every { getRunningLiveLocationShareSummaries() } returns it
        }
    }

    fun givenLiveLocationShareSummaryReturns(
            eventId: String,
            summary: LiveLocationShareAggregatedSummary?
    ): LiveData<Optional<LiveLocationShareAggregatedSummary>> {
        return MutableLiveData(Optional(summary)).also {
            every { getLiveLocationShareSummary(eventId) } returns it
        }
    }

    fun givenStopLiveLocationShareReturns(result: UpdateLiveLocationShareResult) {
        coEvery { stopLiveLocationShare() } returns result
    }

    fun givenRedactLiveLocationShare(beaconInfoEventId: String, reason: String?) {
        coEvery { redactLiveLocationShare(beaconInfoEventId, reason) } just runs
    }

    /**
     * @param inverse when true it will check redaction of the live did not happen
     * @param beaconInfoEventId event id of the beacon related to the live
     * @param reason reason explaining the redaction
     */
    fun verifyRedactLiveLocationShare(inverse: Boolean = false, beaconInfoEventId: String, reason: String?) {
        coVerify(inverse = inverse) { redactLiveLocationShare(beaconInfoEventId, reason) }
    }
}
