/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.analytics.plan

import im.vector.app.features.analytics.itf.VectorAnalyticsEvent

// GENERATED FILE, DO NOT EDIT. FOR MORE INFORMATION VISIT
// https://github.com/matrix-org/matrix-analytics-events/

/**
 * Triggered when the user becomes unauthenticated without actually clicking
 * sign out(E.g. Due to expiry of an access token without a way to refresh).
 */
data class UnauthenticatedError(
        /**
         * The error code as defined in matrix spec. The source of this error is
         * from the homeserver.
         */
        val errorCode: ErrorCode,
        /**
         * The reason for the error. The source of this error is from the
         * homeserver, the reason can vary and is subject to change so there is
         * no enum of possible values.
         */
        val errorReason: String,
        /**
         * Whether the auth mechanism is refresh-token-based.
         */
        val refreshTokenAuth: Boolean,
        /**
         * Whether a soft logout or hard logout was triggered.
         */
        val softLogout: Boolean,
) : VectorAnalyticsEvent {

    enum class ErrorCode {
        M_FORBIDDEN,
        M_UNKNOWN,
        M_UNKNOWN_TOKEN,
    }

    override fun getName() = "UnauthenticatedError"

    override fun getProperties(): Map<String, Any>? {
        return mutableMapOf<String, Any>().apply {
            put("errorCode", errorCode.name)
            put("errorReason", errorReason)
            put("refreshTokenAuth", refreshTokenAuth)
            put("softLogout", softLogout)
        }.takeIf { it.isNotEmpty() }
    }
}
