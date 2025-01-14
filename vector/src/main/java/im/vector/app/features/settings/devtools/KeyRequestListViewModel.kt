/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import org.matrix.android.sdk.api.session.crypto.OutgoingKeyRequest
import org.matrix.android.sdk.api.session.crypto.model.IncomingRoomKeyRequest

data class KeyRequestListViewState(
        val incomingRequests: Async<PagedList<IncomingRoomKeyRequest>> = Uninitialized,
        val outgoingRoomKeyRequests: Async<PagedList<OutgoingKeyRequest>> = Uninitialized
) : MavericksState

class KeyRequestListViewModel @AssistedInject constructor(
        @Assisted initialState: KeyRequestListViewState,
        private val session: Session
) :
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
