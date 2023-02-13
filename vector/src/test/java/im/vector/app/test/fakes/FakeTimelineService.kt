/*
 * Copyright (c) 2021 New Vector Ltd
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
import io.mockk.every
import io.mockk.mockk
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineService
import org.matrix.android.sdk.api.util.Optional

class FakeTimelineService : TimelineService by mockk() {

    fun givenTimelineEventReturns(eventId: String, event: TimelineEvent?) {
        every { getTimelineEvent(eventId) } returns event
    }

    fun givenTimelineEventLiveReturns(
            eventId: String,
            event: TimelineEvent?
    ): LiveData<Optional<TimelineEvent>> {
        return MutableLiveData(Optional(event)).also {
            every { getTimelineEventLive(eventId) } returns it
        }
    }
}
