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

package im.vector.riotredesign.features.roomdirectory.picker

import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.ViewModelContext
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.room.model.thirdparty.ThirdPartyProtocol
import im.vector.riotredesign.core.platform.VectorViewModel
import org.koin.android.ext.android.get

class RoomDirectoryPickerViewModel(initialState: RoomDirectoryPickerViewState,
                                   private val session: Session) : VectorViewModel<RoomDirectoryPickerViewState>(initialState) {

    companion object : MvRxViewModelFactory<RoomDirectoryPickerViewModel, RoomDirectoryPickerViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: RoomDirectoryPickerViewState): RoomDirectoryPickerViewModel? {
            val currentSession = viewModelContext.activity.get<Session>()

            return RoomDirectoryPickerViewModel(state, currentSession)
        }
    }

    init {
        load()
    }

    fun load() {
        session.getThirdPartyProtocol(object : MatrixCallback<Map<String, ThirdPartyProtocol>> {
            override fun onSuccess(data: Map<String, ThirdPartyProtocol>) {
                setState {
                    copy(asyncThirdPartyRequest = Success(data))
                }
            }

            override fun onFailure(failure: Throwable) {
                setState {
                    copy(asyncThirdPartyRequest = Fail(failure))
                }
            }
        })
    }

}