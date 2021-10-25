/*
 * Copyright 2020 New Vector Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package org.matrix.android.sdk.api.session.contentscanning

enum class ScanState {
    TRUSTED,
    INFECTED,
    UNKNOWN,
    IN_PROGRESS
}

data class ScanStatusInfo(
        val state : ScanState,
        val scanDateTimestamp: Long?,
        val humanReadableMessage: String?
)
