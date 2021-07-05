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
import org.matrix.android.sdk.api.MatrixPatterns
import org.matrix.android.sdk.api.MatrixPatterns.getDomain
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.AliasAvailabilityResult
import org.matrix.android.sdk.api.session.room.failure.CreateRoomFailure

class CreateSpaceViewModel @AssistedInject constructor(
        @Assisted initialState: CreateSpaceState,
        private val session: Session,
        private val stringProvider: StringProvider,
        private val createSpaceViewModelTask: CreateSpaceViewModelTask,
        private val errorFormatter: ErrorFormatter
) : VectorViewModel<CreateSpaceState, CreateSpaceAction, CreateSpaceEvents>(initialState) {

    init {
        setState {
            copy(
                    homeServerName = session.myUserId.getDomain()
            )
        }
    }

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
            is CreateSpaceAction.SetRoomType            -> {
                setState {
                    copy(
                            step = CreateSpaceState.Step.SetDetails,
                            spaceType = action.type
                    )
                }
                _viewEvents.post(CreateSpaceEvents.NavigateToDetails)
            }
            is CreateSpaceAction.NameChanged            -> {
                setState {
                    if (aliasManuallyModified) {
                        copy(
                                nameInlineError = null,
                                name = action.name,
                                aliasVerificationTask = Uninitialized
                        )
                    } else {
                        val tentativeAlias =
                                MatrixPatterns.candidateAliasFromRoomName(action.name)
                        copy(
                                nameInlineError = null,
                                name = action.name,
                                aliasLocalPart = tentativeAlias,
                                aliasVerificationTask = Uninitialized
                        )
                    }
                }
            }
            is CreateSpaceAction.TopicChanged           -> {
                setState {
                    copy(
                            topic = action.topic
                    )
                }
            }
            is CreateSpaceAction.SpaceAliasChanged      -> {
                // This called only when the alias is change manually
                // not when programmatically changed via a change on name
                setState {
                    copy(
                            aliasManuallyModified = true,
                            aliasLocalPart = action.aliasLocalPart,
                            aliasVerificationTask = Uninitialized
                    )
                }
            }
            CreateSpaceAction.OnBackPressed             -> {
                handleBackNavigation()
            }
            CreateSpaceAction.NextFromDetails           -> {
                handleNextFromDetails()
            }
            CreateSpaceAction.NextFromDefaultRooms      -> {
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
            is CreateSpaceAction.SetAvatar              -> {
                setState { copy(avatarUri = action.uri) }
            }
            is CreateSpaceAction.SetSpaceTopology       -> {
                handleSetTopology(action)
            }
        }.exhaustive
    }

    private fun handleSetTopology(action: CreateSpaceAction.SetSpaceTopology) {
        when (action.topology) {
            SpaceTopology.JustMe         -> {
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
            CreateSpaceState.Step.ChooseType        -> {
                _viewEvents.post(CreateSpaceEvents.Dismiss)
            }
            CreateSpaceState.Step.SetDetails        -> {
                setState {
                    copy(
                            step = CreateSpaceState.Step.ChooseType,
                            nameInlineError = null,
                            creationResult = Uninitialized
                    )
                }
                _viewEvents.post(CreateSpaceEvents.NavigateToChooseType)
            }
            CreateSpaceState.Step.AddRooms          -> {
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
                // it'a public space, let's check alias
                val aliasLocalPart = state.aliasLocalPart
                _viewEvents.post(CreateSpaceEvents.ShowModalLoading(null))
                setState {
                    copy(aliasVerificationTask = Loading())
                }
                viewModelScope.launch {
                    try {
                        when (val result = session.checkAliasAvailability(aliasLocalPart)) {
                            AliasAvailabilityResult.Available       -> {
                                setState {
                                    copy(
                                            step = CreateSpaceState.Step.AddRooms
                                    )
                                }
                                _viewEvents.post(CreateSpaceEvents.HideModalLoading)
                                _viewEvents.post(CreateSpaceEvents.NavigateToAddRooms)
                            }
                            is AliasAvailabilityResult.NotAvailable -> {
                                setState {
                                    copy(aliasVerificationTask = Fail(result.roomAliasError))
                                }
                                _viewEvents.post(CreateSpaceEvents.HideModalLoading)
                            }
                        }
                    } catch (failure: Throwable) {
                        setState {
                            copy(aliasVerificationTask = Fail(failure))
                        }
                        _viewEvents.post(CreateSpaceEvents.HideModalLoading)
                    }
                }
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
                val alias = if (state.spaceType == SpaceType.Public) {
                    state.aliasLocalPart
                } else null
                val result = createSpaceViewModelTask.execute(
                        CreateSpaceTaskParams(
                                spaceName = spaceName,
                                spaceTopic = state.topic,
                                spaceAvatar = state.avatarUri,
                                isPublic = state.spaceType == SpaceType.Public,
                                defaultRooms = state.defaultRooms
                                        ?.entries
                                        ?.sortedBy { it.key }
                                        ?.mapNotNull { it.value } ?: emptyList(),
                                spaceAlias = alias
                        )
                )
                when (result) {
                    is CreateSpaceTaskResult.Success             -> {
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
                    is CreateSpaceTaskResult.PartialSuccess      -> {
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
                        if (result.failure is CreateRoomFailure.AliasError) {
                            setState {
                                copy(
                                        step = CreateSpaceState.Step.SetDetails,
                                        aliasVerificationTask = Fail(result.failure.aliasError),
                                        creationResult = Uninitialized
                                )
                            }
                            _viewEvents.post(CreateSpaceEvents.HideModalLoading)
                            _viewEvents.post(CreateSpaceEvents.NavigateToDetails)
                        } else {
                            setState {
                                copy(creationResult = Fail(result.failure))
                            }
                            _viewEvents.post(CreateSpaceEvents.ShowModalError(errorFormatter.toHumanReadable(result.failure)))
                        }
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
