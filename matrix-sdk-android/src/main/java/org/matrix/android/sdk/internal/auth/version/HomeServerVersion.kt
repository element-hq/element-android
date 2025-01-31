/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.auth.version

import org.matrix.android.sdk.api.extensions.ensureNotEmpty

/**
 * Values will take the form "rX.Y.Z".
 * Ref: https://matrix.org/docs/spec/client_server/latest#get-matrix-client-versions
 */
internal data class HomeServerVersion(
        val major: Int,
        val minor: Int,
        val patch: Int
) : Comparable<HomeServerVersion> {
    override fun compareTo(other: HomeServerVersion): Int {
        return when {
            major > other.major -> 1
            major < other.major -> -1
            minor > other.minor -> 1
            minor < other.minor -> -1
            patch > other.patch -> 1
            patch < other.patch -> -1
            else -> 0
        }
    }

    companion object {
        internal val pattern = Regex("""[r|v](\d+)\.(\d+)(?:\.(\d+))?""")

        internal fun parse(value: String): HomeServerVersion? {
            val result = pattern.matchEntire(value) ?: return null
            return HomeServerVersion(
                    major = result.groupValues[1].toInt(),
                    minor = result.groupValues[2].toInt(),
                    patch = result.groupValues.getOrNull(index = 3)?.ensureNotEmpty()?.toInt() ?: 0
            )
        }

        val r0_0_0 = HomeServerVersion(major = 0, minor = 0, patch = 0)
        val r0_1_0 = HomeServerVersion(major = 0, minor = 1, patch = 0)
        val r0_2_0 = HomeServerVersion(major = 0, minor = 2, patch = 0)
        val r0_3_0 = HomeServerVersion(major = 0, minor = 3, patch = 0)
        val r0_4_0 = HomeServerVersion(major = 0, minor = 4, patch = 0)
        val r0_5_0 = HomeServerVersion(major = 0, minor = 5, patch = 0)
        val r0_6_0 = HomeServerVersion(major = 0, minor = 6, patch = 0)
        val r0_6_1 = HomeServerVersion(major = 0, minor = 6, patch = 1)
        val v1_3_0 = HomeServerVersion(major = 1, minor = 3, patch = 0)
    }
}
