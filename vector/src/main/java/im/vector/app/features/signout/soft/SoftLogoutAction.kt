/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.features.signout.soft

import im.vector.app.core.platform.VectorViewModelAction
import org.matrix.android.sdk.api.auth.data.Credentials

sealed class SoftLogoutAction : VectorViewModelAction {
    // In case of failure to get the login flow
    object RetryLoginFlow : SoftLogoutAction()

    // For password entering management
    data class PasswordChanged(val password: String) : SoftLogoutAction()
    data class SignInAgain(val password: String) : SoftLogoutAction()

    // For signing again with SSO
    data class WebLoginSuccess(val credentials: Credentials) : SoftLogoutAction()

    // To clear the current session
    object ClearData : SoftLogoutAction()
}
