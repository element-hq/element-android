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

package im.vector.app.features.roomprofile.polls.list.domain

import im.vector.app.features.roomprofile.polls.list.data.RoomPollRepository
import io.mockk.coVerify
import io.mockk.justRun
import io.mockk.mockk
import org.junit.Test

internal class DisposePollHistoryUseCaseTest {

    private val fakeRoomPollRepository = mockk<RoomPollRepository>()

    private val disposePollHistoryUseCase = DisposePollHistoryUseCase(
            roomPollRepository = fakeRoomPollRepository,
    )

    @Test
    fun `given repo when execute then correct method of repo is called`() {
        // Given
        val aRoomId = "roomId"
        justRun { fakeRoomPollRepository.dispose(aRoomId) }

        // When
        disposePollHistoryUseCase.execute(aRoomId)

        // Then
        coVerify { fakeRoomPollRepository.dispose(aRoomId) }
    }
}
