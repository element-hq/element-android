/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.auth.registration

import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.auth.registration.RegisterThreePid

/**
 * Container to store the data when a three pid is in validation step
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
        fun from(threePid: RegisterThreePid,
                 addThreePidRegistrationResponse: AddThreePidRegistrationResponse,
                 registrationParams: RegistrationParams): ThreePidData {
            return when (threePid) {
                is RegisterThreePid.Email  ->
                    ThreePidData(threePid.email, "", "", addThreePidRegistrationResponse, registrationParams)
                is RegisterThreePid.Msisdn ->
                    ThreePidData("", threePid.msisdn, threePid.countryCode, addThreePidRegistrationResponse, registrationParams)
            }
        }
    }
}
