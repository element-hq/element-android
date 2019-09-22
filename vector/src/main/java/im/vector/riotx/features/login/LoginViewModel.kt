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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import arrow.core.Try
import com.airbnb.mvrx.*
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.Authenticator
import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.auth.data.InteractiveAuthenticationFlow
import im.vector.matrix.android.internal.auth.data.LoginFlowResponse
import im.vector.riotx.core.di.ActiveSessionHolder
import im.vector.riotx.core.extensions.configureAndStart
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.utils.LiveEvent
import im.vector.riotx.features.notifications.PushRuleTriggerListener
import timber.log.Timber

class LoginViewModel @AssistedInject constructor(@Assisted initialState: LoginViewState,
                                                 private val authenticator: Authenticator,
                                                 private val activeSessionHolder: ActiveSessionHolder,
                                                 private val pushRuleTriggerListener: PushRuleTriggerListener)
    : VectorViewModel<LoginViewState>(initialState) {

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

    private var loginConfig: LoginConfig? = null

    private val _navigationLiveData = MutableLiveData<LiveEvent<LoginActivity.Navigation>>()
    val navigationLiveData: LiveData<LiveEvent<LoginActivity.Navigation>>
        get() = _navigationLiveData

    private var homeServerConnectionConfig: HomeServerConnectionConfig? = null
    private var currentTask: Cancelable? = null


    fun handle(action: LoginActions) {
        when (action) {
            is LoginActions.InitWith         -> handleInitWith(action)
            is LoginActions.UpdateHomeServer -> handleUpdateHomeserver(action)
            is LoginActions.Login            -> handleLogin(action)
            is LoginActions.SsoLoginSuccess  -> handleSsoLoginSuccess(action)
            is LoginActions.NavigateTo       -> handleNavigation(action)
        }
    }

    private fun handleInitWith(action: LoginActions.InitWith) {
        loginConfig = action.loginConfig
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
                    onSessionCreated(data)
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

    private fun onSessionCreated(session: Session) {
        activeSessionHolder.setActiveSession(session)
        session.configureAndStart(pushRuleTriggerListener)

        setState {
            copy(
                    asyncLoginAction = Success(Unit)
            )
        }
    }

    private fun handleSsoLoginSuccess(action: LoginActions.SsoLoginSuccess) {
        val homeServerConnectionConfigFinal = homeServerConnectionConfig

        if (homeServerConnectionConfigFinal == null) {
            // Should not happen
            Timber.w("homeServerConnectionConfig is null")
        } else {
            val session = authenticator.createSessionFromSso(action.credentials, homeServerConnectionConfigFinal)

            onSessionCreated(session)
        }
    }


    private fun handleUpdateHomeserver(action: LoginActions.UpdateHomeServer) = withState { state ->

        var newConfig : HomeServerConnectionConfig? = null
        Try {
            val homeServerUri = action.homeServerUrl
            newConfig = HomeServerConnectionConfig.Builder()
                    .withHomeServerUri(homeServerUri)
                    .build()
        }

        //Do not retry if we already have flows for this config -> causes infinite focus loop
        if (newConfig?.homeServerUri?.toString() == homeServerConnectionConfig?.homeServerUri?.toString()
                && state.asyncHomeServerLoginFlowRequest is Success) return@withState

        currentTask?.cancel()
        homeServerConnectionConfig = newConfig

        val homeServerConnectionConfigFinal = homeServerConnectionConfig

        if (homeServerConnectionConfigFinal == null) {
            // This is invalid
            setState {
                copy(
                        asyncHomeServerLoginFlowRequest = Fail(Throwable("Bad format"))
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
                    val loginMode = when {
                        // SSO login is taken first
                        data.flows.any { it.type == InteractiveAuthenticationFlow.TYPE_LOGIN_SSO }      -> LoginMode.Sso
                        data.flows.any { it.type == InteractiveAuthenticationFlow.TYPE_LOGIN_PASSWORD } -> LoginMode.Password
                        else                                                                            -> LoginMode.Unsupported
                    }

                    setState {
                        copy(
                                asyncHomeServerLoginFlowRequest = Success(loginMode)
                        )
                    }
                }
            })

        }
    }

    private fun handleNavigation(action: LoginActions.NavigateTo) {
        _navigationLiveData.postValue(LiveEvent(action.target))
    }

    override fun onCleared() {
        super.onCleared()

        currentTask?.cancel()
    }

    fun getInitialHomeServerUrl(): String? {
        return loginConfig?.homeServerUrl
    }

    fun getHomeServerUrl(): String {
        return homeServerConnectionConfig?.homeServerUri?.toString() ?: ""
    }
}