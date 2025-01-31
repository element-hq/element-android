/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.powerlevels

sealed class Role(open val value: Int) : Comparable<Role> {
    object Admin : Role(100)
    object Moderator : Role(50)
    object Default : Role(0)
    data class Custom(override val value: Int) : Role(value)

    override fun compareTo(other: Role): Int {
        return value.compareTo(other.value)
    }

    companion object {

        // Order matters, default value should be checked after defined roles
        fun fromValue(value: Int, default: Int): Role {
            return when (value) {
                Admin.value -> Admin
                Moderator.value -> Moderator
                Default.value,
                default -> Default
                else -> Custom(value)
            }
        }
    }
}
