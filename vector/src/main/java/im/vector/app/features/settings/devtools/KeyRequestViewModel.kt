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

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.app.core.extensions.exhaustive
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
) : MvRxState

class KeyRequestViewModel @AssistedInject constructor(
        @Assisted initialState: KeyRequestViewState,
        private val session: Session)
    : VectorViewModel<KeyRequestViewState, KeyRequestAction, KeyRequestEvents>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: KeyRequestViewState): KeyRequestViewModel
    }

    companion object : MvRxViewModelFactory<KeyRequestViewModel, KeyRequestViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: KeyRequestViewState): KeyRequestViewModel? {
            val fragment: KeyRequestsFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.viewModelFactory.create(state)
        }
    }

    override fun handle(action: KeyRequestAction) {
        when (action) {
            is KeyRequestAction.ExportAudit -> exportAudit(action)
        }.exhaustive
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
