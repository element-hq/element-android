/*
 * Copyright 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package im.vector.riotx.features.settings.crosssigning

import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.crypto.crosssigning.MXCrossSigningInfo
import im.vector.matrix.android.internal.auth.data.LoginFlowTypes
import im.vector.matrix.android.internal.auth.registration.RegistrationFlowResponse
import im.vector.matrix.android.internal.crypto.crosssigning.isVerified
import im.vector.matrix.android.internal.crypto.model.rest.UserPasswordAuth
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.matrix.rx.rx
import im.vector.riotx.core.extensions.exhaustive
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.platform.VectorViewModelAction

data class CrossSigningSettingsViewState(
        val crossSigningInfo: MXCrossSigningInfo? = null,
        val xSigningIsEnableInAccount: Boolean = false,
        val xSigningKeysAreTrusted: Boolean = false,
        val xSigningKeyCanSign: Boolean = true,
        val isUploadingKeys: Boolean = false
) : MvRxState

sealed class CrossSigningAction : VectorViewModelAction {
    object InitializeCrossSigning : CrossSigningAction()
    data class PasswordEntered(val password: String) : CrossSigningAction()
}

class CrossSigningSettingsViewModel @AssistedInject constructor(@Assisted private val initialState: CrossSigningSettingsViewState,
                                                                private val session: Session)
    : VectorViewModel<CrossSigningSettingsViewState, CrossSigningAction, CrossSigningSettingsViewEvents>(initialState) {

    init {
        session.rx().liveCrossSigningInfo(session.myUserId)
                .execute {
                    val crossSigningKeys = it.invoke()?.getOrNull()
                    val xSigningIsEnableInAccount = crossSigningKeys != null
                    val xSigningKeysAreTrusted = session.cryptoService().crossSigningService().checkUserTrust(session.myUserId).isVerified()
                    val xSigningKeyCanSign = session.cryptoService().crossSigningService().canCrossSign()
                    copy(
                            crossSigningInfo = crossSigningKeys,
                            xSigningIsEnableInAccount = xSigningIsEnableInAccount,
                            xSigningKeysAreTrusted = xSigningKeysAreTrusted,
                            xSigningKeyCanSign = xSigningKeyCanSign
                    )
                }
    }

    // Storage when password is required
    private var _pendingSession: String? = null

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: CrossSigningSettingsViewState): CrossSigningSettingsViewModel
    }

    override fun handle(action: CrossSigningAction) {
        when (action) {
            is CrossSigningAction.InitializeCrossSigning -> {
                initializeCrossSigning(null)
            }
            is CrossSigningAction.PasswordEntered        -> {
                initializeCrossSigning(UserPasswordAuth(
                        session = _pendingSession,
                        user = session.myUserId,
                        password = action.password
                ))
            }
        }.exhaustive
    }

    private fun initializeCrossSigning(auth: UserPasswordAuth?) {
        _pendingSession = null

        setState {
            copy(isUploadingKeys = true)
        }
        session.cryptoService().crossSigningService().initializeCrossSigning(auth, object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                _pendingSession = null

                setState {
                    copy(isUploadingKeys = false)
                }
            }

            override fun onFailure(failure: Throwable) {
                _pendingSession = null

                if (failure is Failure.OtherServerError && failure.httpCode == 401) {
                    try {
                        MoshiProvider.providesMoshi()
                                .adapter(RegistrationFlowResponse::class.java)
                                .fromJson(failure.errorBody)
                    } catch (e: Exception) {
                        null
                    }?.let { flowResponse ->
                        // Retry with authentication
                        if (flowResponse.flows?.any { it.stages?.contains(LoginFlowTypes.PASSWORD) == true } == true) {
                            _pendingSession = flowResponse.session ?: ""
                            _viewEvents.post(CrossSigningSettingsViewEvents.RequestPassword)
                            return
                        } else {
                            // can't do this from here
                            _viewEvents.post(CrossSigningSettingsViewEvents.Failure(Throwable("You cannot do that from mobile")))

                            setState {
                                copy(isUploadingKeys = false)
                            }
                            return
                        }
                    }
                }

                _viewEvents.post(CrossSigningSettingsViewEvents.Failure(failure))

                setState {
                    copy(isUploadingKeys = false)
                }
            }
        })
    }

    companion object : MvRxViewModelFactory<CrossSigningSettingsViewModel, CrossSigningSettingsViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: CrossSigningSettingsViewState): CrossSigningSettingsViewModel? {
            val fragment: CrossSigningSettingsFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.viewModelFactory.create(state)
        }
    }
}
