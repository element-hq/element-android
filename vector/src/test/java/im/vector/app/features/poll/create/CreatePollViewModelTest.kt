/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.poll.create

import com.airbnb.mvrx.test.MavericksTestRule
import im.vector.app.features.poll.PollMode
import im.vector.app.test.fakes.FakeCreatePollViewStates.A_FAKE_OPTIONS
import im.vector.app.test.fakes.FakeCreatePollViewStates.A_FAKE_QUESTION
import im.vector.app.test.fakes.FakeCreatePollViewStates.A_FAKE_ROOM_ID
import im.vector.app.test.fakes.FakeCreatePollViewStates.A_POLL_START_TIMELINE_EVENT
import im.vector.app.test.fakes.FakeCreatePollViewStates.createPollArgs
import im.vector.app.test.fakes.FakeCreatePollViewStates.editPollArgs
import im.vector.app.test.fakes.FakeCreatePollViewStates.editedPollViewState
import im.vector.app.test.fakes.FakeCreatePollViewStates.initialCreatePollViewState
import im.vector.app.test.fakes.FakeCreatePollViewStates.pollViewStateWithOnlyQuestion
import im.vector.app.test.fakes.FakeCreatePollViewStates.pollViewStateWithQuestionAndEnoughOptions
import im.vector.app.test.fakes.FakeCreatePollViewStates.pollViewStateWithQuestionAndEnoughOptionsButDeletedLastOption
import im.vector.app.test.fakes.FakeCreatePollViewStates.pollViewStateWithQuestionAndMaxOptions
import im.vector.app.test.fakes.FakeCreatePollViewStates.pollViewStateWithQuestionAndNotEnoughOptions
import im.vector.app.test.fakes.FakeCreatePollViewStates.pollViewStateWithoutQuestionAndEnoughOptions
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.test
import io.mockk.unmockkAll
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.matrix.android.sdk.api.session.room.model.message.PollType

class CreatePollViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mavericksTestRule = MavericksTestRule(
            testDispatcher = testDispatcher // See https://github.com/airbnb/mavericks/issues/599
    )

    private val fakeSession = FakeSession()

    private fun createPollViewModel(pollMode: PollMode): CreatePollViewModel {
        return if (pollMode == PollMode.EDIT) {
            CreatePollViewModel(CreatePollViewState(editPollArgs), fakeSession)
        } else {
            CreatePollViewModel(CreatePollViewState(createPollArgs), fakeSession)
        }
    }

    @Before
    fun setup() {
        fakeSession
                .roomService()
                .getRoom(A_FAKE_ROOM_ID)
                .timelineService()
                .givenTimelineEventReturns(A_POLL_START_TIMELINE_EVENT.eventId, A_POLL_START_TIMELINE_EVENT)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given the view model is initialized then poll cannot be created and more options can be added`() = runTest {
        val createPollViewModel = createPollViewModel(PollMode.CREATE)
        val test = createPollViewModel.test()

        test
                .assertLatestState(initialCreatePollViewState)
                .finish()
    }

    @Test
    fun `given there is not any options when the question is added then poll cannot be created and more options can be added`() = runTest {
        val createPollViewModel = createPollViewModel(PollMode.CREATE)
        val test = createPollViewModel.test()

        createPollViewModel.handle(CreatePollAction.OnQuestionChanged(A_FAKE_QUESTION))

        test
                .assertLatestState(pollViewStateWithOnlyQuestion)
                .finish()
    }

    @Test
    fun `given there is not enough options when the question is added then poll cannot be created and more options can be added`() = runTest {
        val createPollViewModel = createPollViewModel(PollMode.CREATE)
        val test = createPollViewModel.test()

        createPollViewModel.handle(CreatePollAction.OnQuestionChanged(A_FAKE_QUESTION))
        repeat(CreatePollViewModel.MIN_OPTIONS_COUNT - 1) {
            createPollViewModel.handle(CreatePollAction.OnOptionChanged(it, A_FAKE_OPTIONS[it]))
        }

        test
                .assertLatestState(pollViewStateWithQuestionAndNotEnoughOptions)
                .finish()
    }

    @Test
    fun `given there is not a question when enough options are added then poll cannot be created and more options can be added`() = runTest {
        val createPollViewModel = createPollViewModel(PollMode.CREATE)
        val test = createPollViewModel.test()

        repeat(CreatePollViewModel.MIN_OPTIONS_COUNT) {
            createPollViewModel.handle(CreatePollAction.OnOptionChanged(it, A_FAKE_OPTIONS[it]))
        }

        test
                .assertLatestState(pollViewStateWithoutQuestionAndEnoughOptions)
                .finish()
    }

    @Test
    fun `given there is a question when enough options are added then poll can be created and more options can be added`() = runTest {
        val createPollViewModel = createPollViewModel(PollMode.CREATE)
        val test = createPollViewModel.test()

        createPollViewModel.handle(CreatePollAction.OnQuestionChanged(A_FAKE_QUESTION))
        repeat(CreatePollViewModel.MIN_OPTIONS_COUNT) {
            createPollViewModel.handle(CreatePollAction.OnOptionChanged(it, A_FAKE_OPTIONS[it]))
        }

        test
                .assertLatestState(pollViewStateWithQuestionAndEnoughOptions)
                .finish()
    }

    @Test
    fun `given there is a question when max number of options are added then poll can be created and more options cannot be added`() = runTest {
        val createPollViewModel = createPollViewModel(PollMode.CREATE)
        val test = createPollViewModel.test()

        createPollViewModel.handle(CreatePollAction.OnQuestionChanged(A_FAKE_QUESTION))
        repeat(CreatePollViewModel.MAX_OPTIONS_COUNT) {
            if (it >= CreatePollViewModel.MIN_OPTIONS_COUNT) {
                createPollViewModel.handle(CreatePollAction.OnAddOption)
            }
            createPollViewModel.handle(CreatePollAction.OnOptionChanged(it, A_FAKE_OPTIONS[it]))
        }

        test
                .assertLatestState(pollViewStateWithQuestionAndMaxOptions)
                .finish()
    }

    @Test
    fun `given an initial poll state when poll type is changed then view state is updated accordingly`() = runTest {
        val createPollViewModel = createPollViewModel(PollMode.CREATE)
        val test = createPollViewModel.test()

        createPollViewModel.handle(CreatePollAction.OnPollTypeChanged(PollType.UNDISCLOSED))
        createPollViewModel.handle(CreatePollAction.OnPollTypeChanged(PollType.DISCLOSED))

        test
                .assertStatesChanges(
                        initialCreatePollViewState,
                        { copy(pollType = PollType.UNDISCLOSED) },
                        { copy(pollType = PollType.DISCLOSED) },
                )
                .finish()
    }

    @Test
    fun `given there is not a question and enough options when create poll is requested then error view events are post`() = runTest {
        val createPollViewModel = createPollViewModel(PollMode.CREATE)
        val test = createPollViewModel.test()

        createPollViewModel.handle(CreatePollAction.OnCreatePoll)

        createPollViewModel.handle(CreatePollAction.OnQuestionChanged(A_FAKE_QUESTION))
        createPollViewModel.handle(CreatePollAction.OnCreatePoll)

        createPollViewModel.handle(CreatePollAction.OnOptionChanged(0, A_FAKE_OPTIONS[0]))
        createPollViewModel.handle(CreatePollAction.OnCreatePoll)

        test
                .assertEvents(
                        CreatePollViewEvents.EmptyQuestionError,
                        CreatePollViewEvents.NotEnoughOptionsError(requiredOptionsCount = CreatePollViewModel.MIN_OPTIONS_COUNT),
                        CreatePollViewEvents.NotEnoughOptionsError(requiredOptionsCount = CreatePollViewModel.MIN_OPTIONS_COUNT),
                )
    }

    @Test
    fun `given there is a question and enough options when create poll is requested then success view event is post`() = runTest {
        val createPollViewModel = createPollViewModel(PollMode.CREATE)
        val test = createPollViewModel.test()

        createPollViewModel.handle(CreatePollAction.OnQuestionChanged(A_FAKE_QUESTION))
        createPollViewModel.handle(CreatePollAction.OnOptionChanged(0, A_FAKE_OPTIONS[0]))
        createPollViewModel.handle(CreatePollAction.OnOptionChanged(1, A_FAKE_OPTIONS[1]))
        createPollViewModel.handle(CreatePollAction.OnCreatePoll)

        test
                .assertEvents(
                        CreatePollViewEvents.Success,
                )
    }

    @Test
    fun `given there is a question and enough options when the last option is deleted then view state should be updated accordingly`() = runTest {
        val createPollViewModel = createPollViewModel(PollMode.CREATE)
        val test = createPollViewModel.test()

        createPollViewModel.handle(CreatePollAction.OnQuestionChanged(A_FAKE_QUESTION))
        createPollViewModel.handle(CreatePollAction.OnOptionChanged(0, A_FAKE_OPTIONS[0]))
        createPollViewModel.handle(CreatePollAction.OnOptionChanged(1, A_FAKE_OPTIONS[1]))
        createPollViewModel.handle(CreatePollAction.OnDeleteOption(1))

        test.assertLatestState(pollViewStateWithQuestionAndEnoughOptionsButDeletedLastOption)
    }

    @Test
    fun `given an edited poll event when question and options are changed then view state is updated accordingly`() = runTest {
        val createPollViewModel = createPollViewModel(PollMode.EDIT)
        val test = createPollViewModel.test()

        test
                .assertState(editedPollViewState)
                .finish()
    }

    @Test
    fun `given an edited poll event then able to be edited`() = runTest {
        val createPollViewModel = createPollViewModel(PollMode.EDIT)
        val test = createPollViewModel.test()

        createPollViewModel.handle(CreatePollAction.OnCreatePoll)

        test
                .assertEvents(
                        CreatePollViewEvents.Success,
                )
    }
}
