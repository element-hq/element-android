/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.redaction

import im.vector.app.test.fakes.FakeSession
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.UnsignedData

private const val A_ROOM_ID = "room_id"
private const val AN_EVENT_ID = "event_id"

class CheckIfEventIsRedactedUseCaseTest {

    private val fakeSession = FakeSession()

    private val checkIfEventIsRedactedUseCase = CheckIfEventIsRedactedUseCase(
            session = fakeSession
    )

    @Test
    fun `given a room id and event id for redacted event when calling use case then true is returned`() = runTest {
        val event = Event(
                unsignedData = UnsignedData(age = 123, redactedEvent = Event())
        )
        fakeSession.eventService()
                .givenGetEventReturns(event)

        val result = checkIfEventIsRedactedUseCase.execute(A_ROOM_ID, AN_EVENT_ID)

        result shouldBeEqualTo true
    }

    @Test
    fun `given a room id and event id for non redacted event when calling use case then false is returned`() = runTest {
        val event = Event()
        fakeSession.eventService()
                .givenGetEventReturns(event)

        val result = checkIfEventIsRedactedUseCase.execute(A_ROOM_ID, AN_EVENT_ID)

        result shouldBeEqualTo false
    }
}
