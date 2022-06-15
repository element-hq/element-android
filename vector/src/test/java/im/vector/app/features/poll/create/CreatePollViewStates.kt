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

import im.vector.app.features.poll.PollMode
import kotlin.random.Random

object CreatePollViewStates {

    const val fakeRoomId = "fakeRoomId"
    const val fakeEventId = "fakeEventId"

    val createPollArgs = CreatePollArgs(fakeRoomId, null, PollMode.CREATE)
    val editPollArgs = CreatePollArgs(fakeRoomId, fakeEventId, PollMode.EDIT)

    const val fakeQuestion = "What is your favourite coffee?"
    val fakeOptions = List(CreatePollViewModel.MAX_OPTIONS_COUNT + 1) { "Coffee No${Random.nextInt()}" }

    val initialCreatePollViewState = CreatePollViewState(createPollArgs).copy(
        canCreatePoll = false,
        canAddMoreOptions = true
    )

    val pollViewStateWithOnlyQuestion = initialCreatePollViewState.copy(
            question = fakeQuestion,
            canCreatePoll = false,
            canAddMoreOptions = true
    )

    val pollViewStateWithQuestionAndNotEnoughOptions = initialCreatePollViewState.copy(
            question = fakeQuestion,
            options = fakeOptions.take(CreatePollViewModel.MIN_OPTIONS_COUNT - 1).toMutableList().apply { add("") },
            canCreatePoll = false,
            canAddMoreOptions = true
    )

    val pollViewStateWithoutQuestionAndEnoughOptions = initialCreatePollViewState.copy(
            question = "",
            options = fakeOptions.take(CreatePollViewModel.MIN_OPTIONS_COUNT),
            canCreatePoll = false,
            canAddMoreOptions = true
    )

    val pollViewStateWithQuestionAndEnoughOptions = initialCreatePollViewState.copy(
            question = fakeQuestion,
            options = fakeOptions.take(CreatePollViewModel.MIN_OPTIONS_COUNT),
            canCreatePoll = true,
            canAddMoreOptions = true
    )

    val pollViewStateWithQuestionAndMaxOptions = initialCreatePollViewState.copy(
            question = fakeQuestion,
            options = fakeOptions.take(CreatePollViewModel.MAX_OPTIONS_COUNT),
            canCreatePoll = true,
            canAddMoreOptions = false
    )
}
