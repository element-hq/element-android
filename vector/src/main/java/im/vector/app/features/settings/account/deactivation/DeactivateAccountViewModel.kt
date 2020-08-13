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
package im.vector.app.features.settings.account.deactivation

import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.failure.isInvalidPassword
import org.matrix.android.sdk.api.session.Session
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.platform.VectorViewModelAction

data class DeactivateAccountViewState(
        val passwordShown: Boolean = false
) : MvRxState

sealed class DeactivateAccountAction : VectorViewModelAction {
    object TogglePassword : DeactivateAccountAction()
    data class DeactivateAccount(val password: String, val eraseAllData: Boolean) : DeactivateAccountAction()
}

class DeactivateAccountViewModel @AssistedInject constructor(@Assisted private val initialState: DeactivateAccountViewState,
                                                             private val session: Session)
    : VectorViewModel<DeactivateAccountViewState, DeactivateAccountAction, DeactivateAccountViewEvents>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: DeactivateAccountViewState): DeactivateAccountViewModel
    }

    override fun handle(action: DeactivateAccountAction) {
        when (action) {
            DeactivateAccountAction.TogglePassword       -> handleTogglePassword()
            is DeactivateAccountAction.DeactivateAccount -> handleDeactivateAccount(action)
        }.exhaustive
    }

    private fun handleTogglePassword() = withState {
        setState {
            copy(passwordShown = !passwordShown)
        }
    }

    private fun handleDeactivateAccount(action: DeactivateAccountAction.DeactivateAccount) {
        if (action.password.isEmpty()) {
            _viewEvents.post(DeactivateAccountViewEvents.EmptyPassword)
            return
        }

        _viewEvents.post(DeactivateAccountViewEvents.Loading())

        session.deactivateAccount(action.password, action.eraseAllData, object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                _viewEvents.post(DeactivateAccountViewEvents.Done)
            }

            override fun onFailure(failure: Throwable) {
                if (failure.isInvalidPassword()) {
                    _viewEvents.post(DeactivateAccountViewEvents.InvalidPassword)
                } else {
                    _viewEvents.post(DeactivateAccountViewEvents.OtherFailure(failure))
                }
            }
        })
    }

    companion object : MvRxViewModelFactory<DeactivateAccountViewModel, DeactivateAccountViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: DeactivateAccountViewState): DeactivateAccountViewModel? {
            val fragment: DeactivateAccountFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.viewModelFactory.create(state)
        }
    }
}
