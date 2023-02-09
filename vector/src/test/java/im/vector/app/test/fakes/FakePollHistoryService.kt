/*
 * Copyright (c) 2023 New Vector Ltd
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
