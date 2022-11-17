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

package im.vector.app.features.attachments

import com.airbnb.mvrx.test.MavericksTestRule
import im.vector.app.test.fakes.FakeVectorFeatures
import im.vector.app.test.fakes.FakeVectorPreferences
import im.vector.app.test.test
import io.mockk.verifyOrder
import org.junit.Before
import org.junit.Rule
import org.junit.Test

internal class AttachmentTypeSelectorViewModelTest {

    @get:Rule
    val mavericksTestRule = MavericksTestRule()

    private val fakeVectorFeatures = FakeVectorFeatures()
    private val fakeVectorPreferences = FakeVectorPreferences()
    private val initialState = AttachmentTypeSelectorViewState()

    @Before
    fun setUp() {
        // Disable all features by default
        fakeVectorFeatures.givenLocationSharing(isEnabled = false)
        fakeVectorFeatures.givenVoiceBroadcast(isEnabled = false)
        fakeVectorPreferences.givenTextFormatting(isEnabled = false)
    }

    @Test
    fun `given features are not enabled, then options are not visible`() {
        createViewModel()
                .test()
                .assertStates(
                        listOf(
                                initialState,
                        )
                )
                .finish()
    }

    @Test
    fun `given location sharing is enabled, then location sharing option is visible`() {
        fakeVectorFeatures.givenLocationSharing(isEnabled = true)

        createViewModel()
                .test()
                .assertStates(
                        listOf(
                                initialState.copy(
                                        isLocationVisible = true
                                ),
                        )
                )
                .finish()
    }

    @Test
    fun `given voice broadcast is enabled, then voice broadcast option is visible`() {
        fakeVectorFeatures.givenVoiceBroadcast(isEnabled = true)

        createViewModel()
                .test()
                .assertStates(
                        listOf(
                                initialState.copy(
                                        isVoiceBroadcastVisible = true
                                ),
                        )
                )
                .finish()
    }

    @Test
    fun `given text formatting is enabled, then text formatting option is checked`() {
        fakeVectorPreferences.givenTextFormatting(isEnabled = true)

        createViewModel()
                .test()
                .assertStates(
                        listOf(
                                initialState.copy(
                                        isTextFormattingEnabled = true
                                ),
                        )
                )
                .finish()
    }

    @Test
    fun `when text formatting is changed, then it updates the UI`() {
        createViewModel()
                .apply {
                    handle(AttachmentTypeSelectorAction.ToggleTextFormatting(isEnabled = true))
                }
                .test()
                .assertStates(
                        listOf(
                                initialState.copy(
                                        isTextFormattingEnabled = true
                                ),
                        )
                )
                .finish()
    }

    @Test
    fun `when text formatting is changed, then it persists the change`() {
        createViewModel()
                .apply {
                    handle(AttachmentTypeSelectorAction.ToggleTextFormatting(isEnabled = true))
                    handle(AttachmentTypeSelectorAction.ToggleTextFormatting(isEnabled = false))
                }
        verifyOrder {
            fakeVectorPreferences.instance.setTextFormattingEnabled(true)
            fakeVectorPreferences.instance.setTextFormattingEnabled(false)
        }
    }

    private fun createViewModel(): AttachmentTypeSelectorViewModel {
        return AttachmentTypeSelectorViewModel(
                initialState,
                vectorFeatures = fakeVectorFeatures,
                vectorPreferences = fakeVectorPreferences.instance,
        )
    }
}
