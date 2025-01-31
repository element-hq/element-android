/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.account

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.auth.UserPasswordAuth

/**
 * Class to pass request parameters to update the password.
 */
@JsonClass(generateAdapter = true)
internal data class ChangePasswordParams(
        @Json(name = "auth")
        val auth: UserPasswordAuth? = null,

        @Json(name = "new_password")
        val newPassword: String? = null,

        @Json(name = "logout_devices")
        val logoutDevices: Boolean = true
) {
    companion object {
        fun create(userId: String, oldPassword: String, newPassword: String, logoutAllDevices: Boolean): ChangePasswordParams {
            return ChangePasswordParams(
                    auth = UserPasswordAuth(user = userId, password = oldPassword),
                    newPassword = newPassword,
                    logoutDevices = logoutAllDevices
            )
        }
    }
}
