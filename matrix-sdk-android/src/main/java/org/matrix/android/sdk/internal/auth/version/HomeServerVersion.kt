/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.auth.version

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
            else                -> 0
        }
    }

    companion object {
        internal val pattern = Regex("""[r|v](\d+)\.(\d+)\.(\d+)""")

        internal fun parse(value: String): HomeServerVersion? {
            val result = pattern.matchEntire(value) ?: return null
            return HomeServerVersion(
                    major = result.groupValues[1].toInt(),
                    minor = result.groupValues[2].toInt(),
                    patch = result.groupValues[3].toInt()
            )
        }

        val r0_0_0 = HomeServerVersion(major = 0, minor = 0, patch = 0)
        val r0_1_0 = HomeServerVersion(major = 0, minor = 1, patch = 0)
        val r0_2_0 = HomeServerVersion(major = 0, minor = 2, patch = 0)
        val r0_3_0 = HomeServerVersion(major = 0, minor = 3, patch = 0)
        val r0_4_0 = HomeServerVersion(major = 0, minor = 4, patch = 0)
        val r0_5_0 = HomeServerVersion(major = 0, minor = 5, patch = 0)
        val r0_6_0 = HomeServerVersion(major = 0, minor = 6, patch = 0)
        val v1_3_0 = HomeServerVersion(major = 1, minor = 3, patch = 0)
    }
}
