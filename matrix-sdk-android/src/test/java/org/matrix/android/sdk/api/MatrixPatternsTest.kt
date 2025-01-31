/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
