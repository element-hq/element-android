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

package im.vector.app.features.settings.font

import com.airbnb.mvrx.test.MvRxTestRule
import im.vector.app.test.fakes.FakeConfiguration
import im.vector.app.test.fakes.FakeFontScalePreferences
import im.vector.app.test.test
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FontScaleSettingViewModelTest {

    @get:Rule
    val mvrxTestRule = MvRxTestRule()

    private val fakeConfiguration = FakeConfiguration()
    private val fakeFontScalePreferences = FakeFontScalePreferences()

    private var initialState = FontScaleSettingViewState()
    private lateinit var viewModel: FontScaleSettingViewModel

    @Before
    fun setUp() {
        viewModelWith(initialState)
    }

    private fun viewModelWith(state: FontScaleSettingViewState) {
        FontScaleSettingViewModel(
                state,
                fakeConfiguration.instance,
                fakeFontScalePreferences
        ).also {
            viewModel = it
            initialState = state
        }
    }

    @Test
    fun `when handling FontScaleChangedAction, then changes state and emits RestartActivity event`() = runTest {
        val scaleOptions = fakeFontScalePreferences.getAvailableScales()
        viewModelWith(
                initialState.copy(
                        availableScaleOptions = scaleOptions,
                        persistedSettingIndex = 0,
                )
        )

        val test = viewModel.test()

        val newIndex = 2

        viewModel.handle(FontScaleSettingAction.FontScaleChangedAction(scaleOptions[newIndex]))

        test
                .assertStatesChanges(
                        initialState,
                        { copy(persistedSettingIndex = newIndex) }
                )
                .assertEvents(FontScaleSettingViewEvents.RestartActivity)
                .finish()

        fakeFontScalePreferences.verifyAppScaleFontValue(scaleOptions[newIndex])
    }

    @Test
    fun `when handling UseSystemSettingChangedAction, then changes state and emits RestartActivity event`() = runTest {
        val scaleOptions = fakeFontScalePreferences.getAvailableScales()
        viewModelWith(
                initialState.copy(availableScaleOptions = scaleOptions)
        )

        val test = viewModel.test()
        fakeFontScalePreferences.givenAppSettingIsDifferentFromSystemSetting()
        val newValue = false

        viewModel.handle(FontScaleSettingAction.UseSystemSettingChangedAction(newValue))

        test
                .assertStatesChanges(
                        initialState,
                        { copy(useSystemSettings = newValue) }
                )
                .assertEvents(FontScaleSettingViewEvents.RestartActivity)
                .finish()
    }
}
