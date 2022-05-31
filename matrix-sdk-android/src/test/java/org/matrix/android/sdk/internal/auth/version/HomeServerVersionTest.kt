/*
 * Copyright (c) 2022 New Vector Ltd
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

import org.amshove.kluent.internal.assertEquals
import org.junit.Test

class HomeServerVersionTest {

    @Test
    fun `given a semantic version, when parsing, then converts to home server version`() {
        val supportedVersions = listOf(
                case("1.5", expected = aVersion(1, 5, 0)),
                case("0.5.1", expected = aVersion(0, 5, 1)),
                case("1.0.0", expected = aVersion(1, 0, 0)),
                case("1.10.3", expected = aVersion(1, 10, 3))
        ).withPrefixes("v", "r")

        val unsupportedVersions = listOf(
                case("v-1.5.1", expected = null),
                case("r1", expected = null),
                case("a", expected = null),
                case("1a.2b.3c", expected = null),
                case("r", expected = null)
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
