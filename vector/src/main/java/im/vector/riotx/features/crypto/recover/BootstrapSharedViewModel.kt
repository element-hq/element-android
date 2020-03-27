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

package im.vector.riotx.features.crypto.recover

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.nulabinc.zxcvbn.Strength
import com.nulabinc.zxcvbn.Zxcvbn
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.session.Session
import im.vector.riotx.core.extensions.exhaustive
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.features.login.ReAuthHelper

data class BootstrapViewState(
        val step: BootstrapStep = BootstrapStep.SetupPassphrase(false),
        val passphrase: String? = null,
        val crossSigningInitialization: Async<Unit> = Uninitialized,
        val passphraseStrength: Async<Strength> = Uninitialized
) : MvRxState

sealed class BootstrapStep {
    data class SetupPassphrase(val isPasswordVisible: Boolean) : BootstrapStep()
    data class ConfirmPassphrase(val isPasswordVisible: Boolean) : BootstrapStep()
    object Initializing : BootstrapStep()
}

class BootstrapSharedViewModel @AssistedInject constructor(
        @Assisted initialState: BootstrapViewState,
        private val session: Session,
        private val reAuthHelper: ReAuthHelper
) : VectorViewModel<BootstrapViewState, BootstrapActions, BootstrapViewEvents>(initialState) {

    private val zxcvbn = Zxcvbn()

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: BootstrapViewState): BootstrapSharedViewModel
    }

    override fun handle(action: BootstrapActions) = withState { state ->
        when (action) {
            is BootstrapActions.GoBack                -> queryBack()
            BootstrapActions.TogglePasswordVisibility -> {
                when (state.step) {
                    is BootstrapStep.SetupPassphrase   -> {
                        setState {
                            copy(step = state.step.copy(isPasswordVisible = !state.step.isPasswordVisible))
                        }
                    }
                    is BootstrapStep.ConfirmPassphrase -> {
                        setState {
                            copy(step = state.step.copy(isPasswordVisible = !state.step.isPasswordVisible))
                        }
                    }
                    else                               -> {
                    }
                }
            }
            is BootstrapActions.UpdateCandidatePassphrase -> {
                val strength = zxcvbn.measure(action.pass)
                setState {
                    copy(
                            passphrase = action.pass,
                            passphraseStrength = Success(strength)
                    )
                }
            }
            is BootstrapActions.GoToConfirmPassphrase -> {
                setState {
                    copy(
                            passphrase = action.passphrase,
                            step = BootstrapStep.ConfirmPassphrase(
                                    isPasswordVisible = (state.step as? BootstrapStep.SetupPassphrase)?.isPasswordVisible ?: false
                            )
                    )
                }
            }
            is BootstrapActions.UpdateConfirmCandidatePassphrase -> {

            }
        }.exhaustive
    }

    // =======================================
    // Fragment interaction
    // =======================================

    private fun queryBack() = withState { state ->
        when (state.step) {
            is BootstrapStep.SetupPassphrase -> {

            }
            is BootstrapStep.ConfirmPassphrase -> {
                setState {
                    copy(
                            step = BootstrapStep.SetupPassphrase(
                                    isPasswordVisible = (state.step as? BootstrapStep.ConfirmPassphrase)?.isPasswordVisible ?: false
                            )
                    )
                }
            }

        }
    }

    // ======================================
    // Companion, view model assisted creation
    // ======================================

    companion object : MvRxViewModelFactory<BootstrapSharedViewModel, BootstrapViewState> {

        override fun create(viewModelContext: ViewModelContext, state: BootstrapViewState): BootstrapSharedViewModel? {
            val fragment: BootstrapBottomSheet = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.bootstrapViewModelFactory.create(state)
        }
    }
}
