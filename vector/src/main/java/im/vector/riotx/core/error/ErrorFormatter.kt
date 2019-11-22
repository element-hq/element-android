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

package im.vector.riotx.core.error

import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.failure.MatrixError
import im.vector.riotx.R
import im.vector.riotx.core.resources.StringProvider
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

class ErrorFormatter @Inject constructor(private val stringProvider: StringProvider) {

    fun toHumanReadable(failure: Failure): String {
        // Default
        return failure.localizedMessage
    }

    fun toHumanReadable(throwable: Throwable?): String {
        return when (throwable) {
            null                         -> null
            is Failure.NetworkConnection -> {
                when {
                    throwable.ioException is SocketTimeoutException ->
                        stringProvider.getString(R.string.error_network_timeout)
                    throwable.ioException is UnknownHostException   ->
                        // Invalid homeserver?
                        stringProvider.getString(R.string.login_error_unknown_host)
                    else                                            ->
                        stringProvider.getString(R.string.error_no_network)
                }
            }
            is Failure.ServerError       -> {
                when {
                    throwable.error.code == MatrixError.M_CONSENT_NOT_GIVEN  -> {
                        // Special case for terms and conditions
                        stringProvider.getString(R.string.error_terms_not_accepted)
                    }
                    throwable.error.code == MatrixError.FORBIDDEN
                            && throwable.error.message == "Invalid password" -> {
                        stringProvider.getString(R.string.auth_invalid_login_param)
                    }
                    throwable.error.code == MatrixError.USER_IN_USE          -> {
                        stringProvider.getString(R.string.login_signup_error_user_in_use)
                    }
                    throwable.error.code == MatrixError.BAD_JSON             -> {
                        stringProvider.getString(R.string.login_error_bad_json)
                    }
                    throwable.error.code == MatrixError.NOT_JSON             -> {
                        stringProvider.getString(R.string.login_error_not_json)
                    }
                    throwable.error.code == MatrixError.LIMIT_EXCEEDED       -> {
                        limitExceededError(throwable.error)
                    }
                    throwable.error.code == MatrixError.THREEPID_NOT_FOUND   -> {
                        stringProvider.getString(R.string.login_reset_password_error_not_found)
                    }
                    else                                                     -> {
                        throwable.error.message.takeIf { it.isNotEmpty() }
                                ?: throwable.error.code.takeIf { it.isNotEmpty() }
                    }
                }
            }
            else                         -> throwable.localizedMessage
        }
                ?: stringProvider.getString(R.string.unknown_error)
    }

    private fun limitExceededError(error: MatrixError): String {
        val delay = error.retryAfterMillis

        return if (delay == null) {
            stringProvider.getString(R.string.login_error_limit_exceeded)
        } else {
            // Ensure at least 1 second
            val delaySeconds = delay.toInt() / 1000 + 1
            stringProvider.getQuantityString(R.plurals.login_error_limit_exceeded_retry_after, delaySeconds, delaySeconds)
        }
    }
}
