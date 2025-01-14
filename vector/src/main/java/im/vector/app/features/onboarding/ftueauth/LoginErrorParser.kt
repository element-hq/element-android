/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
