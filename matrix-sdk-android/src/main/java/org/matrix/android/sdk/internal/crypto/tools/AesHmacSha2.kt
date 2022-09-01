/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.tools

import org.matrix.android.sdk.api.session.securestorage.SharedSecretStorageError
import org.matrix.android.sdk.api.util.fromBase64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and

internal object AesHmacSha2 {

    class EncryptionInfo(
            val cipherRawBytes: ByteArray,
            val mac: ByteArray,
            val initializationVector: ByteArray
    )

    /**
     * Encryption algorithm aes-hmac-sha2
     * Secrets are encrypted using AES-CTR-256 and MACed using HMAC-SHA-256. The data is encrypted and MACed as follows:
     *
     * Given the secret storage key, generate 64 bytes by performing an HKDF with SHA-256 as the hash, a salt of 32 bytes
     * of 0, and with the secret name as the info.
     *
     * The first 32 bytes are used as the AES key, and the next 32 bytes are used as the MAC key
     *
     * Generate 16 random bytes, set bit 63 to 0 (in order to work around differences in AES-CTR implementations), and use
     * this as the AES initialization vector.
     * This becomes the iv property, encoded using base64.
     *
     * Encrypt the data using AES-CTR-256 using the AES key generated above.
     *
     * This encrypted data, encoded using base64, becomes the ciphertext property.
     *
     * Pass the raw encrypted data (prior to base64 encoding) through HMAC-SHA-256 using the MAC key generated above.
     * The resulting MAC is base64-encoded and becomes the mac property.
     * (We use AES-CTR to match file encryption and key exports.)
     */
    @Throws
    fun encrypt(privateKey: ByteArray, secretName: String, clearDataBase64: String, ivString: String? = null): EncryptionInfo {
        val pseudoRandomKey = HkdfSha256.deriveSecret(
                privateKey,
                zeroByteArray(32),
                secretName.toByteArray(),
                64
        )

        // The first 32 bytes are used as the AES key, and the next 32 bytes are used as the MAC key
        val aesKey = pseudoRandomKey.copyOfRange(0, 32)
        val macKey = pseudoRandomKey.copyOfRange(32, 64)

        val iv = if (ivString != null) {
            ivString.fromBase64()
        } else {
            val secureRandom = SecureRandom()
            ByteArray(16).apply {
                secureRandom.nextBytes(this)
            }
        }
        // clear bit 63 of the salt to stop us hitting the 64-bit counter boundary
        // (which would mean we wouldn't be able to decrypt on Android). The loss
        // of a single bit of salt is a price we have to pay.
        iv[9] = iv[9] and 0x7f

        val cipher = Cipher.getInstance("AES/CTR/NoPadding")

        val secretKeySpec = SecretKeySpec(aesKey, "AES")
        val ivParameterSpec = IvParameterSpec(iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec)
        // secret are not that big, just do Final
        val cipherBytes = cipher.doFinal(clearDataBase64.toByteArray())
        require(cipherBytes.isNotEmpty())

        val macKeySpec = SecretKeySpec(macKey, "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(macKeySpec)
        val digest = mac.doFinal(cipherBytes)

        return EncryptionInfo(
                cipherRawBytes = cipherBytes,
                initializationVector = iv,
                mac = digest
        )
    }

    fun decrypt(privateKey: ByteArray, secretName: String, aesHmacSha2Result: EncryptionInfo): String {
        val pseudoRandomKey = HkdfSha256.deriveSecret(
                privateKey,
                zeroByteArray(32),
                secretName.toByteArray(),
                64
        )

        // The first 32 bytes are used as the AES key, and the next 32 bytes are used as the MAC key
        val aesKey = pseudoRandomKey.copyOfRange(0, 32)
        val macKey = pseudoRandomKey.copyOfRange(32, 64)

        val iv = aesHmacSha2Result.initializationVector
        val cipherRawBytes = aesHmacSha2Result.cipherRawBytes

        val macKeySpec = SecretKeySpec(macKey, "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256").apply { init(macKeySpec) }
        val digest = mac.doFinal(cipherRawBytes)
        if (!aesHmacSha2Result.mac.contentEquals(digest)) {
            throw SharedSecretStorageError.BadMac
        }
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")

        val secretKeySpec = SecretKeySpec(aesKey, "AES")
        val ivParameterSpec = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)
        // secret are not that big, just do Final
        val decryptedSecret = cipher.doFinal(cipherRawBytes)

        require(decryptedSecret.isNotEmpty())

        return String(decryptedSecret, Charsets.UTF_8)
    }

    /** Calculate the MAC for checking the key.
     *
     * @param {ByteArray} [key] the key to use
     * @param {string} [iv] The initialization vector as a base64-encoded string.
     *     If omitted, a random initialization vector will be created.
     * @return [EncryptionInfo] An object that contains, `mac` and `iv` properties.
     */
    fun calculateKeyCheck(key: ByteArray, iv: String?): EncryptionInfo {
        val zerosStr = String(zeroByteArray(32))
        return encrypt(key, "", zerosStr, iv)
    }

    private fun zeroByteArray(size: Int): ByteArray = ByteArray(size) { 0.toByte() }
}
