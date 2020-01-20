/*
 * Copyright 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotx.features.roomprofile.settings

import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.internal.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import im.vector.matrix.rx.rx
import im.vector.matrix.rx.unwrap
import im.vector.riotx.core.extensions.postLiveEvent
import im.vector.riotx.core.platform.VectorViewModel

class RoomSettingsViewModel @AssistedInject constructor(@Assisted initialState: RoomSettingsViewState,
                                                        private val session: Session)
    : VectorViewModel<RoomSettingsViewState, RoomSettingsAction>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: RoomSettingsViewState): RoomSettingsViewModel
    }

    companion object : MvRxViewModelFactory<RoomSettingsViewModel, RoomSettingsViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: RoomSettingsViewState): RoomSettingsViewModel? {
            val fragment: RoomSettingsFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.viewModelFactory.create(state)
        }
    }

    private val room = session.getRoom(initialState.roomId)!!

    init {
        observeRoomSummary()
    }

    private fun observeRoomSummary() {
        room.rx().liveRoomSummary()
                .unwrap()
                .execute { async ->
                    copy(roomSummary = async)
                }
    }

    override fun handle(action: RoomSettingsAction) {
        when (action) {
            is RoomSettingsAction.EnableEncryption -> handleEnableEncryption()
        }
    }

    private fun handleEnableEncryption() {
        setState {
            copy(currentRequest = Loading())
        }

        room.enableEncryption(MXCRYPTO_ALGORITHM_MEGOLM, object : MatrixCallback<Unit> {
            override fun onFailure(failure: Throwable) {
                setState {
                    copy(currentRequest = Uninitialized)
                }

                _requestErrorLiveData.postLiveEvent(failure)
            }

            override fun onSuccess(data: Unit) {
                setState {
                    copy(currentRequest = Uninitialized)
                }
            }
        })
    }
}
