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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.matrix.android.sdk.MatrixTest
import org.matrix.android.sdk.api.session.crypto.keysbackup.computeRecoveryKey
import org.matrix.android.sdk.api.session.crypto.keysbackup.extractCurveKeyFromRecoveryKey
import org.matrix.android.sdk.api.session.crypto.keysbackup.isValidRecoveryKey

class RecoveryKeyTest : MatrixTest {

    private val curve25519Key = byteArrayOf(
            0x77.toByte(), 0x07.toByte(), 0x6D.toByte(), 0x0A.toByte(), 0x73.toByte(), 0x18.toByte(), 0xA5.toByte(), 0x7D.toByte(),
            0x3C.toByte(), 0x16.toByte(), 0xC1.toByte(), 0x72.toByte(), 0x51.toByte(), 0xB2.toByte(), 0x66.toByte(), 0x45.toByte(),
            0xDF.toByte(), 0x4C.toByte(), 0x2F.toByte(), 0x87.toByte(), 0xEB.toByte(), 0xC0.toByte(), 0x99.toByte(), 0x2A.toByte(),
            0xB1.toByte(), 0x77.toByte(), 0xFB.toByte(), 0xA5.toByte(), 0x1D.toByte(), 0xB9.toByte(), 0x2C.toByte(), 0x2A.toByte())

    @Test
    fun isValidRecoveryKey_valid_true() {
        assertTrue(isValidRecoveryKey("EsTcLW2KPGiFwKEA3As5g5c4BXwkqeeJZJV8Q9fugUMNUE4d"))

        // Space should be ignored
        assertTrue(isValidRecoveryKey("EsTc LW2K PGiF wKEA 3As5 g5c4 BXwk qeeJ ZJV8 Q9fu gUMN UE4d"))

        // All whitespace should be ignored
        assertTrue(isValidRecoveryKey("EsTc LW2K PGiF wKEA 3As5 g5c4\r\nBXwk qeeJ ZJV8 Q9fu gUMN UE4d"))
    }

    @Test
    fun isValidRecoveryKey_null_false() {
        assertFalse(isValidRecoveryKey(null))
    }

    @Test
    fun isValidRecoveryKey_empty_false() {
        assertFalse(isValidRecoveryKey(""))
    }

    @Test
    fun isValidRecoveryKey_wrong_size_false() {
        assertFalse(isValidRecoveryKey("abc"))
    }

    @Test
    fun isValidRecoveryKey_bad_first_byte_false() {
        assertFalse(isValidRecoveryKey("FsTc LW2K PGiF wKEA 3As5 g5c4 BXwk qeeJ ZJV8 Q9fu gUMN UE4d"))
    }

    @Test
    fun isValidRecoveryKey_bad_second_byte_false() {
        assertFalse(isValidRecoveryKey("EqTc LW2K PGiF wKEA 3As5 g5c4 BXwk qeeJ ZJV8 Q9fu gUMN UE4d"))
    }

    @Test
    fun isValidRecoveryKey_bad_parity_false() {
        assertFalse(isValidRecoveryKey("EsTc LW2K PGiF wKEA 3As5 g5c4 BXwk qeeJ ZJV8 Q9fu gUMN UE4e"))
    }

    @Test
    fun computeRecoveryKey_ok() {
        assertEquals("EsTcLW2KPGiFwKEA3As5g5c4BXwkqeeJZJV8Q9fugUMNUE4d", computeRecoveryKey(curve25519Key))
    }

    @Test
    fun extractCurveKeyFromRecoveryKey_ok() {
        assertArrayEquals(curve25519Key, extractCurveKeyFromRecoveryKey("EsTc LW2K PGiF wKEA 3As5 g5c4 BXwk qeeJ ZJV8 Q9fu gUMN UE4d"))
    }
}
