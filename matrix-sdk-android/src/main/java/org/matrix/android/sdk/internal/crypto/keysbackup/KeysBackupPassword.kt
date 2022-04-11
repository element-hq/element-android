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

/**
 * Utility to compute a backup private key from a password and vice-versa.
 */
package org.matrix.android.sdk.internal.crypto.keysbackup

import androidx.annotation.WorkerThread
import org.matrix.android.sdk.api.listeners.ProgressListener
import timber.log.Timber
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.xor

private const val SALT_LENGTH = 32
private const val DEFAULT_ITERATION = 500_000

internal data class GeneratePrivateKeyResult(
        // The private key
        val privateKey: ByteArray,
        // the salt used to generate the private key
        val salt: String,
        // number of key derivations done on the generated private key.
        val iterations: Int
)

/**
 * Compute a private key from a password.
 *
 * @param password the password to use.
 *
 * @return a {privateKey, salt, iterations} tuple.
 */
@WorkerThread
internal fun generatePrivateKeyWithPassword(password: String,
                                            progressListener: ProgressListener?
): GeneratePrivateKeyResult {
    val salt = generateSalt()
    val iterations = DEFAULT_ITERATION
    val privateKey = deriveKey(password, salt, iterations, progressListener)

    return GeneratePrivateKeyResult(privateKey, salt, iterations)
}

/**
 * Retrieve a private key from {password, salt, iterations}
 *
 * @param password the password used to generated the private key.
 * @param salt the salt.
 * @param iterations number of key derivations.
 * @param progressListener the progress listener
 *
 * @return a private key.
 */
@WorkerThread
internal fun retrievePrivateKeyWithPassword(password: String,
                                            salt: String,
                                            iterations: Int,
                                            progressListener: ProgressListener? = null): ByteArray {
    return deriveKey(password, salt, iterations, progressListener)
}

/**
 * Compute a private key by deriving a password and a salt strings.
 *
 * @param password the password.
 * @param salt the salt.
 * @param iterations number of derivations.
 * @param progressListener a listener to follow progress.
 *
 * @return a private key.
 */
@WorkerThread
internal fun deriveKey(password: String,
                       salt: String,
                       iterations: Int,
                       progressListener: ProgressListener?): ByteArray {
    // Note: copied and adapted from MXMegolmExportEncryption
    val t0 = System.currentTimeMillis()

    // based on https://en.wikipedia.org/wiki/PBKDF2 algorithm
    // it is simpler than the generic algorithm because the expected key length is equal to the mac key length.
    // noticed as dklen/hlen

    // dklen = 256
    // hlen = 512
    val prf = Mac.getInstance("HmacSHA512")

    prf.init(SecretKeySpec(password.toByteArray(), "HmacSHA512"))

    // 256 bits key length
    val dk = ByteArray(32)
    val uc = ByteArray(64)

    // U1 = PRF(Password, Salt || INT_32_BE(i)) with i goes from 1 to dklen/hlen
    prf.update(salt.toByteArray())
    val int32BE = byteArrayOf(0, 0, 0, 1)
    prf.update(int32BE)
    prf.doFinal(uc, 0)

    // copy to the key
    System.arraycopy(uc, 0, dk, 0, dk.size)

    var lastProgress = -1

    for (index in 2..iterations) {
        // Uc = PRF(Password, Uc-1)
        prf.update(uc)
        prf.doFinal(uc, 0)

        // F(Password, Salt, c, i) = U1 ^ U2 ^ ... ^ Uc
        for (byteIndex in dk.indices) {
            dk[byteIndex] = dk[byteIndex] xor uc[byteIndex]
        }

        val progress = (index + 1) * 100 / iterations
        if (progress != lastProgress) {
            lastProgress = progress
            progressListener?.onProgress(lastProgress, 100)
        }
    }

    Timber.v("KeysBackupPassword: deriveKeys() : " + iterations + " in " + (System.currentTimeMillis() - t0) + " ms")

    return dk
}

/**
 * Generate a 32 chars salt
 */
private fun generateSalt(): String {
    val salt = buildString {
        do {
            append(UUID.randomUUID().toString())
        } while (length < SALT_LENGTH)
    }

    return salt.substring(0, SALT_LENGTH)
}
