/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location.live

import im.vector.app.test.fakes.FakeActiveSessionHolder
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Test
import org.matrix.android.sdk.api.session.room.location.UpdateLiveLocationShareResult

private const val A_ROOM_ID = "room_id"
private const val AN_EVENT_ID = "event_id"

class StopLiveLocationShareUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()

    private val stopLiveLocationShareUseCase = StopLiveLocationShareUseCase(
            activeSessionHolder = fakeActiveSessionHolder.instance
    )

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given a room id when calling use case then the current live is stopped with success`() = runTest {
        val updateLiveResult = UpdateLiveLocationShareResult.Success(AN_EVENT_ID)
        fakeActiveSessionHolder
                .fakeSession
                .roomService()
                .getRoom(A_ROOM_ID)
                .locationSharingService()
                .givenStopLiveLocationShareReturns(updateLiveResult)

        val result = stopLiveLocationShareUseCase.execute(A_ROOM_ID)

        result shouldBeEqualTo updateLiveResult
    }

    @Test
    fun `given a room id and error during the process when calling use case then result is failure`() = runTest {
        val error = Throwable()
        val updateLiveResult = UpdateLiveLocationShareResult.Failure(error)
        fakeActiveSessionHolder
                .fakeSession
                .roomService()
                .getRoom(A_ROOM_ID)
                .locationSharingService()
                .givenStopLiveLocationShareReturns(updateLiveResult)

        val result = stopLiveLocationShareUseCase.execute(A_ROOM_ID)

        result shouldBeEqualTo updateLiveResult
    }
}
