/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.util

import java.security.MessageDigest
import java.util.Locale

/**
 * Compute a Hash of a String, using md5 algorithm.
 */
fun String.md5() = try {
    val digest = MessageDigest.getInstance("md5")
    digest.update(toByteArray())
    digest.digest()
            .joinToString("") { String.format("%02X", it) }
            .lowercase(Locale.ROOT)
} catch (exc: Exception) {
    // Should not happen, but just in case
    hashCode().toString()
}
