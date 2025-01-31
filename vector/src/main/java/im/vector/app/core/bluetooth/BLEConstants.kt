/*
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.bluetooth

import java.nio.ByteBuffer
import java.util.UUID

typealias DeviceAddress = String

fun uShortToBytes(x: UShort): ByteArray {
    val buffer: ByteBuffer = ByteBuffer.allocate(java.lang.Short.BYTES)
    buffer.putShort(x.toShort())
    return buffer.array()
}

fun bytesToUShort(bytes: ByteArray): UShort {
    val buffer: ByteBuffer = ByteBuffer.allocate(java.lang.Short.BYTES)
    buffer.put(bytes.sliceArray(0 until java.lang.Short.BYTES))
    buffer.flip()
    return buffer.getUShort()
}

fun ByteBuffer.getUShort() =
        (((this[0].toUInt() and 0xFFu) shl 8) or
                (this[1].toUInt() and 0xFFu)).toUShort()

fun String.decodeHex(): ByteArray {
    check(length % 2 == 0) { "Hex string must have an even length"}

    return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
}

typealias PublicKey = String
fun PublicKey.toPublicKeyBytes(): PublicKeyBytes {
    return this.decodeHex()
}

typealias PublicKeyBytes = ByteArray
fun PublicKeyBytes.toPublicKey(): PublicKey {
    return this.joinToString("") { "%02x".format(it) }
}

object BLEConstants {
    val SERVICE_UUID: UUID = UUID.fromString("a2fda8dd-d250-4a64-8b9a-248f50b93c64")
    val PSM_UUID: UUID = UUID.fromString("15d4151b-1008-41c0-85f2-950facf8a3cd")
    const val PSM_CHARACTERISTIC_SIZE = 20
    const val PSM_CHARACTERISTIC_KEY_SIZE = PSM_CHARACTERISTIC_SIZE - 2
}
