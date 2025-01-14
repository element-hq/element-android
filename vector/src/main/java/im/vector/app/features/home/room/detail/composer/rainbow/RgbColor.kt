/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.composer.rainbow

data class RgbColor(
        val r: Int,
        val g: Int,
        val b: Int
)

fun RgbColor.toDashColor(): String {
    return listOf(r, g, b)
            .joinToString(separator = "", prefix = "#") {
                it.toString(16).padStart(2, '0')
            }
}
