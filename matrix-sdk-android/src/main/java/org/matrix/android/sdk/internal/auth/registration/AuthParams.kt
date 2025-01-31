/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.auth.registration

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes

/**
 * Open class, parent to all possible authentication parameters.
 */
@JsonClass(generateAdapter = true)
internal data class AuthParams(
        @Json(name = "type")
        val type: String,

        /**
         * Note: session can be null for reset password request.
         */
        @Json(name = "session")
        val session: String?,

        /**
         * parameter for "m.login.recaptcha" type.
         */
        @Json(name = "response")
        val captchaResponse: String? = null,

        /**
         * parameter for "m.login.email.identity" type.
         */
        @Json(name = "threepid_creds")
        val threePidCredentials: ThreePidCredentials? = null
) {

    companion object {
        fun createForCaptcha(session: String, captchaResponse: String): AuthParams {
            return AuthParams(
                    type = LoginFlowTypes.RECAPTCHA,
                    session = session,
                    captchaResponse = captchaResponse
            )
        }

        fun createForEmailIdentity(session: String, threePidCredentials: ThreePidCredentials): AuthParams {
            return AuthParams(
                    type = LoginFlowTypes.EMAIL_IDENTITY,
                    session = session,
                    threePidCredentials = threePidCredentials
            )
        }

        /**
         * Note that there is a bug in Synapse (I have to investigate where), but if we pass LoginFlowTypes.MSISDN,
         * the homeserver answer with the login flow with MatrixError fields and not with a simple MatrixError 401.
         */
        fun createForMsisdnIdentity(session: String, threePidCredentials: ThreePidCredentials): AuthParams {
            return AuthParams(
                    type = LoginFlowTypes.MSISDN,
                    session = session,
                    threePidCredentials = threePidCredentials
            )
        }

        fun createForResetPassword(clientSecret: String, sid: String): AuthParams {
            return AuthParams(
                    type = LoginFlowTypes.EMAIL_IDENTITY,
                    session = null,
                    threePidCredentials = ThreePidCredentials(
                            clientSecret = clientSecret,
                            sid = sid
                    )
            )
        }
    }
}
