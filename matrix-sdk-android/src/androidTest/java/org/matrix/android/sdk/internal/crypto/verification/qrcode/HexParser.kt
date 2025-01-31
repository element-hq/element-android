/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
