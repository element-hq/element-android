/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomdirectory

/**
 * This class describes a rooms directory server protocol.
 */
data class RoomDirectoryData(
        /**
         * The server name (might be null).
         * Set null when the server is the current user's homeserver.
         */
        val homeServer: String? = null,

        /**
         * The display name (the server description).
         */
        val displayName: String = MATRIX_PROTOCOL_NAME,

        /**
         * The avatar url.
         */
        val avatarUrl: String? = null,

        /**
         * The third party server identifier.
         */
        val thirdPartyInstanceId: String? = null,

        /**
         * Tell if all the federated servers must be included.
         */
        val includeAllNetworks: Boolean = false
) {

    companion object {
        const val MATRIX_PROTOCOL_NAME = "Matrix"
    }
}
