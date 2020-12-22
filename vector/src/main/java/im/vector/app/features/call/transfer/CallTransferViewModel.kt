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

package im.vector.app.features.call.transfer

import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.app.core.platform.EmptyAction
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import org.matrix.android.sdk.api.session.Session
import timber.log.Timber

class CallTransferViewModel @AssistedInject constructor(@Assisted initialState: CallTransferViewState)
    : VectorViewModel<CallTransferViewState, EmptyAction, EmptyViewEvents>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: CallTransferViewState): CallTransferViewModel
    }

    companion object : MvRxViewModelFactory<CallTransferViewModel, CallTransferViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: CallTransferViewState): CallTransferViewModel? {
            val activity: CallTransferActivity = (viewModelContext as ActivityViewModelContext).activity()
            return activity.callTransferViewModelFactory.create(state)
        }
    }

    override fun handle(action: EmptyAction) {}

}
