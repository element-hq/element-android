/*
 * Copyright (c) 2023 New Vector Ltd
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

package im.vector.app.core.bluetooth

import java.util.UUID

typealias DeviceAddress = String

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
