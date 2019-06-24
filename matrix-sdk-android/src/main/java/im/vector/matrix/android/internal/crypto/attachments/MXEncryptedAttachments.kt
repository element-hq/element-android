/*
 * Copyright 2016 OpenMarket Ltd
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

package im.vector.matrix.android.internal.crypto.attachments

import android.text.TextUtils
import android.util.Base64
import im.vector.matrix.android.internal.crypto.model.rest.EncryptedFileInfo
import im.vector.matrix.android.internal.crypto.model.rest.EncryptedFileKey
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object MXEncryptedAttachments {
    private const val CRYPTO_BUFFER_SIZE = 32 * 1024
    private const val CIPHER_ALGORITHM = "AES/CTR/NoPadding"
    private const val SECRET_KEY_SPEC_ALGORITHM = "AES"
    private const val MESSAGE_DIGEST_ALGORITHM = "SHA-256"

    /**
     * Define the result of an encryption file
     */
    data class EncryptionResult(
            var encryptedFileInfo: EncryptedFileInfo,
            var encryptedStream: InputStream
    )

    /***
     * Encrypt an attachment stream.
     * @param attachmentStream the attachment stream
     * @param mimetype the mime type
     * @return the encryption file info
     */
    fun encryptAttachment(attachmentStream: InputStream, mimetype: String): EncryptionResult? {
        val t0 = System.currentTimeMillis()
        val secureRandom = SecureRandom()

        // generate a random iv key
        // Half of the IV is random, the lower order bits are zeroed
        // such that the counter never wraps.
        // See https://github.com/matrix-org/matrix-ios-kit/blob/3dc0d8e46b4deb6669ed44f72ad79be56471354c/MatrixKit/Models/Room/MXEncryptedAttachments.m#L75
        val initVectorBytes = ByteArray(16)
        Arrays.fill(initVectorBytes, 0.toByte())

        val ivRandomPart = ByteArray(8)
        secureRandom.nextBytes(ivRandomPart)

        System.arraycopy(ivRandomPart, 0, initVectorBytes, 0, ivRandomPart.size)

        val key = ByteArray(32)
        secureRandom.nextBytes(key)

        val outStream = ByteArrayOutputStream()

        try {
            val encryptCipher = Cipher.getInstance(CIPHER_ALGORITHM)
            val secretKeySpec = SecretKeySpec(key, SECRET_KEY_SPEC_ALGORITHM)
            val ivParameterSpec = IvParameterSpec(initVectorBytes)
            encryptCipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec)

            val messageDigest = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM)

            val data = ByteArray(CRYPTO_BUFFER_SIZE)
            var read: Int
            var encodedBytes: ByteArray

            read = attachmentStream.read(data)
            while (read != -1) {
                encodedBytes = encryptCipher.update(data, 0, read)
                messageDigest.update(encodedBytes, 0, encodedBytes.size)
                outStream.write(encodedBytes)
                read = attachmentStream.read(data)
            }

            // encrypt the latest chunk
            encodedBytes = encryptCipher.doFinal()
            messageDigest.update(encodedBytes, 0, encodedBytes.size)
            outStream.write(encodedBytes)

            val result = EncryptionResult(
                    encryptedFileInfo = EncryptedFileInfo(
                            url = null,
                            mimetype = mimetype,
                            key = EncryptedFileKey(
                                    alg = "A256CTR",
                                    ext = true,
                                    key_ops = listOf("encrypt", "decrypt"),
                                    kty = "oct",
                                    k = base64ToBase64Url(Base64.encodeToString(key, Base64.DEFAULT))!!
                            ),
                            iv = Base64.encodeToString(initVectorBytes, Base64.DEFAULT).replace("\n", "").replace("=", ""),
                            hashes = mapOf("sha256" to base64ToUnpaddedBase64(Base64.encodeToString(messageDigest.digest(), Base64.DEFAULT))!!),
                            v = "v2"
                    ),
                    encryptedStream = ByteArrayInputStream(outStream.toByteArray())
            )

            outStream.close()

            Timber.v("Encrypt in " + (System.currentTimeMillis() - t0) + " ms")
            return result
        } catch (oom: OutOfMemoryError) {
            Timber.e(oom, "## encryptAttachment failed " + oom.message)
        } catch (e: Exception) {
            Timber.e(e, "## encryptAttachment failed " + e.message)
        }

        try {
            outStream.close()
        } catch (e: Exception) {
            Timber.e(e, "## encryptAttachment() : fail to close outStream")
        }

        return null
    }

    /**
     * Decrypt an attachment
     *
     * @param attachmentStream  the attachment stream
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
     * @param attachmentStream  the attachment stream
     * @param elementToDecrypt the elementToDecrypt info
     * @return the decrypted attachment stream
     */
    fun decryptAttachment(attachmentStream: InputStream?, elementToDecrypt: ElementToDecrypt?): InputStream? {
        // sanity checks
        if (null == attachmentStream || elementToDecrypt == null) {
            Timber.e("## decryptAttachment() : null stream")
            return null
        }

        // detect if there is no data to decrypt
        try {
            if (0 == attachmentStream.available()) {
                return ByteArrayInputStream(ByteArray(0))
            }
        } catch (e: Exception) {
            Timber.e(e, "Fail to retrieve the file size")
        }

        val t0 = System.currentTimeMillis()

        val outStream = ByteArrayOutputStream()

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

            read = attachmentStream.read(data)
            while (read != -1) {
                messageDigest.update(data, 0, read)
                decodedBytes = decryptCipher.update(data, 0, read)
                outStream.write(decodedBytes)
                read = attachmentStream.read(data)
            }

            // decrypt the last chunk
            decodedBytes = decryptCipher.doFinal()
            outStream.write(decodedBytes)

            val currentDigestValue = base64ToUnpaddedBase64(Base64.encodeToString(messageDigest.digest(), Base64.DEFAULT))

            if (!TextUtils.equals(elementToDecrypt.sha256, currentDigestValue)) {
                Timber.e("## decryptAttachment() :  Digest value mismatch")
                outStream.close()
                return null
            }

            val decryptedStream = ByteArrayInputStream(outStream.toByteArray())
            outStream.close()

            Timber.v("Decrypt in " + (System.currentTimeMillis() - t0) + " ms")

            return decryptedStream
        } catch (oom: OutOfMemoryError) {
            Timber.e(oom, "## decryptAttachment() :  failed " + oom.message)
        } catch (e: Exception) {
            Timber.e(e, "## decryptAttachment() :  failed " + e.message)
        }

        try {
            outStream.close()
        } catch (closeException: Exception) {
            Timber.e(closeException, "## decryptAttachment() :  fail to close the file")
        }

        return null
    }

    /**
     * Base64 URL conversion methods
     */

    private fun base64UrlToBase64(base64Url: String?): String? {
        var result = base64Url
        if (null != result) {
            result = result.replace("-".toRegex(), "+")
            result = result.replace("_".toRegex(), "/")
        }

        return result
    }

    private fun base64ToBase64Url(base64: String?): String? {
        var result = base64
        if (null != result) {
            result = result.replace("\n".toRegex(), "")
            result = result.replace("\\+".toRegex(), "-")
            result = result.replace("/".toRegex(), "_")
            result = result.replace("=".toRegex(), "")
        }
        return result
    }

    private fun base64ToUnpaddedBase64(base64: String?): String? {
        var result = base64
        if (null != result) {
            result = result.replace("\n".toRegex(), "")
            result = result.replace("=".toRegex(), "")
        }

        return result
    }
}
