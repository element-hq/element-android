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

package org.matrix.android.sdk.api.rendezvous.channels

import android.util.Base64
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.rendezvous.RendezvousChannel
import org.matrix.android.sdk.api.rendezvous.RendezvousFailureReason
import org.matrix.android.sdk.api.rendezvous.RendezvousTransport
import org.matrix.android.sdk.api.rendezvous.model.RendezvousError
import org.matrix.android.sdk.api.rendezvous.model.SecureRendezvousChannelAlgorithm
import org.matrix.android.sdk.api.util.MatrixJsonParser
import org.matrix.android.sdk.internal.crypto.verification.getDecimalCodeRepresentation
import org.matrix.olm.OlmSAS
import timber.log.Timber
import java.security.SecureRandom
import java.util.LinkedList
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 *  Implements X25519 ECDH key agreement and AES-256-GCM encryption channel as per MSC3903:
 *  https://github.com/matrix-org/matrix-spec-proposals/pull/3903
 */
class ECDHRendezvousChannel(
        override var transport: RendezvousTransport,
        private val algorithm: SecureRendezvousChannelAlgorithm,
        theirPublicKeyBase64: String?,
) : RendezvousChannel {
    companion object {
        private const val ALGORITHM_SPEC = "AES/GCM/NoPadding"
        private const val KEY_SPEC = "AES"
        private val TAG = LoggerTag(ECDHRendezvousChannel::class.java.simpleName, LoggerTag.RENDEZVOUS).value
    }

    @JsonClass(generateAdapter = true)
    internal data class ECDHPayload(
            val algorithm: SecureRendezvousChannelAlgorithm? = null,
            val key: String? = null,
            val ciphertext: String? = null,
            val iv: String? = null,
    )

    private val olmSASMutex = Mutex()
    private var olmSAS: OlmSAS?
    private val ourPublicKey: ByteArray
    private val ecdhAdapter = MatrixJsonParser.getMoshi().adapter(ECDHPayload::class.java)
    private var theirPublicKey: ByteArray? = null
    private var aesKey: ByteArray? = null

    init {
        theirPublicKeyBase64?.let {
            theirPublicKey = decodeBase64(it)
        }
        olmSAS = OlmSAS()
        ourPublicKey = decodeBase64(olmSAS!!.publicKey)
    }

    fun encodeBase64(input: ByteArray?): String? {
        if (algorithm == SecureRendezvousChannelAlgorithm.ECDH_V2) {
            return Base64.encodeToString(input, Base64.NO_WRAP or Base64.NO_PADDING)
        }
        return Base64.encodeToString(input, Base64.NO_WRAP)
    }

    fun decodeBase64(input: String?): ByteArray {
        // for decoding we aren't concerned about padding
        return Base64.decode(input, Base64.NO_WRAP)
    }

    @Throws(RendezvousError::class)
    override suspend fun connect(): String {
        val sas = olmSAS ?: throw RendezvousError("Channel closed", RendezvousFailureReason.Unknown)
        val isInitiator = theirPublicKey == null

        if (isInitiator) {
            Timber.tag(TAG).i("Waiting for other device to send their public key")
            val res = this.receiveAsPayload() ?: throw RendezvousError("No reply from other device", RendezvousFailureReason.ProtocolError)

            if (res.key == null) {
                throw RendezvousError(
                        "Unsupported algorithm: ${res.algorithm}",
                        RendezvousFailureReason.UnsupportedAlgorithm,
                )
            }
            theirPublicKey = decodeBase64(res.key)
        } else {
            // send our public key unencrypted
            Timber.tag(TAG).i("Sending public key")
            send(
                    ECDHPayload(
                            algorithm = algorithm,
                            key = encodeBase64(ourPublicKey)
                    )
            )
        }

        olmSASMutex.withLock {
            sas.setTheirPublicKey(encodeBase64(theirPublicKey))
            sas.setTheirPublicKey(encodeBase64(theirPublicKey))

            val initiatorKey = encodeBase64(if (isInitiator) ourPublicKey else theirPublicKey)
            val recipientKey = encodeBase64(if (isInitiator) theirPublicKey else ourPublicKey)
            val aesInfo = "${algorithm.value}|$initiatorKey|$recipientKey"

            aesKey = sas.generateShortCode(aesInfo, 32)

            val rawChecksum = sas.generateShortCode(aesInfo, 5)
            return rawChecksum.getDecimalCodeRepresentation(separator = "-")
        }
    }

    private suspend fun send(payload: ECDHPayload) {
        transport.send("application/json".toMediaType(), ecdhAdapter.toJson(payload).toByteArray(Charsets.UTF_8))
    }

    override suspend fun send(data: ByteArray) {
        if (aesKey == null) {
            throw IllegalStateException("Shared secret not established")
        }
        send(encrypt(data))
    }

    private suspend fun receiveAsPayload(): ECDHPayload? {
        transport.receive()?.toString(Charsets.UTF_8)?.let {
            return ecdhAdapter.fromJson(it)
        } ?: return null
    }

    override suspend fun receive(): ByteArray? {
        if (aesKey == null) {
            throw IllegalStateException("Shared secret not established")
        }
        val payload = receiveAsPayload() ?: return null
        return decrypt(payload)
    }

    override suspend fun close() {
        val sas = olmSAS ?: throw IllegalStateException("Channel already closed")
        olmSASMutex.withLock {
            // this does a double release check already so we don't re-check ourselves
            sas.releaseSas()
            olmSAS = null
        }
        transport.close()
    }

    private fun encrypt(plainText: ByteArray): ECDHPayload {
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)

        val cipherText = LinkedList<Byte>()

        val encryptCipher = Cipher.getInstance(ALGORITHM_SPEC)
        val secretKeySpec = SecretKeySpec(aesKey, KEY_SPEC)
        val ivParameterSpec = IvParameterSpec(iv)
        encryptCipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec)
        cipherText.addAll(encryptCipher.update(plainText).toList())
        cipherText.addAll(encryptCipher.doFinal().toList())

        return ECDHPayload(
                ciphertext = encodeBase64(cipherText.toByteArray()),
                iv = encodeBase64(iv)
        )
    }

    private fun decrypt(payload: ECDHPayload): ByteArray {
        val iv = decodeBase64(payload.iv)
        val encryptCipher = Cipher.getInstance(ALGORITHM_SPEC)
        val secretKeySpec = SecretKeySpec(aesKey, KEY_SPEC)
        val ivParameterSpec = IvParameterSpec(iv)
        encryptCipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)

        val plainText = LinkedList<Byte>()
        plainText.addAll(encryptCipher.update(decodeBase64(payload.ciphertext)).toList())
        plainText.addAll(encryptCipher.doFinal().toList())

        return plainText.toByteArray()
    }
}
