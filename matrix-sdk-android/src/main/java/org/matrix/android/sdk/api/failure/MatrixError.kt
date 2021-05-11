/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.failure

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.auth.data.InteractiveAuthenticationFlow

/**
 * This data class holds the error defined by the matrix specifications.
 * You shouldn't have to instantiate it.
 * Ref: https://matrix.org/docs/spec/client_server/latest#api-standards
 */
@JsonClass(generateAdapter = true)
data class MatrixError(
        /** unique string which can be used to handle an error message */
        @Json(name = "errcode") val code: String,
        /** human-readable error message */
        @Json(name = "error") val message: String,

        // For M_CONSENT_NOT_GIVEN
        @Json(name = "consent_uri") val consentUri: String? = null,
        // For M_RESOURCE_LIMIT_EXCEEDED
        @Json(name = "limit_type") val limitType: String? = null,
        @Json(name = "admin_contact") val adminUri: String? = null,
        // For M_LIMIT_EXCEEDED
        @Json(name = "retry_after_ms") val retryAfterMillis: Long? = null,
        // For M_UNKNOWN_TOKEN
        @Json(name = "soft_logout") val isSoftLogout: Boolean? = null,
        // For M_INVALID_PEPPER
        // {"error": "pepper does not match 'erZvr'", "lookup_pepper": "pQgMS", "algorithm": "sha256", "errcode": "M_INVALID_PEPPER"}
        @Json(name = "lookup_pepper") val newLookupPepper: String? = null,

        // For M_FORBIDDEN UIA
        @Json(name = "session")
        val session: String? = null,
        @Json(name = "completed")
        val completedStages: List<String>? = null,
        @Json(name = "flows")
        val flows: List<InteractiveAuthenticationFlow>? = null,
        @Json(name = "params")
        val params: JsonDict? = null
) {

    companion object {
        /** Forbidden access, e.g. joining a room without permission, failed login. */
        const val M_FORBIDDEN = "M_FORBIDDEN"

        /** An unknown error has occurred. */
        const val M_UNKNOWN = "M_UNKNOWN"

        /** The access token specified was not recognised. */
        const val M_UNKNOWN_TOKEN = "M_UNKNOWN_TOKEN"

        /** No access token was specified for the request. */
        const val M_MISSING_TOKEN = "M_MISSING_TOKEN"

        /** Request contained valid JSON, but it was malformed in some way, e.g. missing required keys, invalid values for keys. */
        const val M_BAD_JSON = "M_BAD_JSON"

        /** Request did not contain valid JSON. */
        const val M_NOT_JSON = "M_NOT_JSON"

        /** No resource was found for this request. */
        const val M_NOT_FOUND = "M_NOT_FOUND"

        /** Too many requests have been sent in a short period of time. Wait a while then try again. */
        const val M_LIMIT_EXCEEDED = "M_LIMIT_EXCEEDED"

        /* ==========================================================================================
         * Other error codes the client might encounter are
         * ========================================================================================== */

        /** Encountered when trying to register a user ID which has been taken. */
        const val M_USER_IN_USE = "M_USER_IN_USE"

        /** Sent when the room alias given to the createRoom API is already in use. */
        const val M_ROOM_IN_USE = "M_ROOM_IN_USE"

        /** (Not documented yet) */
        const val M_BAD_PAGINATION = "M_BAD_PAGINATION"

        /** The request was not correctly authorized. Usually due to login failures. */
        const val M_UNAUTHORIZED = "M_UNAUTHORIZED"

        /** (Not documented yet) */
        const val M_OLD_VERSION = "M_OLD_VERSION"

        /** The server did not understand the request. */
        const val M_UNRECOGNIZED = "M_UNRECOGNIZED"

        /** (Not documented yet) */
        const val M_LOGIN_EMAIL_URL_NOT_YET = "M_LOGIN_EMAIL_URL_NOT_YET"

        /** Authentication could not be performed on the third party identifier. */
        const val M_THREEPID_AUTH_FAILED = "M_THREEPID_AUTH_FAILED"

        /** Sent when a threepid given to an API cannot be used because no record matching the threepid was found. */
        const val M_THREEPID_NOT_FOUND = "M_THREEPID_NOT_FOUND"

        /** Sent when a threepid given to an API cannot be used because the same threepid is already in use. */
        const val M_THREEPID_IN_USE = "M_THREEPID_IN_USE"

        /** The client's request used a third party server, eg. identity server, that this server does not trust. */
        const val M_SERVER_NOT_TRUSTED = "M_SERVER_NOT_TRUSTED"

        /** The request or entity was too large. */
        const val M_TOO_LARGE = "M_TOO_LARGE"

        /** (Not documented yet) */
        const val M_CONSENT_NOT_GIVEN = "M_CONSENT_NOT_GIVEN"

        /** The request cannot be completed because the homeserver has reached a resource limit imposed on it. For example,
         *  a homeserver held in a shared hosting environment may reach a resource limit if it starts using too much memory
         *  or disk space. The error MUST have an admin_contact field to provide the user receiving the error a place to reach
         *  out to. Typically, this error will appear on routes which attempt to modify state (eg: sending messages, account
         *  data, etc) and not routes which only read state (eg: /sync, get account data, etc). */
        const val M_RESOURCE_LIMIT_EXCEEDED = "M_RESOURCE_LIMIT_EXCEEDED"

        /** The user ID associated with the request has been deactivated. Typically for endpoints that prove authentication, such as /login. */
        const val M_USER_DEACTIVATED = "M_USER_DEACTIVATED"

        /** Encountered when trying to register a user ID which is not valid. */
        const val M_INVALID_USERNAME = "M_INVALID_USERNAME"

        /** Sent when the initial state given to the createRoom API is invalid. */
        const val M_INVALID_ROOM_STATE = "M_INVALID_ROOM_STATE"

        /** The server does not permit this third party identifier. This may happen if the server only permits,
         *  for example, email addresses from a particular domain. */
        const val M_THREEPID_DENIED = "M_THREEPID_DENIED"

        /** The client's request to create a room used a room version that the server does not support. */
        const val M_UNSUPPORTED_ROOM_VERSION = "M_UNSUPPORTED_ROOM_VERSION"

        /** The client attempted to join a room that has a version the server does not support.
         *  Inspect the room_version property of the error response for the room's version. */
        const val M_INCOMPATIBLE_ROOM_VERSION = "M_INCOMPATIBLE_ROOM_VERSION"

        /** The state change requested cannot be performed, such as attempting to unban a user who is not banned. */
        const val M_BAD_STATE = "M_BAD_STATE"

        /** The room or resource does not permit guests to access it. */
        const val M_GUEST_ACCESS_FORBIDDEN = "M_GUEST_ACCESS_FORBIDDEN"

        /** A Captcha is required to complete the request. */
        const val M_CAPTCHA_NEEDED = "M_CAPTCHA_NEEDED"

        /** The Captcha provided did not match what was expected. */
        const val M_CAPTCHA_INVALID = "M_CAPTCHA_INVALID"

        /** A required parameter was missing from the request. */
        const val M_MISSING_PARAM = "M_MISSING_PARAM"

        /** A parameter that was specified has the wrong value. For example, the server expected an integer and instead received a string. */
        const val M_INVALID_PARAM = "M_INVALID_PARAM"

        /** The resource being requested is reserved by an application service, or the application service making the request has not created the resource. */
        const val M_EXCLUSIVE = "M_EXCLUSIVE"

        /** The user is unable to reject an invite to join the server notices room. See the Server Notices module for more information. */
        const val M_CANNOT_LEAVE_SERVER_NOTICE_ROOM = "M_CANNOT_LEAVE_SERVER_NOTICE_ROOM"

        /** (Not documented yet) */
        const val M_WRONG_ROOM_KEYS_VERSION = "M_WRONG_ROOM_KEYS_VERSION"

        /** (Not documented yet) */
        const val M_WEAK_PASSWORD = "M_WEAK_PASSWORD"

        const val M_TERMS_NOT_SIGNED = "M_TERMS_NOT_SIGNED"

        // For identity service
        const val M_INVALID_PEPPER = "M_INVALID_PEPPER"

        // Possible value for "limit_type"
        const val LIMIT_TYPE_MAU = "monthly_active_user"
    }
}
