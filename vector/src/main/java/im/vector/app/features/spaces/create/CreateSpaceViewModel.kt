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

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxState
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
import im.vector.app.core.platform.VectorViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.platform.VectorViewModelAction
import im.vector.app.core.resources.StringProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session

data class CreateSpaceState(
        val name: String? = null,
        val avatarUri: Uri? = null,
        val topic: String = "",
        val step: Step = Step.ChooseType,
        val spaceType: SpaceType? = null,
        val nameInlineError: String? = null,
        val defaultRooms: Map<Int, String?>? = null,
        val creationResult: Async<String> = Uninitialized
) : MvRxState {

    enum class Step {
        ChooseType,
        SetDetails,
        AddRooms
    }
}

enum class SpaceType {
    Public,
    Private
}

sealed class CreateSpaceAction : VectorViewModelAction {
    data class SetRoomType(val type: SpaceType) : CreateSpaceAction()
    data class NameChanged(val name: String) : CreateSpaceAction()
    data class TopicChanged(val topic: String) : CreateSpaceAction()
    data class SetAvatar(val uri: Uri?) : CreateSpaceAction()
    object OnBackPressed : CreateSpaceAction()
    object NextFromDetails : CreateSpaceAction()
    object NextFromDefaultRooms : CreateSpaceAction()
    data class DefaultRoomNameChanged(val index: Int, val name: String) : CreateSpaceAction()
}

sealed class CreateSpaceEvents : VectorViewEvents {
    object NavigateToDetails : CreateSpaceEvents()
    object NavigateToChooseType : CreateSpaceEvents()
    object NavigateToAddRooms : CreateSpaceEvents()
    object Dismiss : CreateSpaceEvents()
    data class FinishSuccess(val spaceId: String, val defaultRoomId: String?) : CreateSpaceEvents()
    data class ShowModalError(val errorMessage: String) : CreateSpaceEvents()
    object HideModalLoading : CreateSpaceEvents()
}

class CreateSpaceViewModel @AssistedInject constructor(
        @Assisted initialState: CreateSpaceState,
        private val session: Session,
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
        }.exhaustive
    }

    private fun handleBackNavigation() = withState { state ->
        when (state.step) {
            CreateSpaceState.Step.ChooseType -> {
                _viewEvents.post(CreateSpaceEvents.Dismiss)
            }
            CreateSpaceState.Step.SetDetails -> {
                setState {
                    copy(
                            step = CreateSpaceState.Step.ChooseType
                    )
                }
                _viewEvents.post(CreateSpaceEvents.NavigateToChooseType)
            }
            CreateSpaceState.Step.AddRooms -> {
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
            setState {
                copy(
                        step = CreateSpaceState.Step.AddRooms
                )
            }
            _viewEvents.post(CreateSpaceEvents.NavigateToAddRooms)
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
                        _viewEvents.post(CreateSpaceEvents.FinishSuccess(result.spaceId, result.childIds.firstOrNull()))
                    }
                    is CreateSpaceTaskResult.PartialSuccess      -> {
                        // XXX what can we do here?
                        setState {
                            copy(creationResult = Success(result.spaceId))
                        }
                        _viewEvents.post(CreateSpaceEvents.FinishSuccess(result.spaceId, result.childIds.firstOrNull()))
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
