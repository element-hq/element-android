/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.poll.create

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.extensions.getVectorLastMessageContent
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.poll.PollMode
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.getTimelineEvent
import org.matrix.android.sdk.api.session.room.model.message.MessagePollContent
import org.matrix.android.sdk.api.session.room.model.message.PollType

class CreatePollViewModel @AssistedInject constructor(
        @Assisted private val initialState: CreatePollViewState,
        session: Session
) : VectorViewModel<CreatePollViewState, CreatePollAction, CreatePollViewEvents>(initialState) {

    private val room = session.getRoom(initialState.roomId)!!

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<CreatePollViewModel, CreatePollViewState> {
        override fun create(initialState: CreatePollViewState): CreatePollViewModel
    }

    companion object : MavericksViewModelFactory<CreatePollViewModel, CreatePollViewState> by hiltMavericksViewModelFactory() {

        const val MIN_OPTIONS_COUNT = 2
        const val MAX_OPTIONS_COUNT = 20
    }

    init {
        observeState()
        initialState.editedEventId?.let {
            initializeEditedPoll(it)
        }
    }

    private fun observeState() {
        onEach(
                CreatePollViewState::question,
                CreatePollViewState::options
        ) { question, options ->
            setState {
                copy(
                        canCreatePoll = canCreatePoll(question, options),
                        canAddMoreOptions = options.size < MAX_OPTIONS_COUNT
                )
            }
        }
    }

    private fun initializeEditedPoll(eventId: String) {
        val event = room.getTimelineEvent(eventId) ?: return
        val content = event.getVectorLastMessageContent() as? MessagePollContent ?: return

        val pollCreationInfo = content.getBestPollCreationInfo()
        val pollType = pollCreationInfo?.kind ?: PollType.DISCLOSED_UNSTABLE
        val question = pollCreationInfo?.question?.getBestQuestion() ?: ""
        val options = pollCreationInfo?.answers?.mapNotNull { it.getBestAnswer() } ?: List(MIN_OPTIONS_COUNT) { "" }

        setState {
            copy(
                    question = question,
                    options = options,
                    pollType = pollType
            )
        }
    }

    override fun handle(action: CreatePollAction) {
        when (action) {
            CreatePollAction.OnCreatePoll -> handleOnCreatePoll()
            CreatePollAction.OnAddOption -> handleOnAddOption()
            is CreatePollAction.OnDeleteOption -> handleOnDeleteOption(action.index)
            is CreatePollAction.OnOptionChanged -> handleOnOptionChanged(action.index, action.option)
            is CreatePollAction.OnQuestionChanged -> handleOnQuestionChanged(action.question)
            is CreatePollAction.OnPollTypeChanged -> handleOnPollTypeChanged(action.pollType)
        }
    }

    private fun handleOnCreatePoll() = withState { state ->
        val nonEmptyOptions = state.options.filter { it.isNotEmpty() }
        when {
            state.question.isEmpty() -> {
                _viewEvents.post(CreatePollViewEvents.EmptyQuestionError)
            }
            nonEmptyOptions.size < MIN_OPTIONS_COUNT -> {
                _viewEvents.post(CreatePollViewEvents.NotEnoughOptionsError(requiredOptionsCount = MIN_OPTIONS_COUNT))
            }
            else -> {
                when (state.mode) {
                    PollMode.CREATE -> room.sendService().sendPoll(state.pollType, state.question, nonEmptyOptions)
                    PollMode.EDIT -> sendEditedPoll(state.editedEventId!!, state.pollType, state.question, nonEmptyOptions)
                }
                _viewEvents.post(CreatePollViewEvents.Success)
            }
        }
    }

    private fun sendEditedPoll(editedEventId: String, pollType: PollType, question: String, options: List<String>) {
        val editedEvent = room.getTimelineEvent(editedEventId) ?: return
        room.relationService().editPoll(editedEvent, pollType, question, options)
    }

    private fun handleOnAddOption() {
        setState {
            val extendedOptions = options + ""
            copy(
                    options = extendedOptions
            )
        }
    }

    private fun handleOnDeleteOption(index: Int) {
        setState {
            val filteredOptions = options.filterIndexed { ind, _ -> ind != index }
            copy(
                    options = filteredOptions
            )
        }
    }

    private fun handleOnOptionChanged(index: Int, option: String) {
        setState {
            val changedOptions = options.mapIndexed { ind, s -> if (ind == index) option else s }
            copy(
                    options = changedOptions
            )
        }
    }

    private fun handleOnQuestionChanged(question: String) {
        setState {
            copy(
                    question = question
            )
        }
    }

    private fun handleOnPollTypeChanged(pollType: PollType) {
        setState {
            copy(
                    pollType = pollType
            )
        }
    }

    private fun canCreatePoll(question: String, options: List<String>): Boolean {
        return question.isNotEmpty() &&
                options.filter { it.isNotEmpty() }.size >= MIN_OPTIONS_COUNT
    }
}
