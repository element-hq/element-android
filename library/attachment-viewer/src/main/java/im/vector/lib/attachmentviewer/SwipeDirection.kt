/*
 * Copyright 2020-2024 New Vector Ltd.
 * Copyright 2018 stfalcon.com
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.attachmentviewer

sealed class SwipeDirection {
    object NotDetected : SwipeDirection()
    object Up : SwipeDirection()
    object Down : SwipeDirection()
    object Left : SwipeDirection()
    object Right : SwipeDirection()

    companion object {
        fun fromAngle(angle: Double): SwipeDirection {
            return when (angle) {
                in 0.0..45.0 -> Right
                in 45.0..135.0 -> Up
                in 135.0..225.0 -> Left
                in 225.0..315.0 -> Down
                in 315.0..360.0 -> Right
                else -> NotDetected
            }
        }
    }
}
