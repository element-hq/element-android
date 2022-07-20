/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class MatrixPatternsTest {

    @Test
    fun `given user id cases, when checking isUserId, then returns expected`() {
        val cases = listOf(
                UserIdCase("foobar", isUserId = false),
                UserIdCase("@foobar", isUserId = false),
                UserIdCase("foobar@matrix.org", isUserId = false),
                UserIdCase("@foobar: matrix.org", isUserId = false),
                UserIdCase("@foobar:matrix.org", isUserId = true),
        )

        cases.forEach { (input, expected) ->
            MatrixPatterns.isUserId(input) shouldBeEqualTo expected
        }
    }

    @Test
    fun `given matrix id cases, when extracting userName, then returns expected`() {
        val cases = listOf(
                MatrixIdCase("foobar", userName = null),
                MatrixIdCase("@foobar", userName = null),
                MatrixIdCase("foobar@matrix.org", userName = null),
                MatrixIdCase("@foobar: matrix.org", userName = null),
                MatrixIdCase("foobar:matrix.org", userName = null),
                MatrixIdCase("@foobar:matrix.org", userName = "foobar"),
        )

        cases.forEach { (input, expected) ->
            MatrixPatterns.extractUserNameFromId(input) shouldBeEqualTo expected
        }
    }
}

private data class UserIdCase(val input: String, val isUserId: Boolean)
private data class MatrixIdCase(val input: String, val userName: String?)
