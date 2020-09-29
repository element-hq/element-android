/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.features.roomdirectory.createroom

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.raw.wellknown.getElementWellknown
import im.vector.app.features.raw.wellknown.isE2EByDefault
import im.vector.app.features.roomdirectory.RoomDirectoryActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.model.RoomDirectoryVisibility
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomPreset

class CreateRoomViewModel @AssistedInject constructor(@Assisted initialState: CreateRoomViewState,
                                                      private val session: Session,
                                                      private val rawService: RawService
) : VectorViewModel<CreateRoomViewState, CreateRoomAction, EmptyViewEvents>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: CreateRoomViewState): CreateRoomViewModel
    }

    init {
        initAdminE2eByDefault()
    }

    private var adminE2EByDefault = true

    private fun initAdminE2eByDefault() {
        viewModelScope.launch(Dispatchers.IO) {
            adminE2EByDefault = tryOrNull {
                rawService.getElementWellknown(session.myUserId)
                        ?.isE2EByDefault()
                        ?: true
            } ?: true

            setState {
                copy(
                        isEncrypted = !isPublic && adminE2EByDefault,
                        hsAdminHasDisabledE2E = !adminE2EByDefault
                )
            }
        }
    }

    companion object : MvRxViewModelFactory<CreateRoomViewModel, CreateRoomViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: CreateRoomViewState): CreateRoomViewModel? {
            val activity: FragmentActivity = (viewModelContext as ActivityViewModelContext).activity()

            return when (activity) {
                is CreateRoomActivity    -> activity.createRoomViewModelFactory.create(state)
                is RoomDirectoryActivity -> activity.createRoomViewModelFactory.create(state)
                else                     -> error("Wrong activity")
            }
        }
    }

    override fun handle(action: CreateRoomAction) {
        when (action) {
            is CreateRoomAction.SetName              -> setName(action)
            is CreateRoomAction.SetIsPublic          -> setIsPublic(action)
            is CreateRoomAction.SetIsInRoomDirectory -> setIsInRoomDirectory(action)
            is CreateRoomAction.SetIsEncrypted       -> setIsEncrypted(action)
            is CreateRoomAction.Create               -> doCreateRoom()
        }
    }

    private fun setName(action: CreateRoomAction.SetName) = setState { copy(roomName = action.name) }

    private fun setIsPublic(action: CreateRoomAction.SetIsPublic) = setState {
        copy(
                isPublic = action.isPublic,
                isEncrypted = !action.isPublic && adminE2EByDefault
        )
    }

    private fun setIsInRoomDirectory(action: CreateRoomAction.SetIsInRoomDirectory) = setState { copy(isInRoomDirectory = action.isInRoomDirectory) }

    private fun setIsEncrypted(action: CreateRoomAction.SetIsEncrypted) = setState { copy(isEncrypted = action.isEncrypted) }

    private fun doCreateRoom() = withState { state ->
        if (state.asyncCreateRoomRequest is Loading || state.asyncCreateRoomRequest is Success) {
            return@withState
        }

        setState {
            copy(asyncCreateRoomRequest = Loading())
        }

        val createRoomParams = CreateRoomParams()
                .apply {
                    name = state.roomName.takeIf { it.isNotBlank() }
                    // Directory visibility
                    visibility = if (state.isInRoomDirectory) RoomDirectoryVisibility.PUBLIC else RoomDirectoryVisibility.PRIVATE
                    // Public room
                    preset = if (state.isPublic) CreateRoomPreset.PRESET_PUBLIC_CHAT else CreateRoomPreset.PRESET_PRIVATE_CHAT

                    // Encryption
                    if (state.isEncrypted) {
                        enableEncryption()
                    }
                }

        session.createRoom(createRoomParams, object : MatrixCallback<String> {
            override fun onSuccess(data: String) {
                setState {
                    copy(asyncCreateRoomRequest = Success(data))
                }
            }

            override fun onFailure(failure: Throwable) {
                setState {
                    copy(asyncCreateRoomRequest = Fail(failure))
                }
            }
        })
    }
}
