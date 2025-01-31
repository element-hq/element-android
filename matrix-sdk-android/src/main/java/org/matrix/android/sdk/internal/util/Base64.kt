/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.util

/**
 * Base64 URL conversion methods.
 */

internal fun base64UrlToBase64(base64Url: String): String {
    return base64Url.replace('-', '+')
            .replace('_', '/')
}

internal fun base64ToBase64Url(base64: String): String {
    return base64.replace("\n".toRegex(), "")
            .replace("\\+".toRegex(), "-")
            .replace('/', '_')
            .replace("=", "")
}

internal fun base64ToUnpaddedBase64(base64: String): String {
    return base64.replace("\n".toRegex(), "")
            .replace("=", "")
}
