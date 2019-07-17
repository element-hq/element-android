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

package im.vector.riotx.features.roomdirectory.createroom

import androidx.fragment.app.FragmentActivity
import com.airbnb.mvrx.*
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.room.model.RoomDirectoryVisibility
import im.vector.matrix.android.api.session.room.model.create.CreateRoomParams
import im.vector.matrix.android.api.session.room.model.create.CreateRoomPreset
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.features.roomdirectory.RoomDirectoryActivity

class CreateRoomViewModel @AssistedInject constructor(@Assisted initialState: CreateRoomViewState,
                                                      private val session: Session
) : VectorViewModel<CreateRoomViewState>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: CreateRoomViewState): CreateRoomViewModel
    }

    companion object : MvRxViewModelFactory<CreateRoomViewModel, CreateRoomViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: CreateRoomViewState): CreateRoomViewModel? {
            val activity: FragmentActivity = (viewModelContext as ActivityViewModelContext).activity()

            return when (activity) {
                is CreateRoomActivity    -> activity.createRoomViewModelFactory.create(state)
                is RoomDirectoryActivity -> activity.createRoomViewModelFactory.create(state)
                else                     -> throw IllegalStateException("Wrong activity")
            }
        }
    }

    fun setName(newName: String) = setState { copy(roomName = newName) }

    fun setIsPublic(isPublic: Boolean) = setState { copy(isPublic = isPublic) }

    fun setIsInRoomDirectory(isInRoomDirectory: Boolean) = setState { copy(isInRoomDirectory = isInRoomDirectory) }

    fun doCreateRoom() = withState { state ->
        if (state.asyncCreateRoomRequest is Loading || state.asyncCreateRoomRequest is Success) {
            return@withState
        }

        setState {
            copy(asyncCreateRoomRequest = Loading())
        }

        val createRoomParams = CreateRoomParams().apply {
            name = state.roomName.takeIf { it.isNotBlank() }

            // Directory visibility
            visibility = if (state.isInRoomDirectory) RoomDirectoryVisibility.PUBLIC else RoomDirectoryVisibility.PRIVATE

            // Public room
            preset = if (state.isPublic) CreateRoomPreset.PRESET_PUBLIC_CHAT else CreateRoomPreset.PRESET_PRIVATE_CHAT
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