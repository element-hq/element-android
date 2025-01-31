/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.securestorage

import org.matrix.android.sdk.api.listeners.ProgressListener
import org.matrix.android.sdk.api.session.crypto.keysbackup.extractCurveKeyFromRecoveryKey
import org.matrix.android.sdk.internal.crypto.keysbackup.deriveKey

/** Tag class. */
interface SsssKeySpec

data class RawBytesKeySpec(
        val privateKey: ByteArray
) : SsssKeySpec {

    companion object {

        fun fromPassphrase(passphrase: String, salt: String, iterations: Int, progressListener: ProgressListener?): RawBytesKeySpec {
            return RawBytesKeySpec(
                    privateKey = deriveKey(
                            passphrase,
                            salt,
                            iterations,
                            progressListener
                    )
            )
        }

        fun fromRecoveryKey(recoveryKey: String): RawBytesKeySpec? {
            return extractCurveKeyFromRecoveryKey(recoveryKey)?.let {
                RawBytesKeySpec(
                        privateKey = it
                )
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RawBytesKeySpec

        if (!privateKey.contentEquals(other.privateKey)) return false

        return true
    }

    override fun hashCode(): Int {
        return privateKey.contentHashCode()
    }
}
