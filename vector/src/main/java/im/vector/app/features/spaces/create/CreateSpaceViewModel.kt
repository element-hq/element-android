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
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.R
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.platform.VectorViewModelAction
import im.vector.app.core.resources.StringProvider
import org.matrix.android.sdk.api.session.Session

data class CreateSpaceState(
        val name: String? = null,
        val avatarUri: Uri? = null,
        val topic: String = "",
        val step: Step = Step.ChooseType,
        val spaceType: SpaceType? = null,
        val nameInlineError : String? = null,
        val defaultRooms: List<String>? = null
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
    object OnBackPressed : CreateSpaceAction()
    object NextFromDetails : CreateSpaceAction()
}

sealed class CreateSpaceEvents : VectorViewEvents {
    object NavigateToDetails : CreateSpaceEvents()
    object NavigateToChooseType : CreateSpaceEvents()
    object NavigateToAddRooms : CreateSpaceEvents()
    object Dismiss : CreateSpaceEvents()
}

class CreateSpaceViewModel @AssistedInject constructor(
        @Assisted initialState: CreateSpaceState,
        private val session: Session,
        private val stringProvider: StringProvider
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
}
