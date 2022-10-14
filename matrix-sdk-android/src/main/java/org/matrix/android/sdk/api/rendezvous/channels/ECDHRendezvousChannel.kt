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
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.MediaType.Companion.toMediaType
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.rendezvous.RendezvousChannel
import org.matrix.android.sdk.api.rendezvous.RendezvousFailureReason
import org.matrix.android.sdk.api.rendezvous.RendezvousTransport
import org.matrix.android.sdk.api.rendezvous.model.ECDHRendezvous
import org.matrix.android.sdk.api.rendezvous.model.ECDHRendezvousCode
import org.matrix.android.sdk.api.rendezvous.model.RendezvousError
import org.matrix.android.sdk.api.rendezvous.model.RendezvousIntent
import org.matrix.android.sdk.api.rendezvous.model.SecureRendezvousChannelAlgorithm
import org.matrix.android.sdk.api.rendezvous.transports.SimpleHttpRendezvousTransportDetails
import org.matrix.android.sdk.api.util.MatrixJsonParser
import org.matrix.android.sdk.internal.extensions.toUnsignedInt
import org.matrix.olm.OlmSAS
import java.security.SecureRandom
import java.util.LinkedList
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@JsonClass(generateAdapter = true)
data class ECDHPayload(
        @Json val algorithm: SecureRendezvousChannelAlgorithm? = null,
        @Json val key: String? = null,
        @Json val ciphertext: String? = null,
        @Json val iv: String? = null
)

private val TAG = LoggerTag(ECDHRendezvousChannel::class.java.simpleName, LoggerTag.RENDEZVOUS).value

fun getDecimalCodeRepresentation(byteArray: ByteArray): String {
    val b0 = byteArray[0].toUnsignedInt() // need unsigned byte
    val b1 = byteArray[1].toUnsignedInt() // need unsigned byte
    val b2 = byteArray[2].toUnsignedInt() // need unsigned byte
    val b3 = byteArray[3].toUnsignedInt() // need unsigned byte
    val b4 = byteArray[4].toUnsignedInt() // need unsigned byte
    // (B0 << 5 | B1 >> 3) + 1000
    val first = (b0.shl(5) or b1.shr(3)) + 1000
    // ((B1 & 0x7) << 10 | B2 << 2 | B3 >> 6) + 1000
    val second = ((b1 and 0x7).shl(10) or b2.shl(2) or b3.shr(6)) + 1000
    // ((B3 & 0x3f) << 7 | B4 >> 1) + 1000
    val third = ((b3 and 0x3f).shl(7) or b4.shr(1)) + 1000
    return "$first-$second-$third"
}

const val ALGORITHM_SPEC = "AES/GCM/NoPadding"
const val KEY_SPEC = "AES"

/**
 *  Implements X25519 ECDH key agreement and AES-256-GCM encryption channel as per MSC3903:
 *  https://github.com/matrix-org/matrix-spec-proposals/pull/3903
 */
class ECDHRendezvousChannel(override var transport: RendezvousTransport, theirPublicKeyBase64: String?) : RendezvousChannel {
    private var olmSAS: OlmSAS?
    private val ourPublicKey: ByteArray
    private val ecdhAdapter = MatrixJsonParser.getMoshi().adapter(ECDHPayload::class.java)
    private var theirPublicKey: ByteArray? = null
    private var aesKey: ByteArray? = null

    init {
        theirPublicKeyBase64 ?.let {
            theirPublicKey = Base64.decode(it, Base64.NO_WRAP)
        }
        olmSAS = OlmSAS()
        ourPublicKey = Base64.decode(olmSAS!!.publicKey, Base64.NO_WRAP)
    }

    override suspend fun connect(): String {
        if (olmSAS == null) {
            throw RuntimeException("Channel closed")
        }
        val isInitiator = theirPublicKey == null

        if (isInitiator) {
//            Timber.tag(TAG).i("Waiting for other device to send their public key")
            val res = this.receiveAsPayload() ?: throw RuntimeException("No reply from other device")

            if (res.key == null) {
                throw RendezvousError(
                        "Unsupported algorithm: ${res.algorithm}",
                        RendezvousFailureReason.UnsupportedAlgorithm,
                )
            }
            theirPublicKey = Base64.decode(res.key, Base64.NO_WRAP)
        } else {
            // send our public key unencrypted
//            Timber.tag(TAG).i("Sending public key")
            send(
                    ECDHPayload(
                    algorithm = SecureRendezvousChannelAlgorithm.ECDH_V1,
                    key = Base64.encodeToString(ourPublicKey, Base64.NO_WRAP)
            )
            )
        }

        olmSAS!!.setTheirPublicKey(Base64.encodeToString(theirPublicKey, Base64.NO_WRAP))

        val initiatorKey = Base64.encodeToString(if (isInitiator) ourPublicKey else theirPublicKey, Base64.NO_WRAP)
        val recipientKey = Base64.encodeToString(if (isInitiator) theirPublicKey else ourPublicKey, Base64.NO_WRAP)
        val aesInfo = "${SecureRendezvousChannelAlgorithm.ECDH_V1.value}|$initiatorKey|$recipientKey"

        aesKey = olmSAS!!.generateShortCode(aesInfo, 32)

//        Timber.tag(TAG).i("Our public key: ${Base64.encodeToString(ourPublicKey, Base64.NO_WRAP)}")
//        Timber.tag(TAG).i("Their public key: ${Base64.encodeToString(theirPublicKey, Base64.NO_WRAP)}")
//        Timber.tag(TAG).i("AES info: $aesInfo")
//        Timber.tag(TAG).i("AES key: ${Base64.encodeToString(aesKey, Base64.NO_WRAP)}")

        val rawChecksum = olmSAS!!.generateShortCode(aesInfo, 5)
        return getDecimalCodeRepresentation(rawChecksum)
    }

    private suspend fun send(payload: ECDHPayload) {
        transport.send("application/json".toMediaType(), ecdhAdapter.toJson(payload).toByteArray(Charsets.UTF_8))
    }

    override suspend fun send(data: ByteArray) {
        if (aesKey == null) {
            throw RuntimeException("Shared secret not established")
        }
        send(encrypt(data))
    }

    private suspend fun receiveAsPayload(): ECDHPayload? {
        transport.receive()?.toString(Charsets.UTF_8) ?.let {
            return ecdhAdapter.fromJson(it)
        } ?: return null
    }

    override suspend fun receive(): ByteArray? {
        if (aesKey == null) {
            throw RuntimeException("Shared secret not established")
        }
        val payload = receiveAsPayload() ?: return null
        return decrypt(payload)
    }

    override suspend fun generateCode(intent: RendezvousIntent): ECDHRendezvousCode {
        return ECDHRendezvousCode(
                intent,
                rendezvous = ECDHRendezvous(
                        transport.details() as SimpleHttpRendezvousTransportDetails,
                        SecureRendezvousChannelAlgorithm.ECDH_V1,
                        key = Base64.encodeToString(ourPublicKey, Base64.NO_WRAP)
                )
        )
    }

    override suspend fun cancel(reason: RendezvousFailureReason) {
        try {
            transport.cancel(reason)
        } finally {
            close()
        }
    }

    override suspend fun close() {
        olmSAS?.releaseSas()
        olmSAS = null
    }

    private fun encrypt(plainText: ByteArray): ECDHPayload {
//        Timber.tag(TAG).d("Encrypting: ${plainText.toString(Charsets.UTF_8)}")
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
                ciphertext = Base64.encodeToString(cipherText.toByteArray(), Base64.NO_WRAP),
                iv = Base64.encodeToString(iv, Base64.NO_WRAP)
        )
    }

    private fun decrypt(payload: ECDHPayload): ByteArray {
        val iv = Base64.decode(payload.iv, Base64.NO_WRAP)
        val encryptCipher = Cipher.getInstance(ALGORITHM_SPEC)
        val secretKeySpec = SecretKeySpec(aesKey, KEY_SPEC)
        val ivParameterSpec = IvParameterSpec(iv)
        encryptCipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)

        val plainText = LinkedList<Byte>()
        plainText.addAll(encryptCipher.update(Base64.decode(payload.ciphertext, Base64.NO_WRAP)).toList())
        plainText.addAll(encryptCipher.doFinal().toList())

        val plainTextBytes = plainText.toByteArray()

//        Timber.tag(TAG).d("Decrypted: ${plainTextBytes.toString(Charsets.UTF_8)}")
        return plainTextBytes
    }
}
