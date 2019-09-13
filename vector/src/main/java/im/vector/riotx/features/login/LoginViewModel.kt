/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.riotx.features.login

import arrow.core.Try
import com.airbnb.mvrx.*
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.Authenticator
import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.auth.data.LoginFlowResponse
import im.vector.riotx.core.di.ActiveSessionHolder
import im.vector.riotx.core.extensions.configureAndStart
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.features.notifications.PushRuleTriggerListener

class LoginViewModel @AssistedInject constructor(@Assisted initialState: LoginViewState,
                                                 val authenticator: Authenticator,
                                                 val activeSessionHolder: ActiveSessionHolder,
                                                 val pushRuleTriggerListener: PushRuleTriggerListener) : VectorViewModel<LoginViewState>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: LoginViewState): LoginViewModel
    }

    companion object : MvRxViewModelFactory<LoginViewModel, LoginViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: LoginViewState): LoginViewModel? {
            val activity: LoginActivity = (viewModelContext as ActivityViewModelContext).activity()
            return activity.loginViewModelFactory.create(state)
        }
    }

    init {

    }

    var homeServerConnectionConfig: HomeServerConnectionConfig? = null
    private var currentTask: Cancelable? = null


    fun handle(action: LoginActions) {
        when (action) {
            is LoginActions.UpdateHomeServer -> handleUpdateHomeserver(action)
            is LoginActions.Login            -> handleLogin(action)
        }
    }

    private fun handleLogin(action: LoginActions.Login) {
        val homeServerConnectionConfigFinal = homeServerConnectionConfig

        if (homeServerConnectionConfigFinal == null) {
            setState {
                copy(
                        asyncLoginAction = Fail(Throwable("Bad configuration"))
                )
            }
        } else {
            setState {
                copy(
                        asyncLoginAction = Loading()
                )
            }

            authenticator.authenticate(homeServerConnectionConfigFinal, action.login, action.password, object : MatrixCallback<Session> {
                override fun onSuccess(data: Session) {
                    activeSessionHolder.setActiveSession(data)
                    data.configureAndStart(pushRuleTriggerListener)

                    setState {
                        copy(
                                asyncLoginAction = Success(Unit)
                        )
                    }
                }

                override fun onFailure(failure: Throwable) {
                    setState {
                        copy(
                                asyncLoginAction = Fail(failure)
                        )
                    }
                }
            })
        }
    }

    private fun handleUpdateHomeserver(action: LoginActions.UpdateHomeServer) {
        currentTask?.cancel()

        Try {
            val homeServerUri = action.homeServerUrl
            homeServerConnectionConfig = HomeServerConnectionConfig.Builder()
                    .withHomeServerUri(homeServerUri)
                    .build()
        }

        val homeServerConnectionConfigFinal = homeServerConnectionConfig

        if (homeServerConnectionConfigFinal == null) {
            // This is invalid
            setState {
                copy(
                        asyncHomeServerLoginFlowRequest = Fail(Throwable("Baf format"))
                )
            }
        } else {

            setState {
                copy(
                        asyncHomeServerLoginFlowRequest = Loading()
                )
            }


            currentTask = authenticator.getLoginFlow(homeServerConnectionConfigFinal, object : MatrixCallback<LoginFlowResponse> {
                override fun onFailure(failure: Throwable) {
                    setState {
                        copy(
                                asyncHomeServerLoginFlowRequest = Fail(failure)
                        )
                    }
                }

                override fun onSuccess(data: LoginFlowResponse) {
                    setState {
                        copy(
                                asyncHomeServerLoginFlowRequest = Success(data)
                        )
                    }

                    handleLoginFlowResponse(data)
                }
            })

        }
    }

    private fun handleLoginFlowResponse(loginFlowResponse: LoginFlowResponse) {

    }


    override fun onCleared() {
        super.onCleared()

        currentTask?.cancel()
    }

}