/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
