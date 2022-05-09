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

import androidx.core.text.isDigitsOnly
import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import javax.inject.Inject

typealias LoginValidationResult = Pair<String?, String?>

class LoginFieldsValidation @Inject constructor(
        private val stringProvider: StringProvider
) {

    fun validate(usernameOrId: String, password: String, isMatrixOrg: Boolean): Pair<String?, String?> {
        return validateUsernameOrId(usernameOrId, isMatrixOrg) to validatePassword(password)
    }

    private fun validateUsernameOrId(usernameOrId: String, isMatrixOrg: Boolean): String? {
        val accountError = when {
            usernameOrId.isEmpty()                                                   -> stringProvider.getString(R.string.error_empty_field_choose_user_name)
            isNumericOnlyUserIdForbidden(isMatrixOrg) && usernameOrId.isDigitsOnly() -> stringProvider.getString(R.string.error_forbidden_digits_only_username)
            else                                                                     -> null
        }
        return accountError
    }

    private fun isNumericOnlyUserIdForbidden(isMatrixOrg: Boolean) = isMatrixOrg

    private fun validatePassword(password: String): String? {
        val passwordError = when {
            password.isEmpty() -> stringProvider.getString(R.string.error_empty_field_choose_password)
            else               -> null
        }
        return passwordError
    }
}

fun LoginValidationResult.onValid(action: () -> Unit) {
    when {
        first != null && second != null -> action()
    }
}
