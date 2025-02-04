/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.matrix.android.sdk.api.session.room.poll.LoadedPollsStatus
import org.matrix.android.sdk.api.session.room.poll.PollHistoryService
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

class FakePollHistoryService : PollHistoryService by mockk() {

    fun givenDispose() {
        justRun { dispose() }
    }

    fun verifyDispose() {
        verify { dispose() }
    }

    fun givenGetPollsReturns(events: List<TimelineEvent>): LiveData<List<TimelineEvent>> {
        return MutableLiveData(events).also {
            every { getPollEvents() } returns it
        }
    }

    fun verifyGetPolls() {
        verify { getPollEvents() }
    }

    fun givenGetLoadedPollsStatusReturns(status: LoadedPollsStatus) {
        coEvery { getLoadedPollsStatus() } returns status
    }

    fun verifyGetLoadedPollsStatus() {
        coVerify { getLoadedPollsStatus() }
    }

    fun givenLoadMoreReturns(status: LoadedPollsStatus) {
        coEvery { loadMore() } returns status
    }

    fun verifyLoadMore() {
        coVerify { loadMore() }
    }

    fun givenSyncPollsSuccess() {
        coJustRun { syncPolls() }
    }

    fun verifySyncPolls() {
        coVerify { syncPolls() }
    }
}
