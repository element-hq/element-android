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

import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Test
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.test.fakes.FakeEventSenderProcessor
import org.matrix.android.sdk.test.fakes.FakeLocalEchoEventFactory

private const val A_ROOM_ID = "room_id"
private const val A_LATITUDE = 1.4
private const val A_LONGITUDE = 44.0
private const val AN_UNCERTAINTY = 5.0

@ExperimentalCoroutinesApi
internal class DefaultSendStaticLocationTaskTest {

    private val fakeLocalEchoEventFactory = FakeLocalEchoEventFactory()
    private val fakeEventSenderProcessor = FakeEventSenderProcessor()

    private val defaultSendStaticLocationTask = DefaultSendStaticLocationTask(
            localEchoEventFactory = fakeLocalEchoEventFactory.instance,
            eventSenderProcessor = fakeEventSenderProcessor
    )

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given parameters when calling the task then it is correctly executed`() = runTest {
        val params = SendStaticLocationTask.Params(
                roomId = A_ROOM_ID,
                latitude = A_LATITUDE,
                longitude = A_LONGITUDE,
                uncertainty = AN_UNCERTAINTY,
                isUserLocation = true
        )
        val event = fakeLocalEchoEventFactory.givenCreateStaticLocationEvent(
                withLocalEcho = true
        )
        val cancelable = mockk<Cancelable>()
        fakeEventSenderProcessor.givenPostEventReturns(event, cancelable)

        val result = defaultSendStaticLocationTask.execute(params)

        result shouldBeEqualTo cancelable
        fakeLocalEchoEventFactory.verifyCreateStaticLocationEvent(
                roomId = params.roomId,
                latitude = params.latitude,
                longitude = params.longitude,
                uncertainty = params.uncertainty,
                isUserLocation = params.isUserLocation
        )
        fakeLocalEchoEventFactory.verifyCreateLocalEcho(event)
    }
}
