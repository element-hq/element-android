/*
 * Copyright (c) 2020 New Vector Ltd
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

package org.matrix.android.sdk.internal.session.homeserver

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.extensions.orTrue

/**
 * Ref: https://matrix.org/docs/spec/client_server/latest#get-matrix-client-r0-capabilities
 */
@JsonClass(generateAdapter = true)
internal data class GetCapabilitiesResult(
        /**
         * Required. The custom capabilities the server supports, using the Java package naming convention.
         */
        @Json(name = "capabilities")
        val capabilities: Capabilities? = null
)

@JsonClass(generateAdapter = true)
internal data class Capabilities(
        /**
         * Capability to indicate if the user can change their password.
         */
        @Json(name = "m.change_password")
        val changePassword: ChangePassword? = null

        // No need for m.room_versions for the moment
)

@JsonClass(generateAdapter = true)
internal data class ChangePassword(
        /**
         * Required. True if the user can change their password, false otherwise.
         */
        @Json(name = "enabled")
        val enabled: Boolean?
)

// The spec says: If not present, the client should assume that password changes are possible via the API
internal fun GetCapabilitiesResult.canChangePassword(): Boolean {
    return capabilities?.changePassword?.enabled.orTrue()
}
