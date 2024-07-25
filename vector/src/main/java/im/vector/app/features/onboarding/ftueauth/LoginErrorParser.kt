/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.onboarding.ftueauth

import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.onboarding.ftueauth.LoginErrorParser.LoginErrorResult
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.failure.isHomeserverUnavailable
import org.matrix.android.sdk.api.failure.isInvalidPassword
import org.matrix.android.sdk.api.failure.isInvalidUsername
import org.matrix.android.sdk.api.failure.isLoginEmailUnknown
import javax.inject.Inject

class LoginErrorParser @Inject constructor(
        private val errorFormatter: ErrorFormatter,
        private val stringProvider: StringProvider,
) {
    fun parse(throwable: Throwable, password: String): LoginErrorResult {
        return when {
            throwable.isInvalidUsername() -> {
                LoginErrorResult(throwable, usernameOrIdError = errorFormatter.toHumanReadable(throwable))
            }
            throwable.isLoginEmailUnknown() -> {
                LoginErrorResult(throwable, usernameOrIdError = stringProvider.getString(CommonStrings.login_login_with_email_error))
            }
            throwable.isInvalidPassword() && password.hasSurroundingSpaces() -> {
                LoginErrorResult(throwable, passwordError = stringProvider.getString(CommonStrings.auth_invalid_login_param_space_in_password))
            }
            throwable.isHomeserverUnavailable() -> {
                LoginErrorResult(throwable, usernameOrIdError = stringProvider.getString(CommonStrings.login_error_homeserver_not_found))
            }
            else -> {
                LoginErrorResult(throwable)
            }
        }
    }

    private fun String.hasSurroundingSpaces() = trim() != this

    data class LoginErrorResult(val cause: Throwable, val usernameOrIdError: String? = null, val passwordError: String? = null)
}

fun LoginErrorResult.onUnknown(action: (Throwable) -> Unit): LoginErrorResult {
    when {
        usernameOrIdError == null && passwordError == null -> action(cause)
    }
    return this
}

fun LoginErrorResult.onUsernameOrIdError(action: (String) -> Unit): LoginErrorResult {
    usernameOrIdError?.let(action)
    return this
}

fun LoginErrorResult.onPasswordError(action: (String) -> Unit): LoginErrorResult {
    passwordError?.let(action)
    return this
}
