/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.features.poll.PollMode
import im.vector.app.features.poll.create.CreatePollArgs
import im.vector.app.features.poll.create.CreatePollViewModel
import im.vector.app.features.poll.create.CreatePollViewState
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.room.model.message.MessagePollContent
import org.matrix.android.sdk.api.session.room.model.message.PollAnswer
import org.matrix.android.sdk.api.session.room.model.message.PollCreationInfo
import org.matrix.android.sdk.api.session.room.model.message.PollQuestion
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import kotlin.random.Random

object FakeCreatePollViewStates {

    const val A_FAKE_ROOM_ID = "fakeRoomId"
    private const val A_FAKE_EVENT_ID = "fakeEventId"
    private const val A_FAKE_USER_ID = "fakeUserId"

    val createPollArgs = CreatePollArgs(A_FAKE_ROOM_ID, null, PollMode.CREATE)
    val editPollArgs = CreatePollArgs(A_FAKE_ROOM_ID, A_FAKE_EVENT_ID, PollMode.EDIT)

    const val A_FAKE_QUESTION = "What is your favourite coffee?"
    val A_FAKE_OPTIONS = List(CreatePollViewModel.MAX_OPTIONS_COUNT + 1) { "Coffee No${Random.nextInt()}" }

    private val A_POLL_CONTENT = MessagePollContent(
            unstablePollCreationInfo = PollCreationInfo(
                    question = PollQuestion(
                            unstableQuestion = A_FAKE_QUESTION
                    ),
                    maxSelections = 1,
                    answers = listOf(
                            PollAnswer(
                                    id = "5ef5f7b0-c9a1-49cf-a0b3-374729a43e76",
                                    unstableAnswer = A_FAKE_OPTIONS[0]
                            ),
                            PollAnswer(
                                    id = "ec1a4db0-46d8-4d7a-9bb6-d80724715938",
                                    unstableAnswer = A_FAKE_OPTIONS[1]
                            )
                    )
            )
    )

    private val A_POLL_START_EVENT = Event(
            type = EventType.POLL_START.unstable,
            eventId = A_FAKE_EVENT_ID,
            originServerTs = 1652435922563,
            senderId = A_FAKE_USER_ID,
            roomId = A_FAKE_ROOM_ID,
            content = A_POLL_CONTENT.toContent()
    )

    val A_POLL_START_TIMELINE_EVENT = TimelineEvent(
            root = A_POLL_START_EVENT,
            localId = 12345,
            eventId = A_FAKE_EVENT_ID,
            displayIndex = 1,
            senderInfo = SenderInfo(A_FAKE_USER_ID, isUniqueDisplayName = true, avatarUrl = "", displayName = "")
    )

    val initialCreatePollViewState = CreatePollViewState(createPollArgs).copy(
            canCreatePoll = false,
            canAddMoreOptions = true
    )

    val pollViewStateWithOnlyQuestion = initialCreatePollViewState.copy(
            question = A_FAKE_QUESTION,
            canCreatePoll = false,
            canAddMoreOptions = true
    )

    val pollViewStateWithQuestionAndNotEnoughOptions = initialCreatePollViewState.copy(
            question = A_FAKE_QUESTION,
            options = A_FAKE_OPTIONS.take(CreatePollViewModel.MIN_OPTIONS_COUNT - 1).toMutableList().apply { add("") },
            canCreatePoll = false,
            canAddMoreOptions = true
    )

    val pollViewStateWithoutQuestionAndEnoughOptions = initialCreatePollViewState.copy(
            question = "",
            options = A_FAKE_OPTIONS.take(CreatePollViewModel.MIN_OPTIONS_COUNT),
            canCreatePoll = false,
            canAddMoreOptions = true
    )

    val pollViewStateWithQuestionAndEnoughOptions = initialCreatePollViewState.copy(
            question = A_FAKE_QUESTION,
            options = A_FAKE_OPTIONS.take(CreatePollViewModel.MIN_OPTIONS_COUNT),
            canCreatePoll = true,
            canAddMoreOptions = true
    )

    val pollViewStateWithQuestionAndEnoughOptionsButDeletedLastOption = pollViewStateWithQuestionAndEnoughOptions.copy(
            options = A_FAKE_OPTIONS.take(CreatePollViewModel.MIN_OPTIONS_COUNT).toMutableList().apply { removeLast() },
            canCreatePoll = false,
            canAddMoreOptions = true
    )

    val pollViewStateWithQuestionAndMaxOptions = initialCreatePollViewState.copy(
            question = A_FAKE_QUESTION,
            options = A_FAKE_OPTIONS.take(CreatePollViewModel.MAX_OPTIONS_COUNT),
            canCreatePoll = true,
            canAddMoreOptions = false
    )

    val editedPollViewState = pollViewStateWithQuestionAndEnoughOptions.copy(
            editedEventId = A_FAKE_EVENT_ID,
            mode = PollMode.EDIT
    )
}
