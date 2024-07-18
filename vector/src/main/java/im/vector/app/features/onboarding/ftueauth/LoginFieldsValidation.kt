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
