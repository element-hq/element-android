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

package im.vector.app.features.poll.create

import com.airbnb.mvrx.test.MvRxTestRule
import im.vector.app.features.poll.PollMode
import im.vector.app.features.poll.create.CreatePollViewStates.createPollArgs
import im.vector.app.features.poll.create.CreatePollViewStates.editPollArgs
import im.vector.app.features.poll.create.CreatePollViewStates.fakeOptions
import im.vector.app.features.poll.create.CreatePollViewStates.fakeQuestion
import im.vector.app.features.poll.create.CreatePollViewStates.initialCreatePollViewState
import im.vector.app.features.poll.create.CreatePollViewStates.pollViewStateWithOnlyQuestion
import im.vector.app.features.poll.create.CreatePollViewStates.pollViewStateWithQuestionAndNotEnoughOptions
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.test
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

class CreatePollViewModelTest {

    @get:Rule
    val mvrxTestRule = MvRxTestRule()

    private val fakeSession = FakeSession()

    private fun createPollViewModel(pollMode: PollMode): CreatePollViewModel {
        return if (pollMode == PollMode.EDIT) {
            CreatePollViewModel(CreatePollViewState(editPollArgs), fakeSession)
        } else {
            CreatePollViewModel(CreatePollViewState(createPollArgs), fakeSession)
        }
    }

    @Test
    fun `given the view model is initialized then poll cannot be created and options can be added`() {
        val createPollViewModel = createPollViewModel(PollMode.CREATE)
        createPollViewModel
                .test()
                .assertState(initialCreatePollViewState)
                .finish()
    }
}
