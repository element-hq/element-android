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
import timber.log.Timber

class CreatePollViewModel @AssistedInject constructor(@Assisted
                                                      initialState: CreatePollViewState) :
        VectorViewModel<CreatePollViewState, CreatePollAction, CreatePollViewEvents>(initialState) {

    @AssistedFactory
    interface Factory {
        fun create(initialState: CreatePollViewState): CreatePollViewModel
    }

    companion object : MavericksViewModelFactory<CreatePollViewModel, CreatePollViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: CreatePollViewState): CreatePollViewModel {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state) ?: error("You should let your activity/fragment implements Factory interface")
        }
    }

    init {
        // Initialize with 2 default empty options
        setState {
            copy(
                    question = "",
                    options = listOf("", "")
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
        Timber.d(state.toString())
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
            val filteredOptions = options.filterIndexed { ind, _ -> ind != index  }
            copy(
                    options = filteredOptions
            )
        }
    }

    private fun handleOnOptionChanged(index: Int, option: String) {
        setState {
            val changedOptions = options.mapIndexed { ind, s -> if(ind == index) option else s }
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
}
