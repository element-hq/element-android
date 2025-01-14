/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.extensions

@Suppress("UNCHECKED_CAST") // We're casting null failure results to R
inline fun <T, R> Result<T>.andThen(block: (T) -> Result<R>): Result<R> {
    return when (val result = getOrNull()) {
        null -> this as Result<R>
        else -> block(result)
    }
}
