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

package fr.gouv.tchap.features.expired

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session

class ExpiredAccountViewModel @AssistedInject constructor(
        @Assisted initialState: ExpiredAccountViewState,
        val session: Session
) : VectorViewModel<ExpiredAccountViewState, ExpiredAccountAction, EmptyViewEvents>(initialState) {

    @AssistedFactory
    interface Factory {
        fun create(initialState: ExpiredAccountViewState): ExpiredAccountViewModel
    }

    companion object : MvRxViewModelFactory<ExpiredAccountViewModel, ExpiredAccountViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: ExpiredAccountViewState): ExpiredAccountViewModel {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state) ?: error("You should let your activity/fragment implements Factory interface")
        }
    }

    override fun handle(action: ExpiredAccountAction) {
        when (action) {
            ExpiredAccountAction.RequestSendingRenewalEmail -> onRequestRenewalEmail()
        }.exhaustive
    }

    private fun onRequestRenewalEmail() {
        setState { copy(isRenewalEmailSent = true) }
        viewModelScope.launch {
            session.accountValidityService().requestRenewalEmail()
        }
    }
}
