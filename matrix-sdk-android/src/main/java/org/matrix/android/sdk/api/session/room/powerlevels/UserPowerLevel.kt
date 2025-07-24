/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.powerlevels

sealed interface UserPowerLevel : Comparable<UserPowerLevel> {
    data object Infinite : UserPowerLevel

    @JvmInline
    value class Value(val value: Int) : UserPowerLevel

    override fun compareTo(other: UserPowerLevel): Int {
        return when (this) {
            Infinite -> when (other) {
                Infinite -> 0
                is Value -> 1
            }
            is Value -> when (other) {
                Infinite -> -1
                is Value -> value.compareTo(other.value)
            }
        }
    }

    companion object {
        val User = Value(0)
        val Moderator = Value(50)
        val Admin = Value(100)
        val SuperAdmin = Value(150)
    }
}
