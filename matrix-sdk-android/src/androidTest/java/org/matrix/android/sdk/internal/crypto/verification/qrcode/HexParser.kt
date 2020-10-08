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

fun hexToByteArray(hex: String): ByteArray {
    // Remove all spaces
    return hex.replace(" ", "")
            .let {
                if (it.length % 2 != 0) "0$it" else it
            }
            .let {
                ByteArray(it.length / 2)
                        .apply {
                            for (i in this.indices) {
                                val index = i * 2
                                val v = it.substring(index, index + 2).toInt(16)
                                this[i] = v.toByte()
                            }
                        }
            }
}
