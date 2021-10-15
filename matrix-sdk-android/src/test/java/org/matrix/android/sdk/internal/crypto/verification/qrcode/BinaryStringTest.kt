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

package org.matrix.android.sdk.internal.crypto.verification.qrcode

import org.amshove.kluent.shouldBeEqualTo
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.MatrixTest

@FixMethodOrder(MethodSorters.JVM)
class BinaryStringTest : MatrixTest {

    /**
     * I want to put bytes to a String, and vice versa
     */
    @Test
    fun testNominalCase() {
        val byteArray = ByteArray(256)
        for (i in byteArray.indices) {
            byteArray[i] = i.toByte() // Random.nextInt(255).toByte()
        }

        val str = byteArray.toString(Charsets.ISO_8859_1)

        str.length shouldBeEqualTo 256

        // Ok convert back to bytearray

        val result = str.toByteArray(Charsets.ISO_8859_1)

        result.size shouldBeEqualTo 256

        for (i in 0..255) {
            result[i] shouldBeEqualTo i.toByte()
            result[i] shouldBeEqualTo byteArray[i]
        }
    }
}
