/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location.domain.usecase

import im.vector.app.features.location.LocationData
import im.vector.app.test.fakes.FakeSession
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.OverrideMockKs
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class CompareLocationsUseCaseTest {

    private val session = FakeSession()

    @OverrideMockKs
    lateinit var compareLocationsUseCase: CompareLocationsUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `given 2 very near locations when calling execute then these locations are considered as equal`() = runTest {
        // Given
        val location1 = LocationData(
                latitude = 48.858269,
                longitude = 2.294551,
                uncertainty = null
        )
        val location2 = LocationData(
                latitude = 48.858275,
                longitude = 2.294547,
                uncertainty = null
        )
        // When
        val areEqual = compareLocationsUseCase.execute(location1, location2)

        // Then
        assert(areEqual)
    }

    @Test
    fun `given 2 far away locations when calling execute then these locations are considered as not equal`() = runTest {
        // Given
        val location1 = LocationData(
                latitude = 48.858269,
                longitude = 2.294551,
                uncertainty = null
        )
        val location2 = LocationData(
                latitude = 48.861777,
                longitude = 2.289348,
                uncertainty = null
        )
        // When
        val areEqual = compareLocationsUseCase.execute(location1, location2)

        // Then
        assert(areEqual.not())
    }
}
