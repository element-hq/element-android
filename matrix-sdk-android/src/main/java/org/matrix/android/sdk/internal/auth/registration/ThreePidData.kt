/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.auth.registration

import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.auth.registration.RegisterThreePid

/**
 * Container to store the data when a three pid is in validation step.
 */
@JsonClass(generateAdapter = true)
internal data class ThreePidData(
        val email: String,
        val msisdn: String,
        val country: String,
        val addThreePidRegistrationResponse: AddThreePidRegistrationResponse,
        val registrationParams: RegistrationParams
) {
    val threePid: RegisterThreePid
        get() {
            return if (email.isNotBlank()) {
                RegisterThreePid.Email(email)
            } else {
                RegisterThreePid.Msisdn(msisdn, country)
            }
        }

    companion object {
        fun from(
                threePid: RegisterThreePid,
                addThreePidRegistrationResponse: AddThreePidRegistrationResponse,
                registrationParams: RegistrationParams
        ): ThreePidData {
            return when (threePid) {
                is RegisterThreePid.Email ->
                    ThreePidData(threePid.email, "", "", addThreePidRegistrationResponse, registrationParams)
                is RegisterThreePid.Msisdn ->
                    ThreePidData("", threePid.msisdn, threePid.countryCode, addThreePidRegistrationResponse, registrationParams)
            }
        }
    }
}
