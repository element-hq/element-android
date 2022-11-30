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

package im.vector.app.features.settings.labs

import com.airbnb.mvrx.test.MavericksTestRule
import im.vector.app.core.session.clientinfo.DeleteMatrixClientInfoUseCase
import im.vector.app.core.session.clientinfo.UpdateMatrixClientInfoUseCase
import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.test
import im.vector.app.test.testDispatcher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class VectorSettingsLabsViewModelTest {

    @get:Rule
    val mavericksTestRule = MavericksTestRule(testDispatcher = testDispatcher)

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()
    private val fakeUpdateMatrixClientInfoUseCase = mockk<UpdateMatrixClientInfoUseCase>()
    private val fakeDeleteMatrixClientInfoUseCase = mockk<DeleteMatrixClientInfoUseCase>()

    private fun createViewModel(): VectorSettingsLabsViewModel {
        return VectorSettingsLabsViewModel(
                initialState = VectorSettingsLabsViewState(),
                activeSessionHolder = fakeActiveSessionHolder.instance,
                updateMatrixClientInfoUseCase = fakeUpdateMatrixClientInfoUseCase,
                deleteMatrixClientInfoUseCase = fakeDeleteMatrixClientInfoUseCase,
        )
    }

    @Test
    fun `given update client info action when handling this action then update client info use case is called`() {
        // Given
        givenUpdateClientInfoSucceeds()

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(VectorSettingsLabsAction.UpdateClientInfo)

        // Then
        viewModelTest.finish()
        coVerify { fakeUpdateMatrixClientInfoUseCase.execute(fakeActiveSessionHolder.fakeSession) }
    }

    @Test
    fun `given delete client info action when handling this action then delete client info use case is called`() {
        // Given
        givenDeleteClientInfoSucceeds()

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(VectorSettingsLabsAction.DeleteRecordedClientInfo)

        // Then
        viewModelTest.finish()
        coVerify { fakeDeleteMatrixClientInfoUseCase.execute() }
    }

    private fun givenUpdateClientInfoSucceeds() {
        coEvery { fakeUpdateMatrixClientInfoUseCase.execute(any()) } returns Result.success(Unit)
    }

    private fun givenDeleteClientInfoSucceeds() {
        coEvery { fakeDeleteMatrixClientInfoUseCase.execute() } returns Result.success(Unit)
    }
}
