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
