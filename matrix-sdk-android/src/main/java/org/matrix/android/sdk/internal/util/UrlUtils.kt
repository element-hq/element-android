/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.util

import java.net.URL

internal fun String.isValidUrl(): Boolean {
    return try {
        URL(this)
        true
    } catch (t: Throwable) {
        false
    }
}

/**
 * Ensure string starts with "http". If it is not the case, "https://" is added, only if the String is not empty
 */
internal fun String.ensureProtocol(): String {
    return when {
        isEmpty() -> this
        !startsWith("http") -> "https://$this"
        else -> this
    }
}

/**
 * Ensure string ends with "/", if not empty.
 */
internal fun String.ensureTrailingSlash(): String {
    return when {
        isEmpty() -> this
        !endsWith("/") -> "$this/"
        else -> this
    }
}
