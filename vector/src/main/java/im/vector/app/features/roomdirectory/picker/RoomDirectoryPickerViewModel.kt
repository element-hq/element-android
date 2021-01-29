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

package im.vector.app.features.roomdirectory.picker

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.assisted.AssistedFactory
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session

class RoomDirectoryPickerViewModel @AssistedInject constructor(@Assisted initialState: RoomDirectoryPickerViewState,
                                                               private val session: Session)
    : VectorViewModel<RoomDirectoryPickerViewState, RoomDirectoryPickerAction, EmptyViewEvents>(initialState) {

    @AssistedFactory
    interface Factory {
        fun create(initialState: RoomDirectoryPickerViewState): RoomDirectoryPickerViewModel
    }

    companion object : MvRxViewModelFactory<RoomDirectoryPickerViewModel, RoomDirectoryPickerViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: RoomDirectoryPickerViewState): RoomDirectoryPickerViewModel? {
            val fragment: RoomDirectoryPickerFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.roomDirectoryPickerViewModelFactory.create(state)
        }
    }

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            setState {
                copy(asyncThirdPartyRequest = Loading())
            }
            try {
                val thirdPartyProtocols = session.thirdPartyService().getThirdPartyProtocols()
                setState {
                    copy(asyncThirdPartyRequest = Success(thirdPartyProtocols))
                }
            } catch (failure: Throwable) {
                setState {
                    copy(asyncThirdPartyRequest = Fail(failure))
                }
            }
        }
    }

    override fun handle(action: RoomDirectoryPickerAction) {
        when (action) {
            RoomDirectoryPickerAction.Retry -> load()
        }
    }
}
