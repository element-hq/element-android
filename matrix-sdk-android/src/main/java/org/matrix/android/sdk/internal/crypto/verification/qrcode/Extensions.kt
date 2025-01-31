/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.verification.qrcode

import org.matrix.android.sdk.api.util.fromBase64
import org.matrix.android.sdk.api.util.toBase64NoPadding
import org.matrix.android.sdk.internal.extensions.toUnsignedInt

// MATRIX
private val prefix = "MATRIX".toByteArray(Charsets.ISO_8859_1)

internal fun QrCodeData.toEncodedString(): String {
    var result = ByteArray(0)

    // MATRIX
    for (i in prefix.indices) {
        result += prefix[i]
    }

    // Version
    result += 2

    // Mode
    result += when (this) {
        is QrCodeData.VerifyingAnotherUser -> 0
        is QrCodeData.SelfVerifyingMasterKeyTrusted -> 1
        is QrCodeData.SelfVerifyingMasterKeyNotTrusted -> 2
    }.toByte()

    // TransactionId length
    val length = transactionId.length
    result += ((length and 0xFF00) shr 8).toByte()
    result += length.toByte()

    // TransactionId
    transactionId.forEach {
        result += it.code.toByte()
    }

    // Keys
    firstKey.fromBase64().forEach {
        result += it
    }
    secondKey.fromBase64().forEach {
        result += it
    }

    // Secret
    sharedSecret.fromBase64().forEach {
        result += it
    }

    return result.toString(Charsets.ISO_8859_1)
}

internal fun String.toQrCodeData(): QrCodeData? {
    val byteArray = toByteArray(Charsets.ISO_8859_1)

    // Size should be min 6 + 1 + 1 + 2 + ? + 32 + 32 + ? = 74 + transactionLength + secretLength

    // Check header
    // MATRIX
    if (byteArray.size < 10) return null

    for (i in prefix.indices) {
        if (byteArray[i] != prefix[i]) {
            return null
        }
    }

    var cursor = prefix.size // 6

    // Version
    if (byteArray[cursor] != 2.toByte()) {
        return null
    }
    cursor++

    // Get mode
    val mode = byteArray[cursor].toInt()
    cursor++

    // Get transaction length, Big Endian format
    val msb = byteArray[cursor].toUnsignedInt()
    val lsb = byteArray[cursor + 1].toUnsignedInt()

    val transactionLength = msb.shl(8) + lsb

    cursor++
    cursor++

    val secretLength = byteArray.size - 74 - transactionLength

    // ensure the secret length is 8 bytes min
    if (secretLength < 8) {
        return null
    }

    val transactionId = byteArray.copyOfRange(cursor, cursor + transactionLength).toString(Charsets.ISO_8859_1)
    cursor += transactionLength
    val key1 = byteArray.copyOfRange(cursor, cursor + 32).toBase64NoPadding()
    cursor += 32
    val key2 = byteArray.copyOfRange(cursor, cursor + 32).toBase64NoPadding()
    cursor += 32
    val secret = byteArray.copyOfRange(cursor, byteArray.size).toBase64NoPadding()

    return when (mode) {
        0 -> QrCodeData.VerifyingAnotherUser(transactionId, key1, key2, secret)
        1 -> QrCodeData.SelfVerifyingMasterKeyTrusted(transactionId, key1, key2, secret)
        2 -> QrCodeData.SelfVerifyingMasterKeyNotTrusted(transactionId, key1, key2, secret)
        else -> null
    }
}
