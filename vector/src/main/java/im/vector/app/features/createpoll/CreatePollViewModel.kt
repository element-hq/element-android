/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.createpoll

import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.platform.VectorViewModel
import org.matrix.android.sdk.api.session.Session

class CreatePollViewModel @AssistedInject constructor(
        @Assisted private val initialState: CreatePollViewState,
        session: Session
) : VectorViewModel<CreatePollViewState, CreatePollAction, CreatePollViewEvents>(initialState) {

    private val room = session.getRoom(initialState.roomId)!!

    @AssistedFactory
    interface Factory {
        fun create(initialState: CreatePollViewState): CreatePollViewModel
    }

    companion object : MavericksViewModelFactory<CreatePollViewModel, CreatePollViewState> {

        private const val REQUIRED_MIN_OPTION_COUNT = 2

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: CreatePollViewState): CreatePollViewModel {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> (viewModelContext.fragment as CreatePollFragment).createPollViewModelFactory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state) ?: error("You should let your activity/fragment implements Factory interface")
        }
    }

    init {
        // Initialize with REQUIRED_MIN_OPTION_COUNT default empty options
        setState {
            copy(
                    question = "",
                    options = List(REQUIRED_MIN_OPTION_COUNT) { "" }
            )
        }
    }

    override fun handle(action: CreatePollAction) {
        when (action) {
            CreatePollAction.OnCreatePoll         -> handleOnCreatePoll()
            CreatePollAction.OnAddOption          -> handleOnAddOption()
            is CreatePollAction.OnDeleteOption    -> handleOnDeleteOption(action.index)
            is CreatePollAction.OnOptionChanged   -> handleOnOptionChanged(action.index, action.option)
            is CreatePollAction.OnQuestionChanged -> handleOnQuestionChanged(action.question)
        }
    }

    private fun handleOnCreatePoll() = withState { state ->
        val nonEmptyOptions = state.options.filter { it.isNotEmpty() }
        when {
            state.question.isEmpty()                         -> {
                _viewEvents.post(CreatePollViewEvents.EmptyQuestionError)
            }
            nonEmptyOptions.size < REQUIRED_MIN_OPTION_COUNT -> {
                _viewEvents.post(CreatePollViewEvents.NotEnoughOptionsError(requiredOptionsCount = REQUIRED_MIN_OPTION_COUNT))
            }
            else                                             -> {
                room.sendPoll(state.question, state.options)
                _viewEvents.post(CreatePollViewEvents.Success)
            }
        }
    }

    private fun handleOnAddOption() {
        setState {
            val extendedOptions = options + ""
            copy(
                    options = extendedOptions,
                    canCreatePoll = canCreatePoll(this.copy(options = extendedOptions))
            )
        }
    }

    private fun handleOnDeleteOption(index: Int) {
        setState {
            val filteredOptions = options.filterIndexed { ind, _ -> ind != index }
            copy(
                    options = filteredOptions,
                    canCreatePoll = canCreatePoll(this.copy(options = filteredOptions))
            )
        }
    }

    private fun handleOnOptionChanged(index: Int, option: String) {
        setState {
            val changedOptions = options.mapIndexed { ind, s -> if (ind == index) option else s }
            copy(
                    options = changedOptions,
                    canCreatePoll = canCreatePoll(this.copy(options = changedOptions))
            )
        }
    }

    private fun handleOnQuestionChanged(question: String) {
        setState {
            copy(
                    question = question,
                    canCreatePoll = canCreatePoll(this.copy(question = question))
            )
        }
    }

    private fun canCreatePoll(state: CreatePollViewState): Boolean {
        return state.question.isNotEmpty() &&
                state.options.filter { it.isNotEmpty() }.size >= REQUIRED_MIN_OPTION_COUNT
    }
}
