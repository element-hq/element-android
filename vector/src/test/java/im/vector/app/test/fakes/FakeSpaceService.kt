/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.mockk.every
import io.mockk.mockk
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.space.SpaceService

class FakeSpaceService : SpaceService by mockk() {

    fun givenGetSpaceSummariesLiveReturns(roomSummaries: List<RoomSummary>): LiveData<List<RoomSummary>> {
        return MutableLiveData(roomSummaries).also {
            every { getSpaceSummariesLive(any()) } returns it
        }
    }

    fun givenGetSpaceSummariesReturns(roomSummaries: List<RoomSummary>) {
        every { getSpaceSummaries(any()) } returns roomSummaries
    }
}
