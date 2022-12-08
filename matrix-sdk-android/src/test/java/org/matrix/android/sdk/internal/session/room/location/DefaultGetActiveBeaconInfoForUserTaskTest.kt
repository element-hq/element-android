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

import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Test
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.room.model.message.MessageBeaconInfoContent
import org.matrix.android.sdk.test.fakes.FakeStateEventDataSource

private const val A_USER_ID = "user-id"
private const val A_ROOM_ID = "room-id"
private const val A_TIMEOUT = 15_000L
private const val AN_EPOCH = 1655210176L

@ExperimentalCoroutinesApi
class DefaultGetActiveBeaconInfoForUserTaskTest {

    private val fakeStateEventDataSource = FakeStateEventDataSource()

    private val defaultGetActiveBeaconInfoForUserTask = DefaultGetActiveBeaconInfoForUserTask(
            userId = A_USER_ID,
            stateEventDataSource = fakeStateEventDataSource.instance
    )

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given parameters and no error when calling the task then result is computed`() = runTest {
        val currentStateEvent = Event(
                stateKey = A_USER_ID,
                content = MessageBeaconInfoContent(
                        timeout = A_TIMEOUT,
                        isLive = true,
                        unstableTimestampMillis = AN_EPOCH
                ).toContent()
        )
        fakeStateEventDataSource.givenGetStateEventReturns(currentStateEvent)
        val params = GetActiveBeaconInfoForUserTask.Params(
                roomId = A_ROOM_ID
        )

        val result = defaultGetActiveBeaconInfoForUserTask.execute(params)

        result shouldBeEqualTo currentStateEvent
        fakeStateEventDataSource.verifyGetStateEvent(
                roomId = params.roomId,
                eventType = EventType.STATE_ROOM_BEACON_INFO.unstable,
                stateKey = QueryStringValue.Equals(A_USER_ID)
        )
    }
}
