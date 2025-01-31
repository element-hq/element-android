/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.location

/**
 * Represents the result of an update of live location share like a start or a stop.
 */
sealed interface UpdateLiveLocationShareResult {
    data class Success(val beaconEventId: String) : UpdateLiveLocationShareResult
    data class Failure(val error: Throwable) : UpdateLiveLocationShareResult
}
