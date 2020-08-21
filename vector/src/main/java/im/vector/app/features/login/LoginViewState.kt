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

package im.vector.app.features.login

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.PersistState
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import im.vector.app.core.extensions.appendParamToUrl
import org.matrix.android.sdk.api.auth.SSO_REDIRECT_PATH
import org.matrix.android.sdk.api.auth.SSO_REDIRECT_URL_PARAM

data class LoginViewState(
        val asyncLoginAction: Async<Unit> = Uninitialized,
        val asyncHomeServerLoginFlowRequest: Async<Unit> = Uninitialized,
        val asyncResetPassword: Async<Unit> = Uninitialized,
        val asyncResetMailConfirmed: Async<Unit> = Uninitialized,
        val asyncRegistration: Async<Unit> = Uninitialized,

        // User choices
        @PersistState
        val serverType: ServerType = ServerType.Unknown,
        @PersistState
        val signMode: SignMode = SignMode.Unknown,
        @PersistState
        val resetPasswordEmail: String? = null,
        @PersistState
        val homeServerUrl: String? = null,
        // For SSO session recovery
        @PersistState
        val deviceId: String? = null,

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
                // Keep loading when it is success because of the delay to switch to the next Activity
                || asyncLoginAction is Success
    }

    fun isUserLogged(): Boolean {
        return asyncLoginAction is Success
    }

    fun getSsoUrl(): String {
        return buildString {
            append(homeServerUrl?.trim { it == '/' })
            append(SSO_REDIRECT_PATH)
            // Set a redirect url we will intercept later
            appendParamToUrl(SSO_REDIRECT_URL_PARAM, VECTOR_REDIRECT_URL)
            deviceId?.takeIf { it.isNotBlank() }?.let {
                // But https://github.com/matrix-org/synapse/issues/5755
                appendParamToUrl("device_id", it)
            }
        }
    }

    companion object {
        // Note that the domain can be displayed to the user for confirmation that he trusts it. So use a human readable string
        private const val VECTOR_REDIRECT_URL = "element://element"
    }
}
