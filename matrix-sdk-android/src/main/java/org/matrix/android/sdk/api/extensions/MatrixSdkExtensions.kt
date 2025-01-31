/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.extensions

import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo

/* ==========================================================================================
 * MXDeviceInfo
 * ========================================================================================== */

fun CryptoDeviceInfo.getFingerprintHumanReadable() = fingerprint()
        ?.chunked(4)
        ?.joinToString(separator = " ")

/* ==========================================================================================
 * DeviceInfo
 * ========================================================================================== */

fun List<DeviceInfo>.sortByLastSeen(): List<DeviceInfo> {
    return this.sortedByDescending { it.lastSeenTs ?: 0 }
}
