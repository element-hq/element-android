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

package im.vector.app.features.location.domain.usecase

import com.airbnb.mvrx.test.MvRxTestRule
import im.vector.app.features.location.LocationData
import im.vector.app.test.fakes.FakeSession
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.OverrideMockKs
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CompareLocationsUseCaseTest {

    @get:Rule
    val mvRxTestRule = MvRxTestRule()

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
