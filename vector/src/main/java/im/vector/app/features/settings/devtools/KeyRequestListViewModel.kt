/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.settings.devtools

import androidx.lifecycle.asFlow
import androidx.paging.PagedList
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyAction
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.model.IncomingRoomKeyRequest
import org.matrix.android.sdk.api.session.crypto.model.OutgoingRoomKeyRequest

data class KeyRequestListViewState(
        val incomingRequests: Async<PagedList<IncomingRoomKeyRequest>> = Uninitialized,
        val outgoingRoomKeyRequests: Async<PagedList<OutgoingRoomKeyRequest>> = Uninitialized
) : MavericksState

class KeyRequestListViewModel @AssistedInject constructor(@Assisted initialState: KeyRequestListViewState,
                                                          private val session: Session) :
        VectorViewModel<KeyRequestListViewState, EmptyAction, EmptyViewEvents>(initialState) {

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            session.cryptoService().getOutgoingRoomKeyRequestsPaged().asFlow()
                    .execute {
                        copy(outgoingRoomKeyRequests = it)
                    }

            session.cryptoService().getIncomingRoomKeyRequestsPaged()
                    .asFlow()
                    .execute {
                        copy(incomingRequests = it)
                    }
        }
    }

    override fun handle(action: EmptyAction) {}

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<KeyRequestListViewModel, KeyRequestListViewState> {
        override fun create(initialState: KeyRequestListViewState): KeyRequestListViewModel
    }

    companion object : MavericksViewModelFactory<KeyRequestListViewModel, KeyRequestListViewState> by hiltMavericksViewModelFactory()
}
