/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.auth.login

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.internal.auth.registration.AuthParams

/**
 * Class to pass parameters to reset the password once a email has been validated.
 */
@JsonClass(generateAdapter = true)
internal data class ResetPasswordMailConfirmed(
        // authentication parameters
        @Json(name = "auth")
        val auth: AuthParams? = null,

        // the new password
        @Json(name = "new_password")
        val newPassword: String? = null,

        @Json(name = "logout_devices")
        val logoutDevices: Boolean? = null
) {
    companion object {
        fun create(clientSecret: String, sid: String, newPassword: String, logoutDevices: Boolean?): ResetPasswordMailConfirmed {
            return ResetPasswordMailConfirmed(
                    auth = AuthParams.createForResetPassword(clientSecret, sid),
                    newPassword = newPassword,
                    logoutDevices = logoutDevices
            )
        }
    }
}
