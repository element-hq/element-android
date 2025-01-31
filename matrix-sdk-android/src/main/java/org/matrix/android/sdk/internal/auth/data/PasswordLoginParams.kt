/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.auth.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes

/**
 * Ref:
 * - https://matrix.org/docs/spec/client_server/r0.5.0#password-based
 * - https://matrix.org/docs/spec/client_server/r0.5.0#identifier-types
 */
@JsonClass(generateAdapter = true)
internal data class PasswordLoginParams(
        @Json(name = "identifier") val identifier: Map<String, String>,
        @Json(name = "password") val password: String,
        @Json(name = "type") override val type: String,
        @Json(name = "initial_device_display_name") val deviceDisplayName: String?,
        @Json(name = "device_id") val deviceId: String?
) : LoginParams {

    companion object {
        private const val IDENTIFIER_KEY_TYPE = "type"

        private const val IDENTIFIER_KEY_TYPE_USER = "m.id.user"
        private const val IDENTIFIER_KEY_USER = "user"

        private const val IDENTIFIER_KEY_TYPE_THIRD_PARTY = "m.id.thirdparty"
        private const val IDENTIFIER_KEY_MEDIUM = "medium"
        private const val IDENTIFIER_KEY_ADDRESS = "address"

        private const val IDENTIFIER_KEY_TYPE_PHONE = "m.id.phone"
        private const val IDENTIFIER_KEY_COUNTRY = "country"
        private const val IDENTIFIER_KEY_PHONE = "phone"

        fun userIdentifier(
                user: String,
                password: String,
                deviceDisplayName: String?,
                deviceId: String?
        ): PasswordLoginParams {
            return PasswordLoginParams(
                    identifier = mapOf(
                            IDENTIFIER_KEY_TYPE to IDENTIFIER_KEY_TYPE_USER,
                            IDENTIFIER_KEY_USER to user
                    ),
                    password = password,
                    type = LoginFlowTypes.PASSWORD,
                    deviceDisplayName = deviceDisplayName,
                    deviceId = deviceId
            )
        }

        fun thirdPartyIdentifier(
                medium: String,
                address: String,
                password: String,
                deviceDisplayName: String?,
                deviceId: String?
        ): PasswordLoginParams {
            return PasswordLoginParams(
                    identifier = mapOf(
                            IDENTIFIER_KEY_TYPE to IDENTIFIER_KEY_TYPE_THIRD_PARTY,
                            IDENTIFIER_KEY_MEDIUM to medium,
                            IDENTIFIER_KEY_ADDRESS to address
                    ),
                    password = password,
                    type = LoginFlowTypes.PASSWORD,
                    deviceDisplayName = deviceDisplayName,
                    deviceId = deviceId
            )
        }

        fun phoneIdentifier(
                country: String,
                phone: String,
                password: String,
                deviceDisplayName: String?,
                deviceId: String?
        ): PasswordLoginParams {
            return PasswordLoginParams(
                    identifier = mapOf(
                            IDENTIFIER_KEY_TYPE to IDENTIFIER_KEY_TYPE_PHONE,
                            IDENTIFIER_KEY_COUNTRY to country,
                            IDENTIFIER_KEY_PHONE to phone
                    ),
                    password = password,
                    type = LoginFlowTypes.PASSWORD,
                    deviceDisplayName = deviceDisplayName,
                    deviceId = deviceId
            )
        }
    }
}
