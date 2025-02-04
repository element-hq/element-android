/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.extensions

fun Int.incrementByOneAndWrap(max: Int, min: Int = 0): Int {
    val incrementedValue = this + 1
    return if (incrementedValue > max) {
        min
    } else {
        incrementedValue
    }
}
