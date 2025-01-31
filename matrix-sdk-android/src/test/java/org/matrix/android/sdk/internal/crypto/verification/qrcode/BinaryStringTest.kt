/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
     * I want to put bytes to a String, and vice versa.
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
