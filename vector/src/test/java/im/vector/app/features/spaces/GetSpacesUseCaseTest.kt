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

package im.vector.app.features.spaces

import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakeFlowLiveDataConversions
import im.vector.app.test.fakes.givenAsFlow
import im.vector.app.test.test
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.space.SpaceSummaryQueryParams

internal class GetSpacesUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()
    private val fakeFlowLiveDataConversions = FakeFlowLiveDataConversions()

    private val getSpacesUseCase = GetSpacesUseCase(
            activeSessionHolder = fakeActiveSessionHolder.instance,
    )

    @Before
    fun setUp() {
        fakeFlowLiveDataConversions.setup()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given params when execute then the list of summaries is returned`() = runTest {
        // Given
        val queryParams = givenSpaceQueryParams()
        val firstSummaries = listOf<RoomSummary>(mockk())
        val nextSummaries = listOf<RoomSummary>(mockk())
        fakeActiveSessionHolder.fakeSession
                .fakeSpaceService
                .givenGetSpaceSummariesReturns(firstSummaries)
        fakeActiveSessionHolder.fakeSession
                .fakeSpaceService
                .givenGetSpaceSummariesLiveReturns(nextSummaries)
                .givenAsFlow()

        // When
        val testObserver = getSpacesUseCase.execute(queryParams).test(this)
        advanceUntilIdle()

        // Then
        testObserver
                .assertValues(firstSummaries, nextSummaries)
                .finish()
        verify {
            fakeActiveSessionHolder.fakeSession.fakeSpaceService.getSpaceSummaries(queryParams)
            fakeActiveSessionHolder.fakeSession.fakeSpaceService.getSpaceSummariesLive(queryParams)
        }
    }

    @Test
    fun `given no active session when execute then empty flow is returned`() = runTest {
        // Given
        fakeActiveSessionHolder.givenGetSafeActiveSessionReturns(null)
        val queryParams = givenSpaceQueryParams()

        // When
        val testObserver = getSpacesUseCase.execute(queryParams).test(this)
        advanceUntilIdle()

        // Then
        testObserver
                .assertNoValues()
                .finish()
        verify(inverse = true) {
            fakeActiveSessionHolder.fakeSession.fakeSpaceService.getSpaceSummaries(queryParams)
            fakeActiveSessionHolder.fakeSession.fakeSpaceService.getSpaceSummariesLive(queryParams)
        }
    }

    private fun givenSpaceQueryParams(): SpaceSummaryQueryParams {
        return mockk()
    }
}
