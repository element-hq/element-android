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
        assertEquals("4F85ZySpwyY6FuH7mQYyyr5b8nV9zFRBLj92AJa37sMr",
                base58encode(("0123456789" + "0123456789" + "0123456789" + "01").toByteArray()))
    }

    @Test
    fun decode_curve25519() {
        assertArrayEquals(("0123456789" + "0123456789" + "0123456789" + "01").toByteArray(),
                base58decode("4F85ZySpwyY6FuH7mQYyyr5b8nV9zFRBLj92AJa37sMr"))
    }
}
