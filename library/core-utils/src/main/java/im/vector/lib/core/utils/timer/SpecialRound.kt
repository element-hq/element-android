/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.core.utils.timer

import kotlin.math.round

class SpecialRound(private val step: Long) {
    /**
     * Round the provided value to the nearest multiple of `step`.
     */
    fun round(value: Long): Long {
        return round(value.toDouble() / step).toLong() * step
    }
}
