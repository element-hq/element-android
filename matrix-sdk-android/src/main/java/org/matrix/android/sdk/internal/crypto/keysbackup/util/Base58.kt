/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 * Copyright 2011 Google Inc.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.keysbackup.util

import java.math.BigInteger

/**
 * Ref: https://github.com/bitcoin-labs/bitcoin-mobile-android/blob/master/src/bitcoinj/java/com/google/bitcoin/core/Base58.java
 *
 *
 * A custom form of base58 is used to encode BitCoin addresses. Note that this is not the same base58 as used by
 * Flickr, which you may see reference to around the internet.
 *
 * Satoshi says: why base-58 instead of standard base-64 encoding?
 *
 *  * Don't want 0OIl characters that look the same in some fonts and
 * could be used to create visually identical looking account numbers.
 *  * A string with non-alphanumeric characters is not as easily accepted as an account number.
 *  * E-mail usually won't line-break if there's no punctuation to break at.
 *  * Doubleclicking selects the whole number as one word if it's all alphanumeric.
 *
 */
private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
private val BASE = BigInteger.valueOf(58)

/**
 * Encode a byte array to a human readable string with base58 chars.
 */
internal fun base58encode(input: ByteArray): String {
    var bi = BigInteger(1, input)
    val s = StringBuffer()
    while (bi >= BASE) {
        val mod = bi.mod(BASE)
        s.insert(0, ALPHABET[mod.toInt()])
        bi = bi.subtract(mod).divide(BASE)
    }
    s.insert(0, ALPHABET[bi.toInt()])
    // Convert leading zeros too.
    for (anInput in input) {
        if (anInput.toInt() == 0) {
            s.insert(0, ALPHABET[0])
        } else {
            break
        }
    }
    return s.toString()
}

/**
 * Decode a base58 String to a byte array.
 */
internal fun base58decode(input: String): ByteArray {
    var result = decodeToBigInteger(input).toByteArray()

    // Remove the first leading zero if any
    if (result[0] == 0.toByte()) {
        result = result.copyOfRange(1, result.size)
    }

    return result
}

private fun decodeToBigInteger(input: String): BigInteger {
    var bi = BigInteger.valueOf(0)
    // Work backwards through the string.
    for (i in input.length - 1 downTo 0) {
        val alphaIndex = ALPHABET.indexOf(input[i])
        bi = bi.add(BigInteger.valueOf(alphaIndex.toLong()).multiply(BASE.pow(input.length - 1 - i)))
    }
    return bi
}
