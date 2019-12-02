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

import com.airbnb.mvrx.*

data class LoginViewState(
        val asyncLoginAction: Async<Unit> = Uninitialized,
        val asyncHomeServerLoginFlowRequest: Async<Unit> = Uninitialized,
        val asyncResetPassword: Async<Unit> = Uninitialized,
        val asyncResetMailConfirmed: Async<Unit> = Uninitialized,
        val asyncRegistration: Async<Unit> = Uninitialized,

        // User choices
        @PersistState
        val serverType: ServerType = ServerType.MatrixOrg,
        @PersistState
        val signMode: SignMode = SignMode.Unknown,
        @PersistState
        val resetPasswordEmail: String? = null,
        @PersistState
        val homeServerUrl: String? = null,

        // Network result
        @PersistState
        val loginMode: LoginMode = LoginMode.Unknown,
        @PersistState
        // Supported types for the login. We cannot use a sealed class for LoginType because it is not serializable
        val loginModeSupportedTypes: List<String> = emptyList()
) : MvRxState {

    fun isLoading(): Boolean {
        return asyncLoginAction is Loading
                || asyncHomeServerLoginFlowRequest is Loading
                || asyncResetPassword is Loading
                || asyncResetMailConfirmed is Loading
                || asyncRegistration is Loading
    }

    fun isUserLogged(): Boolean {
        return asyncLoginAction is Success
    }

    /**
     * Ex: "https://matrix.org/" -> "matrix.org"
     */
    val homeServerUrlSimple: String
        get() = (homeServerUrl ?: "")
                .substringAfter("://")
                .trim { it == '/' }
}
