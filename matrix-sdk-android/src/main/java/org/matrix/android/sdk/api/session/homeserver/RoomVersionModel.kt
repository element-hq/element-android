/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.homeserver

data class RoomVersionCapabilities(
        val defaultRoomVersion: String,
        val supportedVersion: List<RoomVersionInfo>,
        // Keys are capabilities defined per spec, as for now knock or restricted
        val capabilities: Map<String, RoomCapabilitySupport>?
)

data class RoomVersionInfo(
        val version: String,
        val status: RoomVersionStatus
)

data class RoomCapabilitySupport(
        val preferred: String?,
        val support: List<String>
)

enum class RoomVersionStatus {
    STABLE,
    UNSTABLE
}
