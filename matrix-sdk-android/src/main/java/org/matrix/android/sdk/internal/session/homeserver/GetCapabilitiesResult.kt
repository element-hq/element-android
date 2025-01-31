/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.homeserver

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.util.JsonDict

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
         * True if the user can change their password, false otherwise.
         */
        @Json(name = "m.change_password")
        val changePassword: BooleanCapability? = null,

        /**
         * Capability to indicate if the user can change their display name.
         * True if the user can change their display name, false otherwise.
         */
        @Json(name = "m.set_displayname")
        val changeDisplayName: BooleanCapability? = null,

        /**
         * Capability to indicate if the user can change their avatar.
         * True if the user can change their avatar, false otherwise.
         */
        @Json(name = "m.set_avatar_url")
        val changeAvatar: BooleanCapability? = null,
        /**
         * Capability to indicate if the user can change add, remove or change 3PID associations.
         * True if the user can change their 3PID associations, false otherwise.
         */
        @Json(name = "m.3pid_changes")
        val change3pid: BooleanCapability? = null,
        /**
         * This capability describes the default and available room versions a server supports, and at what level of stability.
         * Clients should make use of this capability to determine if users need to be encouraged to upgrade their rooms.
         */
        @Json(name = "m.room_versions")
        val roomVersions: RoomVersions? = null,
        /**
         * Capability to indicate if the server supports MSC3440 Threading.
         * True if the user can use m.thread relation, false otherwise.
         */
        @Json(name = "m.thread")
        val threads: BooleanCapability? = null
)

@JsonClass(generateAdapter = true)
internal data class BooleanCapability(
        /**
         * Required.
         */
        @Json(name = "enabled")
        val enabled: Boolean?
)

@JsonClass(generateAdapter = true)
internal data class RoomVersions(
        /**
         * Required. The default room version the server is using for new rooms.
         */
        @Json(name = "default")
        val default: String?,

        /**
         * Required. A detailed description of the room versions the server supports.
         */
        @Json(name = "available")
        val available: JsonDict? = null,

        /**
         * Example:
         * <pre>
         *  "room_capabilities": {
         *      "knock" : {
         *              "preferred": "7",
         *              "support" : ["7"]
         *      },
         *      "restricted" : {
         *              "preferred": "9",
         *              "support" : ["8", "9"]
         *      }
         * }
         * </pre>.
         */
        @Json(name = "org.matrix.msc3244.room_capabilities")
        val roomCapabilities: JsonDict? = null
)
