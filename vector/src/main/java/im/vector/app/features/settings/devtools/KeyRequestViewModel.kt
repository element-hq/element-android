/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devtools

import android.net.Uri
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.platform.VectorViewModelAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session

sealed class KeyRequestAction : VectorViewModelAction {
    data class ExportAudit(val uri: Uri) : KeyRequestAction()
}

sealed class KeyRequestEvents : VectorViewEvents {
    data class SaveAudit(val uri: Uri, val raw: String) : KeyRequestEvents()
}

data class KeyRequestViewState(
        val exporting: Async<Unit> = Uninitialized
) : MavericksState

class KeyRequestViewModel @AssistedInject constructor(
        @Assisted initialState: KeyRequestViewState,
        private val session: Session
) :
        VectorViewModel<KeyRequestViewState, KeyRequestAction, KeyRequestEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<KeyRequestViewModel, KeyRequestViewState> {
        override fun create(initialState: KeyRequestViewState): KeyRequestViewModel
    }

    companion object : MavericksViewModelFactory<KeyRequestViewModel, KeyRequestViewState> by hiltMavericksViewModelFactory()

    override fun handle(action: KeyRequestAction) {
        when (action) {
            is KeyRequestAction.ExportAudit -> exportAudit(action)
        }
    }

    private fun exportAudit(action: KeyRequestAction.ExportAudit) {
        setState {
            copy(exporting = Loading())
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // this can take long
                val eventList = session.cryptoService().getGossipingEvents()
                // clean it a bit to
                val raw = GossipingEventsSerializer().serialize(eventList)
                setState {
                    copy(exporting = Success(Unit))
                }
                _viewEvents.post(KeyRequestEvents.SaveAudit(action.uri, raw))
            } catch (error: Throwable) {
                setState {
                    copy(exporting = Fail(error))
                }
            }
        }
    }
}
