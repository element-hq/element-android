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

package im.vector.matrix.android.api.failure

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * This data class holds the error defined by the matrix specifications.
 * You shouldn't have to instantiate it.
 */
@JsonClass(generateAdapter = true)
data class MatrixError(
        @Json(name = "errcode") val code: String,
        @Json(name = "error") val message: String,

        @Json(name = "consent_uri") val consentUri: String? = null,
        // For RESOURCE_LIMIT_EXCEEDED
        @Json(name = "limit_type") val limitType: String? = null,
        @Json(name = "admin_contact") val adminUri: String? = null,
        // For LIMIT_EXCEEDED
        @Json(name = "retry_after_ms") val retryAfterMillis: Long? = null,
        // For UNAUTHORIZED
        @Json(name = "soft_logout") val isSoftLogout: Boolean? = null
) {

    companion object {
        const val FORBIDDEN = "M_FORBIDDEN"
        const val UNKNOWN = "M_UNKNOWN"
        const val UNKNOWN_TOKEN = "M_UNKNOWN_TOKEN"
        const val MISSING_TOKEN = "M_MISSING_TOKEN"
        const val BAD_JSON = "M_BAD_JSON"
        const val NOT_JSON = "M_NOT_JSON"
        const val NOT_FOUND = "M_NOT_FOUND"
        const val LIMIT_EXCEEDED = "M_LIMIT_EXCEEDED"
        const val USER_IN_USE = "M_USER_IN_USE"
        const val ROOM_IN_USE = "M_ROOM_IN_USE"
        const val BAD_PAGINATION = "M_BAD_PAGINATION"
        const val UNAUTHORIZED = "M_UNAUTHORIZED"
        const val OLD_VERSION = "M_OLD_VERSION"
        const val UNRECOGNIZED = "M_UNRECOGNIZED"

        const val LOGIN_EMAIL_URL_NOT_YET = "M_LOGIN_EMAIL_URL_NOT_YET"
        const val THREEPID_AUTH_FAILED = "M_THREEPID_AUTH_FAILED"
        // Error code returned by the server when no account matches the given 3pid
        const val THREEPID_NOT_FOUND = "M_THREEPID_NOT_FOUND"
        const val THREEPID_IN_USE = "M_THREEPID_IN_USE"
        const val SERVER_NOT_TRUSTED = "M_SERVER_NOT_TRUSTED"
        const val TOO_LARGE = "M_TOO_LARGE"
        const val M_CONSENT_NOT_GIVEN = "M_CONSENT_NOT_GIVEN"
        const val RESOURCE_LIMIT_EXCEEDED = "M_RESOURCE_LIMIT_EXCEEDED"
        const val WRONG_ROOM_KEYS_VERSION = "M_WRONG_ROOM_KEYS_VERSION"

        // Possible value for "limit_type"
        const val LIMIT_TYPE_MAU = "monthly_active_user"
    }
}
