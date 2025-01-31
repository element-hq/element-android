/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.keysbackup.util

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.MatrixTest

@FixMethodOrder(MethodSorters.JVM)
class Base58Test : MatrixTest {

    @Test
    fun encode() {
        //  Example comes from https://github.com/keis/base58
        assertEquals("StV1DL6CwTryKyV", base58encode("hello world".toByteArray()))
    }

    @Test
    fun decode() {
        //  Example comes from https://github.com/keis/base58
        assertArrayEquals("hello world".toByteArray(), base58decode("StV1DL6CwTryKyV"))
    }

    @Test
    fun encode_curve25519() {
        // Encode a 32 bytes key
        assertEquals(
                "4F85ZySpwyY6FuH7mQYyyr5b8nV9zFRBLj92AJa37sMr",
                base58encode(("0123456789" + "0123456789" + "0123456789" + "01").toByteArray())
        )
    }

    @Test
    fun decode_curve25519() {
        assertArrayEquals(
                ("0123456789" + "0123456789" + "0123456789" + "01").toByteArray(),
                base58decode("4F85ZySpwyY6FuH7mQYyyr5b8nV9zFRBLj92AJa37sMr")
        )
    }
}
