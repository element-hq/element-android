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

package im.vector.app.features.location.live

import im.vector.app.test.fakes.FakeLocationSharingServiceConnection
import im.vector.app.test.fakes.FakeSession
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Test
import org.matrix.android.sdk.api.session.room.location.UpdateLiveLocationShareResult

private const val A_ROOM_ID = "room_id"
private const val AN_EVENT_ID = "event_id"

class StopLiveLocationShareUseCaseTest {

    private val fakeLocationSharingServiceConnection = FakeLocationSharingServiceConnection()
    private val fakeSession = FakeSession()

    private val stopLiveLocationShareUseCase = StopLiveLocationShareUseCase(
            locationSharingServiceConnection = fakeLocationSharingServiceConnection.instance,
            session = fakeSession
    )

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given a room id when calling use case then the current live is stopped with success`() = runTest {
        fakeLocationSharingServiceConnection.givenStopLiveLocationSharing()

        val result = stopLiveLocationShareUseCase.execute(A_ROOM_ID)

        result shouldBeEqualTo UpdateLiveLocationShareResult.Success(AN_EVENT_ID)
        fakeLocationSharingServiceConnection.verifyStopLiveLocationSharing(A_ROOM_ID)
    }

    @Test
    fun `given a room id and error during the process when calling use case then result is failure`() = runTest {

    }
}
