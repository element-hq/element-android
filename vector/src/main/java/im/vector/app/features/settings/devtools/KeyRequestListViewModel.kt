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

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.internal.crypto.IncomingRoomKeyRequest
import org.matrix.android.sdk.internal.crypto.OutgoingRoomKeyRequest
import im.vector.app.core.platform.EmptyAction
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.launch

data class KeyRequestListViewState(
        val incomingRequests: Async<List<IncomingRoomKeyRequest>> = Uninitialized,
        val outgoingRoomKeyRequests: Async<List<OutgoingRoomKeyRequest>> = Uninitialized
) : MvRxState

class KeyRequestListViewModel @AssistedInject constructor(@Assisted initialState: KeyRequestListViewState,
                                                          private val session: Session)
    : VectorViewModel<KeyRequestListViewState, EmptyAction, EmptyViewEvents>(initialState) {

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            session.cryptoService().getOutgoingRoomKeyRequests().let {
                setState {
                    copy(
                            outgoingRoomKeyRequests = Success(it)
                    )
                }
            }
            session.cryptoService().getIncomingRoomKeyRequests().let {
                setState {
                    copy(
                            incomingRequests = Success(it)
                    )
                }
            }
        }
    }

    override fun handle(action: EmptyAction) {}

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: KeyRequestListViewState): KeyRequestListViewModel
    }

    companion object : MvRxViewModelFactory<KeyRequestListViewModel, KeyRequestListViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: KeyRequestListViewState): KeyRequestListViewModel? {
            val context = viewModelContext as FragmentViewModelContext
            val factory = (context.fragment as? IncomingKeyRequestListFragment)?.viewModelFactory
                    ?: (context.fragment as? OutgoingKeyRequestListFragment)?.viewModelFactory

            return factory?.create(state)
        }
    }
}
