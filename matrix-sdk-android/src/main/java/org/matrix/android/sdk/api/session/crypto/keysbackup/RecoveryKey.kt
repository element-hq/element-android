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

package org.matrix.android.sdk.api.session.crypto.keysbackup

import org.matrix.android.sdk.internal.crypto.keysbackup.util.base58decode
import org.matrix.android.sdk.internal.crypto.keysbackup.util.base58encode
import kotlin.experimental.xor

/**
 * See https://github.com/uhoreg/matrix-doc/blob/e2e_backup/proposals/1219-storing-megolm-keys-serverside.md
 */

private const val CHAR_0 = 0x8B.toByte()
private const val CHAR_1 = 0x01.toByte()

private const val RECOVERY_KEY_LENGTH = 2 + 32 + 1

/**
 * Tell if the format of the recovery key is correct
 *
 * @param recoveryKey
 * @return true if the format of the recovery key is correct
 */
fun isValidRecoveryKey(recoveryKey: String?): Boolean {
    return extractCurveKeyFromRecoveryKey(recoveryKey) != null
}

/**
 * Compute recovery key from curve25519 key
 *
 * @param curve25519Key
 * @return the recovery key
 */
fun computeRecoveryKey(curve25519Key: ByteArray): String {
    // Append header and parity
    val data = ByteArray(curve25519Key.size + 3)

    // Header
    data[0] = CHAR_0
    data[1] = CHAR_1

    // Copy key and compute parity
    var parity: Byte = CHAR_0 xor CHAR_1

    for (i in curve25519Key.indices) {
        data[i + 2] = curve25519Key[i]
        parity = parity xor curve25519Key[i]
    }

    // Parity
    data[curve25519Key.size + 2] = parity

    // Do not add white space every 4 chars, it's up to the presenter to do it
    return base58encode(data)
}

/**
 * Please call [.isValidRecoveryKey] and ensure it returns true before calling this method
 *
 * @param recoveryKey the recovery key
 * @return curveKey, or null in case of error
 */
fun extractCurveKeyFromRecoveryKey(recoveryKey: String?): ByteArray? {
    if (recoveryKey == null) {
        return null
    }

    // Remove any space
    val spaceFreeRecoveryKey = recoveryKey.replace("""\s""".toRegex(), "")

    val b58DecodedKey = base58decode(spaceFreeRecoveryKey)

    // Check length
    if (b58DecodedKey.size != RECOVERY_KEY_LENGTH) {
        return null
    }

    // Check first byte
    if (b58DecodedKey[0] != CHAR_0) {
        return null
    }

    // Check second byte
    if (b58DecodedKey[1] != CHAR_1) {
        return null
    }

    // Check parity
    var parity: Byte = 0

    for (i in 0 until RECOVERY_KEY_LENGTH) {
        parity = parity xor b58DecodedKey[i]
    }

    if (parity != 0.toByte()) {
        return null
    }

    // Remove header and parity bytes
    val result = ByteArray(b58DecodedKey.size - 3)

    for (i in 2 until b58DecodedKey.size - 1) {
        result[i - 2] = b58DecodedKey[i]
    }

    return result
}
