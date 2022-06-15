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
import im.vector.app.features.poll.create.CreatePollViewStates.fakeRoomId
import im.vector.app.features.poll.create.CreatePollViewStates.initialCreatePollViewState
import im.vector.app.features.poll.create.CreatePollViewStates.pollViewStateWithOnlyQuestion
import im.vector.app.features.poll.create.CreatePollViewStates.pollViewStateWithQuestionAndEnoughOptions
import im.vector.app.features.poll.create.CreatePollViewStates.pollViewStateWithQuestionAndMaxOptions
import im.vector.app.features.poll.create.CreatePollViewStates.pollViewStateWithQuestionAndNotEnoughOptions
import im.vector.app.features.poll.create.CreatePollViewStates.pollViewStateWithoutQuestionAndEnoughOptions
import im.vector.app.test.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.message.PollType
import org.matrix.android.sdk.api.session.room.send.SendService
import org.matrix.android.sdk.api.util.Cancelable

class CreatePollViewModelTest {

    @get:Rule
    val mvrxTestRule = MvRxTestRule()

    private val fakeSession = mockk<Session>()
    private val fakeRoom = mockk<Room>()
    private val fakeSendService = mockk<SendService>()
    private val fakeCancellable = mockk<Cancelable>()

    private fun createPollViewModel(pollMode: PollMode): CreatePollViewModel {
        return if (pollMode == PollMode.EDIT) {
            CreatePollViewModel(CreatePollViewState(editPollArgs), fakeSession)
        } else {
            CreatePollViewModel(CreatePollViewState(createPollArgs), fakeSession)
        }
    }

    @Before
    fun setup() {
        every { fakeSession.getRoom(fakeRoomId) } returns fakeRoom
        every { fakeRoom.sendService() } returns fakeSendService
        every { fakeSendService.sendPoll(any(), fakeQuestion, any()) } returns fakeCancellable
    }

    @Test
    fun `given the view model is initialized then poll cannot be created and more options can be added`() = runTest {
        val createPollViewModel = createPollViewModel(PollMode.CREATE)
        createPollViewModel
                .test()
                .assertState(initialCreatePollViewState)
                .finish()
    }

    @Test
    fun `given there is not any options when the question is added then poll cannot be created and more options can be added`() = runTest {
        val createPollViewModel = createPollViewModel(PollMode.CREATE)
        createPollViewModel.handle(CreatePollAction.OnQuestionChanged(fakeQuestion))

        // We need to wait for createPollViewModel.onChange is triggered
        delay(10)
        createPollViewModel
                .test()
                .assertState(pollViewStateWithOnlyQuestion)
                .finish()
    }

    @Test
    fun `given there is not enough options when the question is added then poll cannot be created and more options can be added`() = runTest {
        val createPollViewModel = createPollViewModel(PollMode.CREATE)
        createPollViewModel.handle(CreatePollAction.OnQuestionChanged(fakeQuestion))
        repeat(CreatePollViewModel.MIN_OPTIONS_COUNT - 1) {
            createPollViewModel.handle(CreatePollAction.OnOptionChanged(it, fakeOptions[it]))
        }

        delay(10)
        createPollViewModel
                .test()
                .assertState(pollViewStateWithQuestionAndNotEnoughOptions)
                .finish()
    }

    @Test
    fun `given there is not a question when enough options are added then poll cannot be created and more options can be added`() = runTest {
        val createPollViewModel = createPollViewModel(PollMode.CREATE)
        repeat(CreatePollViewModel.MIN_OPTIONS_COUNT) {
            createPollViewModel.handle(CreatePollAction.OnOptionChanged(it, fakeOptions[it]))
        }

        delay(10)
        createPollViewModel
                .test()
                .assertState(pollViewStateWithoutQuestionAndEnoughOptions)
                .finish()
    }

    @Test
    fun `given there is a question when enough options are added then poll can be created and more options can be added`() = runTest {
        val createPollViewModel = createPollViewModel(PollMode.CREATE)
        createPollViewModel.handle(CreatePollAction.OnQuestionChanged(fakeQuestion))
        repeat(CreatePollViewModel.MIN_OPTIONS_COUNT) {
            createPollViewModel.handle(CreatePollAction.OnOptionChanged(it, fakeOptions[it]))
        }

        delay(10)
        createPollViewModel
                .test()
                .assertState(pollViewStateWithQuestionAndEnoughOptions)
                .finish()
    }

    @Test
    fun `given there is a question when max number of options are added then poll can be created and more options cannot be added`() = runTest {
        val createPollViewModel = createPollViewModel(PollMode.CREATE)
        createPollViewModel.handle(CreatePollAction.OnQuestionChanged(fakeQuestion))
        repeat(CreatePollViewModel.MAX_OPTIONS_COUNT) {
            if (it >= CreatePollViewModel.MIN_OPTIONS_COUNT) {
                createPollViewModel.handle(CreatePollAction.OnAddOption)
            }
            createPollViewModel.handle(CreatePollAction.OnOptionChanged(it, fakeOptions[it]))
        }

        delay(10)
        createPollViewModel
                .test()
                .assertState(pollViewStateWithQuestionAndMaxOptions)
                .finish()
    }

    @Test
    fun `given an initial poll state when poll type is changed then view state is updated accordingly`() = runTest {
        val createPollViewModel = createPollViewModel(PollMode.CREATE)
        createPollViewModel.handle(CreatePollAction.OnPollTypeChanged(PollType.DISCLOSED))
        createPollViewModel.awaitState().pollType shouldBe PollType.DISCLOSED
        createPollViewModel.handle(CreatePollAction.OnPollTypeChanged(PollType.UNDISCLOSED))
        createPollViewModel.awaitState().pollType shouldBe PollType.UNDISCLOSED
    }

    @Test
    fun `given there is not a question and enough options when create poll is requested then error view events are post`() = runTest {
        val createPollViewModel = createPollViewModel(PollMode.CREATE)
        val test = createPollViewModel.test()

        createPollViewModel.handle(CreatePollAction.OnCreatePoll)

        createPollViewModel.handle(CreatePollAction.OnQuestionChanged(fakeQuestion))
        createPollViewModel.handle(CreatePollAction.OnCreatePoll)

        createPollViewModel.handle(CreatePollAction.OnOptionChanged(0, fakeOptions[0]))
        createPollViewModel.handle(CreatePollAction.OnCreatePoll)

        test
                .assertEvents(
                        CreatePollViewEvents.EmptyQuestionError,
                        CreatePollViewEvents.NotEnoughOptionsError(requiredOptionsCount = CreatePollViewModel.MIN_OPTIONS_COUNT),
                        CreatePollViewEvents.NotEnoughOptionsError(requiredOptionsCount = CreatePollViewModel.MIN_OPTIONS_COUNT)
                )
    }

    @Test
    fun `given there is a question and enough options when create poll is requested then success view event is post`() = runTest {
        val createPollViewModel = createPollViewModel(PollMode.CREATE)
        val test = createPollViewModel.test()

        createPollViewModel.handle(CreatePollAction.OnQuestionChanged(fakeQuestion))
        createPollViewModel.handle(CreatePollAction.OnOptionChanged(0, fakeOptions[0]))
        createPollViewModel.handle(CreatePollAction.OnOptionChanged(1, fakeOptions[1]))
        createPollViewModel.handle(CreatePollAction.OnCreatePoll)

        test
                .assertEvents(
                        CreatePollViewEvents.Success
                )
    }
}
