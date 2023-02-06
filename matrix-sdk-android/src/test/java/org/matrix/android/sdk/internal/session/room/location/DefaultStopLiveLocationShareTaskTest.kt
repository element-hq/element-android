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
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.After
import org.junit.Test
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.room.location.UpdateLiveLocationShareResult
import org.matrix.android.sdk.api.session.room.model.message.MessageBeaconInfoContent
import org.matrix.android.sdk.internal.session.room.state.SendStateTask
import org.matrix.android.sdk.test.fakes.FakeGetActiveBeaconInfoForUserTask
import org.matrix.android.sdk.test.fakes.FakeSendStateTask

private const val A_USER_ID = "user-id"
private const val A_ROOM_ID = "room-id"
private const val AN_EVENT_ID = "event-id"
private const val A_TIMEOUT = 15_000L
private const val AN_EPOCH = 1655210176L

@ExperimentalCoroutinesApi
class DefaultStopLiveLocationShareTaskTest {

    private val fakeSendStateTask = FakeSendStateTask()
    private val fakeGetActiveBeaconInfoForUserTask = FakeGetActiveBeaconInfoForUserTask()

    private val defaultStopLiveLocationShareTask = DefaultStopLiveLocationShareTask(
            sendStateTask = fakeSendStateTask,
            getActiveBeaconInfoForUserTask = fakeGetActiveBeaconInfoForUserTask
    )

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given parameters and no error when calling the task then result is success`() = runTest {
        val params = StopLiveLocationShareTask.Params(roomId = A_ROOM_ID)
        val currentStateEvent = Event(
                stateKey = A_USER_ID,
                content = MessageBeaconInfoContent(
                        timeout = A_TIMEOUT,
                        isLive = true,
                        unstableTimestampMillis = AN_EPOCH
                ).toContent()
        )
        fakeGetActiveBeaconInfoForUserTask.givenExecuteReturns(currentStateEvent)
        fakeSendStateTask.givenExecuteRetryReturns(AN_EVENT_ID)

        val result = defaultStopLiveLocationShareTask.execute(params)

        result shouldBeEqualTo UpdateLiveLocationShareResult.Success(AN_EVENT_ID)
        val expectedBeaconContent = MessageBeaconInfoContent(
                timeout = A_TIMEOUT,
                isLive = false,
                unstableTimestampMillis = AN_EPOCH
        ).toContent()
        val expectedSendParams = SendStateTask.Params(
                roomId = params.roomId,
                stateKey = A_USER_ID,
                eventType = EventType.STATE_ROOM_BEACON_INFO.unstable,
                body = expectedBeaconContent
        )
        fakeSendStateTask.verifyExecuteRetry(
                params = expectedSendParams,
                remainingRetry = 3
        )
        val expectedGetBeaconParams = GetActiveBeaconInfoForUserTask.Params(
                roomId = params.roomId
        )
        fakeGetActiveBeaconInfoForUserTask.verifyExecute(
                expectedGetBeaconParams
        )
    }

    @Test
    fun `given parameters and an incorrect current state event when calling the task then result is failure`() = runTest {
        val incorrectCurrentStateEvents = listOf(
                // no event
                null,
                // no stateKey
                Event(
                        stateKey = null,
                        content = MessageBeaconInfoContent(
                                timeout = A_TIMEOUT,
                                isLive = true,
                                unstableTimestampMillis = AN_EPOCH
                        ).toContent()
                ),
                // null content
                Event(
                        stateKey = A_USER_ID,
                        content = null
                )
        )

        incorrectCurrentStateEvents.forEach { currentStateEvent ->
            fakeGetActiveBeaconInfoForUserTask.givenExecuteReturns(currentStateEvent)
            fakeSendStateTask.givenExecuteRetryReturns(AN_EVENT_ID)
            val params = StopLiveLocationShareTask.Params(roomId = A_ROOM_ID)

            val result = defaultStopLiveLocationShareTask.execute(params)

            result shouldBeInstanceOf UpdateLiveLocationShareResult.Failure::class
        }
    }

    @Test
    fun `given parameters and an empty returned event id when calling the task then result is failure`() = runTest {
        val params = StopLiveLocationShareTask.Params(roomId = A_ROOM_ID)
        val currentStateEvent = Event(
                stateKey = A_USER_ID,
                content = MessageBeaconInfoContent(
                        timeout = A_TIMEOUT,
                        isLive = true,
                        unstableTimestampMillis = AN_EPOCH
                ).toContent()
        )
        fakeGetActiveBeaconInfoForUserTask.givenExecuteReturns(currentStateEvent)
        fakeSendStateTask.givenExecuteRetryReturns("")

        val result = defaultStopLiveLocationShareTask.execute(params)

        result shouldBeInstanceOf UpdateLiveLocationShareResult.Failure::class
    }

    @Test
    fun `given parameters and error during event sending when calling the task then result is failure`() = runTest {
        val params = StopLiveLocationShareTask.Params(roomId = A_ROOM_ID)
        val currentStateEvent = Event(
                stateKey = A_USER_ID,
                content = MessageBeaconInfoContent(
                        timeout = A_TIMEOUT,
                        isLive = true,
                        unstableTimestampMillis = AN_EPOCH
                ).toContent()
        )
        fakeGetActiveBeaconInfoForUserTask.givenExecuteReturns(currentStateEvent)
        val error = Throwable()
        fakeSendStateTask.givenExecuteRetryThrows(error)

        val result = defaultStopLiveLocationShareTask.execute(params)

        result shouldBeEqualTo UpdateLiveLocationShareResult.Failure(error)
    }
}
