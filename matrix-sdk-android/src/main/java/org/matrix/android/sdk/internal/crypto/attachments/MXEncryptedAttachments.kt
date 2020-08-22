/*
 * Copyright 2016 OpenMarket Ltd
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

package org.matrix.android.sdk.internal.crypto.attachments

import android.util.Base64
import org.matrix.android.sdk.internal.crypto.model.rest.EncryptedFileInfo
import org.matrix.android.sdk.internal.crypto.model.rest.EncryptedFileKey
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

internal object MXEncryptedAttachments {
    private const val CRYPTO_BUFFER_SIZE = 32 * 1024
    private const val CIPHER_ALGORITHM = "AES/CTR/NoPadding"
    private const val SECRET_KEY_SPEC_ALGORITHM = "AES"
    private const val MESSAGE_DIGEST_ALGORITHM = "SHA-256"

    /***
     * Encrypt an attachment stream.
     * @param attachmentStream the attachment stream. Will be closed after this method call.
     * @param mimetype the mime type
     * @return the encryption file info
     */
    fun encryptAttachment(attachmentStream: InputStream, mimetype: String?): EncryptionResult {
        val t0 = System.currentTimeMillis()
        val secureRandom = SecureRandom()

        // generate a random iv key
        // Half of the IV is random, the lower order bits are zeroed
        // such that the counter never wraps.
        // See https://github.com/matrix-org/matrix-ios-kit/blob/3dc0d8e46b4deb6669ed44f72ad79be56471354c/MatrixKit/Models/Room/MXEncryptedAttachments.m#L75
        val initVectorBytes = ByteArray(16) { 0.toByte() }

        val ivRandomPart = ByteArray(8)
        secureRandom.nextBytes(ivRandomPart)

        System.arraycopy(ivRandomPart, 0, initVectorBytes, 0, ivRandomPart.size)

        val key = ByteArray(32)
        secureRandom.nextBytes(key)

        ByteArrayOutputStream().use { outputStream ->
            val encryptCipher = Cipher.getInstance(CIPHER_ALGORITHM)
            val secretKeySpec = SecretKeySpec(key, SECRET_KEY_SPEC_ALGORITHM)
            val ivParameterSpec = IvParameterSpec(initVectorBytes)
            encryptCipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec)

            val messageDigest = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM)

            val data = ByteArray(CRYPTO_BUFFER_SIZE)
            var read: Int
            var encodedBytes: ByteArray

            attachmentStream.use { inputStream ->
                read = inputStream.read(data)
                while (read != -1) {
                    encodedBytes = encryptCipher.update(data, 0, read)
                    messageDigest.update(encodedBytes, 0, encodedBytes.size)
                    outputStream.write(encodedBytes)
                    read = inputStream.read(data)
                }
            }

            // encrypt the latest chunk
            encodedBytes = encryptCipher.doFinal()
            messageDigest.update(encodedBytes, 0, encodedBytes.size)
            outputStream.write(encodedBytes)

            return EncryptionResult(
                    encryptedFileInfo = EncryptedFileInfo(
                            url = null,
                            mimetype = mimetype,
                            key = EncryptedFileKey(
                                    alg = "A256CTR",
                                    ext = true,
                                    key_ops = listOf("encrypt", "decrypt"),
                                    kty = "oct",
                                    k = base64ToBase64Url(Base64.encodeToString(key, Base64.DEFAULT))
                            ),
                            iv = Base64.encodeToString(initVectorBytes, Base64.DEFAULT).replace("\n", "").replace("=", ""),
                            hashes = mapOf("sha256" to base64ToUnpaddedBase64(Base64.encodeToString(messageDigest.digest(), Base64.DEFAULT))),
                            v = "v2"
                    ),
                    encryptedByteArray = outputStream.toByteArray()
            )
                    .also { Timber.v("Encrypt in ${System.currentTimeMillis() - t0}ms") }
        }
    }

    /**
     * Decrypt an attachment
     *
     * @param attachmentStream  the attachment stream. Will be closed after this method call.
     * @param encryptedFileInfo the encryption file info
     * @return the decrypted attachment stream
     */
    fun decryptAttachment(attachmentStream: InputStream?, encryptedFileInfo: EncryptedFileInfo?): InputStream? {
        if (encryptedFileInfo?.isValid() != true) {
            Timber.e("## decryptAttachment() : some fields are not defined, or invalid key fields")
            return null
        }

        val elementToDecrypt = encryptedFileInfo.toElementToDecrypt()

        return decryptAttachment(attachmentStream, elementToDecrypt)
    }

    /**
     * Decrypt an attachment
     *
     * @param attachmentStream the attachment stream. Will be closed after this method call.
     * @param elementToDecrypt the elementToDecrypt info
     * @return the decrypted attachment stream
     */
    fun decryptAttachment(attachmentStream: InputStream?, elementToDecrypt: ElementToDecrypt?): InputStream? {
        // sanity checks
        if (null == attachmentStream || elementToDecrypt == null) {
            Timber.e("## decryptAttachment() : null stream")
            return null
        }

        val t0 = System.currentTimeMillis()

        ByteArrayOutputStream().use { outputStream ->
            try {
                val key = Base64.decode(base64UrlToBase64(elementToDecrypt.k), Base64.DEFAULT)
                val initVectorBytes = Base64.decode(elementToDecrypt.iv, Base64.DEFAULT)

                val decryptCipher = Cipher.getInstance(CIPHER_ALGORITHM)
                val secretKeySpec = SecretKeySpec(key, SECRET_KEY_SPEC_ALGORITHM)
                val ivParameterSpec = IvParameterSpec(initVectorBytes)
                decryptCipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)

                val messageDigest = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM)

                var read: Int
                val data = ByteArray(CRYPTO_BUFFER_SIZE)
                var decodedBytes: ByteArray

                attachmentStream.use { inputStream ->
                    read = inputStream.read(data)
                    while (read != -1) {
                        messageDigest.update(data, 0, read)
                        decodedBytes = decryptCipher.update(data, 0, read)
                        outputStream.write(decodedBytes)
                        read = inputStream.read(data)
                    }
                }

                // decrypt the last chunk
                decodedBytes = decryptCipher.doFinal()
                outputStream.write(decodedBytes)

                val currentDigestValue = base64ToUnpaddedBase64(Base64.encodeToString(messageDigest.digest(), Base64.DEFAULT))

                if (elementToDecrypt.sha256 != currentDigestValue) {
                    Timber.e("## decryptAttachment() :  Digest value mismatch")
                    return null
                }

                return ByteArrayInputStream(outputStream.toByteArray())
                        .also { Timber.v("Decrypt in ${System.currentTimeMillis() - t0}ms") }
            } catch (oom: OutOfMemoryError) {
                Timber.e(oom, "## decryptAttachment() failed: OOM")
            } catch (e: Exception) {
                Timber.e(e, "## decryptAttachment() failed")
            }
        }

        return null
    }

    /**
     * Base64 URL conversion methods
     */

    private fun base64UrlToBase64(base64Url: String): String {
        return base64Url.replace('-', '+')
                .replace('_', '/')
    }

    internal fun base64ToBase64Url(base64: String): String {
        return base64.replace("\n".toRegex(), "")
                .replace("\\+".toRegex(), "-")
                .replace('/', '_')
                .replace("=", "")
    }

    private fun base64ToUnpaddedBase64(base64: String): String {
        return base64.replace("\n".toRegex(), "")
                .replace("=", "")
    }
}
