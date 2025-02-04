/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding.ftueauth

import im.vector.app.core.resources.StringProvider
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject

class LoginFieldsValidation @Inject constructor(
        private val stringProvider: StringProvider
) {

    fun validate(usernameOrId: String, password: String): LoginValidationResult {
        return LoginValidationResult(usernameOrId, password, validateUsernameOrId(usernameOrId), validatePassword(password))
    }

    private fun validateUsernameOrId(usernameOrId: String): String? {
        val accountError = when {
            usernameOrId.isEmpty() -> stringProvider.getString(CommonStrings.error_empty_field_enter_user_name)
            else -> null
        }
        return accountError
    }

    private fun validatePassword(password: String): String? {
        val passwordError = when {
            password.isEmpty() -> stringProvider.getString(CommonStrings.error_empty_field_your_password)
            else -> null
        }
        return passwordError
    }
}

fun LoginValidationResult.onValid(action: (String, String) -> Unit): LoginValidationResult {
    when {
        usernameOrIdError == null && passwordError == null -> action(usernameOrId, password)
    }
    return this
}

fun LoginValidationResult.onUsernameOrIdError(action: (String) -> Unit): LoginValidationResult {
    usernameOrIdError?.let(action)
    return this
}

fun LoginValidationResult.onPasswordError(action: (String) -> Unit): LoginValidationResult {
    passwordError?.let(action)
    return this
}
