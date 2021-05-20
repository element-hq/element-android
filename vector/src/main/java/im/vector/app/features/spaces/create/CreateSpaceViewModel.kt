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

package im.vector.app.features.spaces.create

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.R
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CreateSpaceViewModel @AssistedInject constructor(
        @Assisted initialState: CreateSpaceState,
        private val stringProvider: StringProvider,
        private val createSpaceViewModelTask: CreateSpaceViewModelTask,
        private val errorFormatter: ErrorFormatter
) : VectorViewModel<CreateSpaceState, CreateSpaceAction, CreateSpaceEvents>(initialState) {

    @AssistedFactory
    interface Factory {
        fun create(initialState: CreateSpaceState): CreateSpaceViewModel
    }

    companion object : MvRxViewModelFactory<CreateSpaceViewModel, CreateSpaceState> {

        override fun create(viewModelContext: ViewModelContext, state: CreateSpaceState): CreateSpaceViewModel? {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state) ?: error("You should let your activity/fragment implements Factory interface")
        }

        override fun initialState(viewModelContext: ViewModelContext): CreateSpaceState? {
            return CreateSpaceState(
                    defaultRooms = mapOf(
                            0 to viewModelContext.activity.getString(R.string.create_spaces_default_public_room_name),
                            1 to viewModelContext.activity.getString(R.string.create_spaces_default_public_random_room_name)
                    )
            )
        }
    }

    override fun handle(action: CreateSpaceAction) {
        when (action) {
            is CreateSpaceAction.SetRoomType -> {
                setState {
                    copy(
                            step = CreateSpaceState.Step.SetDetails,
                            spaceType = action.type
                    )
                }
                _viewEvents.post(CreateSpaceEvents.NavigateToDetails)
            }
            is CreateSpaceAction.NameChanged -> {
                setState {
                    copy(
                            nameInlineError = null,
                            name = action.name
                    )
                }
            }
            is CreateSpaceAction.TopicChanged -> {
                setState {
                    copy(
                            topic = action.topic
                    )
                }
            }
            CreateSpaceAction.OnBackPressed -> {
                handleBackNavigation()
            }
            CreateSpaceAction.NextFromDetails -> {
                handleNextFromDetails()
            }
            CreateSpaceAction.NextFromDefaultRooms -> {
                handleNextFromDefaultRooms()
            }
            is CreateSpaceAction.DefaultRoomNameChanged -> {
                setState {
                    copy(
                            defaultRooms = (defaultRooms ?: emptyMap()).toMutableMap().apply {
                                this[action.index] = action.name
                            }
                    )
                }
            }
            is CreateSpaceAction.SetAvatar -> {
                setState { copy(avatarUri = action.uri) }
            }
            is CreateSpaceAction.SetSpaceTopology -> {
                handleSetTopology(action)
            }
        }.exhaustive
    }

    private fun handleSetTopology(action: CreateSpaceAction.SetSpaceTopology) {
        when (action.topology) {
            SpaceTopology.JustMe -> {
                setState {
                    copy(
                            spaceTopology = SpaceTopology.JustMe,
                            defaultRooms = emptyMap()
                    )
                }
                handleNextFromDefaultRooms()
            }
            SpaceTopology.MeAndTeammates -> {
                setState {
                    copy(
                            spaceTopology = SpaceTopology.MeAndTeammates,
                            step = CreateSpaceState.Step.AddRooms
                    )
                }
                _viewEvents.post(CreateSpaceEvents.NavigateToAddRooms)
            }
        }
    }

    private fun handleBackNavigation() = withState { state ->
        when (state.step) {
            CreateSpaceState.Step.ChooseType -> {
                _viewEvents.post(CreateSpaceEvents.Dismiss)
            }
            CreateSpaceState.Step.SetDetails -> {
                setState {
                    copy(
                            step = CreateSpaceState.Step.ChooseType,
                            nameInlineError = null,
                            creationResult = Uninitialized
                    )
                }
                _viewEvents.post(CreateSpaceEvents.NavigateToChooseType)
            }
            CreateSpaceState.Step.AddRooms -> {
                if (state.spaceType == SpaceType.Private && state.spaceTopology == SpaceTopology.MeAndTeammates) {
                    setState {
                        copy(
                                spaceTopology = null,
                                step = CreateSpaceState.Step.ChoosePrivateType
                        )
                    }
                    _viewEvents.post(CreateSpaceEvents.NavigateToChoosePrivateType)
                } else {
                    setState {
                        copy(
                                step = CreateSpaceState.Step.SetDetails
                        )
                    }
                    _viewEvents.post(CreateSpaceEvents.NavigateToDetails)
                }
            }
            CreateSpaceState.Step.ChoosePrivateType -> {
                setState {
                    copy(
                            step = CreateSpaceState.Step.SetDetails
                    )
                }
                _viewEvents.post(CreateSpaceEvents.NavigateToDetails)
            }
        }
    }

    private fun handleNextFromDetails() = withState { state ->
        if (state.name.isNullOrBlank()) {
            setState {
                copy(
                        nameInlineError = stringProvider.getString(R.string.create_space_error_empty_field_space_name)
                )
            }
        } else {
            if (state.spaceType == SpaceType.Private) {
                setState {
                    copy(
                            step = CreateSpaceState.Step.ChoosePrivateType
                    )
                }
                _viewEvents.post(CreateSpaceEvents.NavigateToChoosePrivateType)
            } else {
                setState {
                    copy(
                            step = CreateSpaceState.Step.AddRooms
                    )
                }
                _viewEvents.post(CreateSpaceEvents.NavigateToAddRooms)
            }
        }
    }

    private fun handleNextFromDefaultRooms() = withState { state ->
        val spaceName = state.name ?: return@withState
        setState {
            copy(creationResult = Loading())
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = createSpaceViewModelTask.execute(
                        CreateSpaceTaskParams(
                                spaceName = spaceName,
                                spaceTopic = state.topic,
                                spaceAvatar = state.avatarUri,
                                isPublic = state.spaceType == SpaceType.Public,
                                defaultRooms = state.defaultRooms
                                        ?.entries
                                        ?.sortedBy { it.key }
                                        ?.mapNotNull { it.value } ?: emptyList()
                        )
                )
                when (result) {
                    is CreateSpaceTaskResult.Success -> {
                        setState {
                            copy(creationResult = Success(result.spaceId))
                        }
                        _viewEvents.post(
                                CreateSpaceEvents.FinishSuccess(
                                        result.spaceId,
                                        result.childIds.firstOrNull(),
                                        state.spaceTopology
                                )
                        )
                    }
                    is CreateSpaceTaskResult.PartialSuccess -> {
                        // XXX what can we do here?
                        setState {
                            copy(creationResult = Success(result.spaceId))
                        }
                        _viewEvents.post(
                                CreateSpaceEvents.FinishSuccess(
                                        result.spaceId,
                                        result.childIds.firstOrNull(),
                                        state.spaceTopology
                                )
                        )
                    }
                    is CreateSpaceTaskResult.FailedToCreateSpace -> {
                        setState {
                            copy(creationResult = Fail(result.failure))
                        }
                        _viewEvents.post(CreateSpaceEvents.ShowModalError(errorFormatter.toHumanReadable(result.failure)))
                    }
                }
            } catch (failure: Throwable) {
                setState {
                    copy(creationResult = Fail(failure))
                }
                _viewEvents.post(CreateSpaceEvents.ShowModalError(errorFormatter.toHumanReadable(failure)))
            }
        }
    }
}
