/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.sync

internal class SyncResponsePostTreatmentAggregator {
    // List of RoomId
    val ephemeralFilesToDelete = mutableListOf<String>()

    // Map of roomId to directUserId
    val directChatsToCheck = mutableMapOf<String, String>()

    // Set of userIds to fetch and update at the end of incremental syncs
    val userIdsToFetch = mutableSetOf<String>()

    // Set of users to call `crossSigningService.checkTrustAndAffectedRoomShields` once per sync
    val userIdsForCheckingTrustAndAffectedRoomShields = mutableSetOf<String>()
}
