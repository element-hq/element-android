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

import im.vector.app.features.roomprofile.polls.list.data.LoadedPollsStatus
import im.vector.app.features.roomprofile.polls.list.data.RoomPollRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class GetLoadedPollsStatusUseCaseTest {

    private val fakeRoomPollRepository = mockk<RoomPollRepository>()

    private val getLoadedPollsStatusUseCase = GetLoadedPollsStatusUseCase(
            roomPollRepository = fakeRoomPollRepository,
    )

    @Test
    fun `given repo when execute then correct method of repo is called`() {
        // Given
        val aRoomId = "roomId"
        val expectedStatus = LoadedPollsStatus(
                canLoadMore = true,
                nbLoadedDays = 10,
        )
        every { fakeRoomPollRepository.getLoadedPollsStatus(aRoomId) } returns expectedStatus

        // When
        val status = getLoadedPollsStatusUseCase.execute(aRoomId)

        // Then
        status shouldBeEqualTo expectedStatus
        verify { fakeRoomPollRepository.getLoadedPollsStatus(aRoomId) }
    }
}
