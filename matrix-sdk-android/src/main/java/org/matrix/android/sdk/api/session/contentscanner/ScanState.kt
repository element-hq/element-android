/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.contentscanner

enum class ScanState {
    TRUSTED,
    INFECTED,
    UNKNOWN,
    IN_PROGRESS
}

data class ScanStatusInfo(
        val state: ScanState,
        val scanDateTimestamp: Long?,
        val humanReadableMessage: String?
)
