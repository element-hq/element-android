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

package im.vector.app.features.home.room.detail.composer.link

import com.airbnb.mvrx.test.MavericksTestRule
import im.vector.app.test.test
import im.vector.app.test.testDispatcher
import org.junit.Rule
import org.junit.Test

class SetLinkViewModelTest {

    @get:Rule
    val mavericksTestRule = MavericksTestRule(testDispatcher = testDispatcher)

    companion object {
        const val link = "https://matrix.org"
        const val newLink = "https://matrix.org/new"
        const val text = "Matrix"
    }

    private val fragmentArgs = SetLinkFragment.Args(
            isTextSupported = true,
            initialLink = link
    )

    private fun createViewModel(
            args: SetLinkFragment.Args
    ) = SetLinkViewModel(
            initialState = SetLinkViewState(args),
    )

    @Test
    fun `given no initial link, then remove button is hidden`() {
        val viewModel = createViewModel(
                fragmentArgs
                        .copy(initialLink = null)
        )

        val viewModelTest = viewModel.test()

        viewModelTest
                .assertLatestState { !it.removeVisible }
                .finish()
    }

    @Test
    fun `given no initial link, when link changed, then remove button is still hidden`() {
        val viewModel = createViewModel(
                fragmentArgs.copy(initialLink = null)
        )

        val viewModelTest = viewModel.test()
        viewModel.handle(SetLinkAction.LinkChanged(newLink))

        viewModelTest
                .assertLatestState { !it.removeVisible }
                .finish()
    }

    @Test
    fun `when link is unchanged, it disables the save button`() {
        val viewModel = createViewModel(
                fragmentArgs
                        .copy(initialLink = link)
        )

        val viewModelTest = viewModel.test()

        viewModelTest
                .assertLatestState { !it.saveEnabled }
                .finish()
    }

    @Test
    fun `when link is changed, it enables the save button`() {
        val viewModel = createViewModel(
                fragmentArgs.copy(initialLink = link)
        )

        val viewModelTest = viewModel.test()
        viewModel.handle(SetLinkAction.LinkChanged(newLink))

        viewModelTest
                .assertLatestState { it.saveEnabled }
                .finish()
    }

    @Test
    fun `given no initial link, when link is changed to empty, it disables the save button`() {
        val viewModel = createViewModel(
                fragmentArgs.copy(initialLink = null)
        )

        val viewModelTest = viewModel.test()
        viewModel.handle(SetLinkAction.LinkChanged(""))

        viewModelTest
                .assertLatestState {
                    !it.saveEnabled
                }
                .finish()
    }

    @Test
    fun `given text is supported, when saved, it emits the right event`() {
        val viewModel = createViewModel(
                fragmentArgs.copy(isTextSupported = true)
        )

        val viewModelTest = viewModel.test()
        viewModel.handle(
                SetLinkAction.Save(link = newLink, text = text)
        )

        viewModelTest
                .assertEvent {
                    it == SetLinkViewEvents.SavedLinkAndText(
                            link = newLink,
                            text = text,
                    )
                }
                .finish()
    }

    @Test
    fun `given text is not supported, when saved, it emits the right event`() {
        val viewModel = createViewModel(
                fragmentArgs.copy(isTextSupported = false)
        )

        val viewModelTest = viewModel.test()
        viewModel.handle(
                SetLinkAction.Save(link = newLink, text = text)
        )

        viewModelTest
                .assertEvent {
                    it == SetLinkViewEvents.SavedLink(link = newLink)
                }
                .finish()
    }
}
