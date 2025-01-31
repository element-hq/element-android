/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.auth.version

import org.amshove.kluent.internal.assertEquals
import org.junit.Test

class HomeServerVersionTest {

    @Test
    fun `given a semantic version, when parsing, then converts to home server version`() {
        val supportedVersions = listOf(
                case("1.5", expected = aVersion(1, 5, 0)),
                case("0.5.1", expected = aVersion(0, 5, 1)),
                case("1.0.0", expected = aVersion(1, 0, 0)),
                case("1.10.3", expected = aVersion(1, 10, 3)),
        ).withPrefixes("v", "r")

        val unsupportedVersions = listOf(
                case("v-1.5.1", expected = null),
                case("1.4.", expected = null),
                case("1.5.1.", expected = null),
                case("r1", expected = null),
                case("a", expected = null),
                case("1a.2b.3c", expected = null),
                case("r", expected = null),
        )

        (supportedVersions + unsupportedVersions).forEach { (input, expected) ->
            val result = HomeServerVersion.parse(input)

            assertEquals(expected, result, "Expected $input to be $expected but got $result")
        }
    }
}

private fun aVersion(major: Int, minor: Int, patch: Int) = HomeServerVersion(major, minor, patch)
private fun case(input: String, expected: HomeServerVersion?) = Case(input, expected)

private fun List<Case>.withPrefixes(vararg prefixes: String) = map { case ->
    prefixes.map { prefix -> case.copy(input = "$prefix${case.input}") }
}.flatten()

private data class Case(val input: String, val expected: HomeServerVersion?)
