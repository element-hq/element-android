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

import com.airbnb.mvrx.test.MavericksTestRule
import im.vector.app.features.settings.FontScaleValue
import im.vector.app.test.fakes.FakeConfiguration
import im.vector.app.test.fakes.FakeFontScalePreferences
import im.vector.app.test.test
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private val A_SELECTION = aFontScaleValue(index = 1)
private val A_SCALE_OPTIONS_WITH_SELECTION = listOf(
        aFontScaleValue(index = 0),
        A_SELECTION,
)

// our tests only make use of the index
private fun aFontScaleValue(index: Int) = FontScaleValue(index, "foo", -1f, 0)

class FontScaleSettingViewModelTest {

    @get:Rule
    val mavericksTestRule = MavericksTestRule()

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
    fun `given useSystemSetting is false when handling FontScaleChangedAction, then changes state and emits RestartActivity event`() =
            runTest {
                fakeFontScalePreferences.givenAvailableScaleOptions(A_SCALE_OPTIONS_WITH_SELECTION)
                viewModelWith(initialState)
                val test = viewModel.test()

                viewModel.handle(FontScaleSettingAction.FontScaleChangedAction(A_SELECTION))

                test
                        .assertStatesChanges(
                                initialState.copy(availableScaleOptions = A_SCALE_OPTIONS_WITH_SELECTION),
                                { copy(persistedSettingIndex = A_SELECTION.index) }
                        )
                        .assertEvents(FontScaleSettingViewEvents.RestartActivity)
                        .finish()

                fakeFontScalePreferences.verifyAppScaleFontValue(A_SELECTION)
            }

    @Test
    fun `given app and system font scale are different when handling UseSystemSettingChangedAction, then changes state and emits RestartActivity event`() =
            runTest {
                fakeFontScalePreferences.givenAvailableScaleOptions(A_SCALE_OPTIONS_WITH_SELECTION)
                viewModelWith(initialState)
                val test = viewModel.test()

                fakeFontScalePreferences.givenAppSettingIsDifferentFromSystemSetting()
                val newValue = false

                viewModel.handle(FontScaleSettingAction.UseSystemSettingChangedAction(newValue))

                test
                        .assertStatesChanges(
                                initialState.copy(availableScaleOptions = A_SCALE_OPTIONS_WITH_SELECTION),
                                { copy(useSystemSettings = newValue) }
                        )
                        .assertEvents(FontScaleSettingViewEvents.RestartActivity)
                        .finish()
            }
}
