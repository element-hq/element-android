/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.location

import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Test
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.room.model.message.MessageBeaconInfoContent
import org.matrix.android.sdk.test.fakes.FakeGetActiveBeaconInfoForUserTask

private const val A_USER_ID = "user-id"
private const val A_ROOM_ID = "room-id"
private const val A_TIMEOUT = 15_000L
private const val AN_EPOCH = 1655210176L

@ExperimentalCoroutinesApi
class DefaultCheckIfExistingActiveLiveTaskTest {

    private val fakeGetActiveBeaconInfoForUserTask = FakeGetActiveBeaconInfoForUserTask()

    private val defaultCheckIfExistingActiveLiveTask = DefaultCheckIfExistingActiveLiveTask(
            getActiveBeaconInfoForUserTask = fakeGetActiveBeaconInfoForUserTask
    )

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given parameters and existing active live event when calling the task then result is true`() = runTest {
        val params = CheckIfExistingActiveLiveTask.Params(
                roomId = A_ROOM_ID
        )
        val currentStateEvent = Event(
                stateKey = A_USER_ID,
                content = MessageBeaconInfoContent(
                        timeout = A_TIMEOUT,
                        isLive = true,
                        unstableTimestampMillis = AN_EPOCH
                ).toContent()
        )
        fakeGetActiveBeaconInfoForUserTask.givenExecuteReturns(currentStateEvent)

        val result = defaultCheckIfExistingActiveLiveTask.execute(params)

        result shouldBeEqualTo true
        val expectedGetActiveBeaconParams = GetActiveBeaconInfoForUserTask.Params(
                roomId = params.roomId
        )
        fakeGetActiveBeaconInfoForUserTask.verifyExecute(expectedGetActiveBeaconParams)
    }

    @Test
    fun `given parameters and no existing active live event when calling the task then result is false`() = runTest {
        val params = CheckIfExistingActiveLiveTask.Params(
                roomId = A_ROOM_ID
        )
        val inactiveEvents = listOf(
                // no event
                null,
                // null content
                Event(
                        stateKey = A_USER_ID,
                        content = null
                ),
                // inactive live
                Event(
                        stateKey = A_USER_ID,
                        content = MessageBeaconInfoContent(
                                timeout = A_TIMEOUT,
                                isLive = false,
                                unstableTimestampMillis = AN_EPOCH
                        ).toContent()
                )
        )

        inactiveEvents.forEach { currentStateEvent ->
            fakeGetActiveBeaconInfoForUserTask.givenExecuteReturns(currentStateEvent)

            val result = defaultCheckIfExistingActiveLiveTask.execute(params)

            result shouldBeEqualTo false
        }
    }
}
