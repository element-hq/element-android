/*
 * Copyright 2019-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.extensions

/**
 * Ex: "https://matrix.org/" -> "matrix.org".
 */
fun String?.toReducedUrl(keepSchema: Boolean = false): String {
    return (this ?: "")
            .run { if (keepSchema) this else substringAfter("://") }
            .trim { it == '/' }
}
