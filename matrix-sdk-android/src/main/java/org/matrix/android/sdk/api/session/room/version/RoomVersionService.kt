/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.version

interface RoomVersionService {
    /**
     * Return the room version of this room.
     */
    fun getRoomVersion(): String

    /**
     * Upgrade to the given room version.
     * @return the replacement room id
     */
    suspend fun upgradeToVersion(version: String): String

    /**
     * Get the recommended room version for the current homeserver.
     */
    fun getRecommendedVersion(): String

    /**
     * Ask if the user has enough power level to upgrade the room.
     */
    fun userMayUpgradeRoom(userId: String): Boolean

    /**
     * Return true if the current room version is declared unstable by the homeserver.
     */
    fun isUsingUnstableRoomVersion(): Boolean
}
