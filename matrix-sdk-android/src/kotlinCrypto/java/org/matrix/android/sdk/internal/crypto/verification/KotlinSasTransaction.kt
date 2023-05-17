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

package org.matrix.android.sdk.internal.crypto.verification

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.api.session.crypto.verification.EmojiRepresentation
import org.matrix.android.sdk.api.session.crypto.verification.SasTransactionState
import org.matrix.android.sdk.api.session.crypto.verification.SasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.SasVerificationTransaction.Companion.KEY_AGREEMENT_V1
import org.matrix.android.sdk.api.session.crypto.verification.SasVerificationTransaction.Companion.KEY_AGREEMENT_V2
import org.matrix.android.sdk.api.session.crypto.verification.SasVerificationTransaction.Companion.KNOWN_AGREEMENT_PROTOCOLS
import org.matrix.android.sdk.api.session.crypto.verification.SasVerificationTransaction.Companion.KNOWN_HASHES
import org.matrix.android.sdk.api.session.crypto.verification.SasVerificationTransaction.Companion.KNOWN_MACS
import org.matrix.android.sdk.api.session.crypto.verification.SasVerificationTransaction.Companion.KNOWN_SHORT_CODES
import org.matrix.android.sdk.api.session.crypto.verification.SasVerificationTransaction.Companion.SAS_MAC_SHA256
import org.matrix.android.sdk.api.session.crypto.verification.SasVerificationTransaction.Companion.SAS_MAC_SHA256_LONGKDF
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationAcceptContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationKeyContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationMacContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationReadyContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationStartContent
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationAccept
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationKey
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationMac
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationReady
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationStart
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_SAS
import org.matrix.olm.OlmSAS
import timber.log.Timber
import java.util.Locale

internal class KotlinSasTransaction(
        private val channel: Channel<VerificationIntent>,
        override val transactionId: String,
        override val otherUserId: String,
        private val myUserId: String,
        private val myTrustedMSK: String?,
        override var otherDeviceId: String?,
        private val myDeviceId: String,
        private val myDeviceFingerprint: String,
        override val isIncoming: Boolean,
        val startReq: ValidVerificationInfoStart.SasVerificationInfoStart? = null,
        val isToDevice: Boolean,
        var state: SasTransactionState,
        val olmSAS: OlmSAS,
) : SasVerificationTransaction {

    override val method: VerificationMethod
        get() = VerificationMethod.SAS

    companion object {

        fun sasStart(inRoom: Boolean, fromDevice: String, requestId: String): VerificationInfoStart {
            return if (inRoom) {
                MessageVerificationStartContent(
                        fromDevice = fromDevice,
                        hashes = KNOWN_HASHES,
                        keyAgreementProtocols = KNOWN_AGREEMENT_PROTOCOLS,
                        messageAuthenticationCodes = KNOWN_MACS,
                        shortAuthenticationStrings = KNOWN_SHORT_CODES,
                        method = VERIFICATION_METHOD_SAS,
                        relatesTo = RelationDefaultContent(
                                type = RelationType.REFERENCE,
                                eventId = requestId
                        ),
                        sharedSecret = null
                )
            } else {
                KeyVerificationStart(
                        fromDevice,
                        VERIFICATION_METHOD_SAS,
                        requestId,
                        KNOWN_AGREEMENT_PROTOCOLS,
                        KNOWN_HASHES,
                        KNOWN_MACS,
                        KNOWN_SHORT_CODES,
                        null
                )
            }
        }

        fun sasAccept(
                inRoom: Boolean,
                requestId: String,
                keyAgreementProtocol: String,
                hash: String,
                commitment: String,
                messageAuthenticationCode: String,
                shortAuthenticationStrings: List<String>,
        ): VerificationInfoAccept {
            return if (inRoom) {
                MessageVerificationAcceptContent.create(
                        requestId,
                        keyAgreementProtocol,
                        hash,
                        commitment,
                        messageAuthenticationCode,
                        shortAuthenticationStrings
                )
            } else {
                KeyVerificationAccept.create(
                        requestId,
                        keyAgreementProtocol,
                        hash,
                        commitment,
                        messageAuthenticationCode,
                        shortAuthenticationStrings
                )
            }
        }

        fun sasReady(
                inRoom: Boolean,
                requestId: String,
                methods: List<String>,
                fromDevice: String,
        ): VerificationInfoReady {
            return if (inRoom) {
                MessageVerificationReadyContent.create(
                        requestId,
                        methods,
                        fromDevice,
                )
            } else {
                KeyVerificationReady(
                        fromDevice = fromDevice,
                        methods = methods,
                        transactionId = requestId,
                )
            }
        }

        fun sasKeyMessage(
                inRoom: Boolean,
                requestId: String,
                pubKey: String,
        ): VerificationInfoKey {
            return if (inRoom) {
                MessageVerificationKeyContent.create(tid = requestId, pubKey = pubKey)
            } else {
                KeyVerificationKey.create(tid = requestId, pubKey = pubKey)
            }
        }

        fun sasMacMessage(
                inRoom: Boolean,
                requestId: String,
                validVerificationInfoMac: ValidVerificationInfoMac
        ): VerificationInfoMac {
            return if (inRoom) {
                MessageVerificationMacContent.create(
                        tid = requestId,
                        keys = validVerificationInfoMac.keys,
                        mac = validVerificationInfoMac.mac
                )
            } else {
                KeyVerificationMac.create(
                        tid = requestId,
                        keys = validVerificationInfoMac.keys,
                        mac = validVerificationInfoMac.mac
                )
            }
        }
    }

    override fun toString(): String {
        return "KotlinSasTransaction(transactionId=$transactionId, state=$state, otherUserId=$otherUserId, otherDeviceId=$otherDeviceId, isToDevice=$isToDevice)"
    }

    // To override finalize(), all you need to do is simply declare it, without using the override keyword:
    protected fun finalize() {
        releaseSAS()
    }

    private fun releaseSAS() {
        // finalization logic
        olmSAS.releaseSas()
    }

    var accepted: ValidVerificationInfoAccept? = null
    var otherKey: String? = null
    var shortCodeBytes: ByteArray? = null
    var myMac: ValidVerificationInfoMac? = null
    var theirMac: ValidVerificationInfoMac? = null
    var verifiedSuccessInfo: MacVerificationResult.Success? = null

    override fun state() = this.state

//    override fun supportsEmoji(): Boolean {
//        return accepted?.shortAuthenticationStrings?.contains(SasMode.EMOJI) == true
//    }

    override fun getEmojiCodeRepresentation(): List<EmojiRepresentation> {
        return shortCodeBytes?.getEmojiCodeRepresentation().orEmpty()
    }

    override fun getDecimalCodeRepresentation(): String? {
        return shortCodeBytes?.getDecimalCodeRepresentation()
    }

    override suspend fun userHasVerifiedShortCode() {
        val deferred = CompletableDeferred<Unit>()
        channel.send(
                VerificationIntent.ActionSASCodeMatches(transactionId, deferred)
        )
        deferred.await()
    }

    override suspend fun acceptVerification() {
        // nop
        // as we are using verification request accept is automatic
    }

    override suspend fun shortCodeDoesNotMatch() {
        val deferred = CompletableDeferred<Unit>()
        channel.send(
                VerificationIntent.ActionSASCodeDoesNotMatch(transactionId, deferred)
        )
        deferred.await()
    }

    override suspend fun cancel() {
        val deferred = CompletableDeferred<Unit>()
        channel.send(
                VerificationIntent.ActionCancel(transactionId, deferred)
        )
        deferred.await()
    }

    override suspend fun cancel(code: CancelCode) {
        val deferred = CompletableDeferred<Unit>()
        channel.send(
                VerificationIntent.ActionCancel(transactionId, deferred)
        )
        deferred.await()
    }

    override fun isToDeviceTransport() = isToDevice

    fun calculateSASBytes(otherKey: String) {
        this.otherKey = otherKey
        olmSAS.setTheirPublicKey(otherKey)
        shortCodeBytes = when (accepted!!.keyAgreementProtocol) {
            KEY_AGREEMENT_V1 -> {
                // (Note: In all of the following HKDF is as defined in RFC 5869, and uses the previously agreed-on hash function as the hash function,
                // the shared secret as the input keying material, no salt, and with the input parameter set to the concatenation of:
                // - the string “MATRIX_KEY_VERIFICATION_SAS”,
                // - the Matrix ID of the user who sent the m.key.verification.start message,
                // - the device ID of the device that sent the m.key.verification.start message,
                // - the Matrix ID of the user who sent the m.key.verification.accept message,
                // - he device ID of the device that sent the m.key.verification.accept message
                // - the transaction ID.
                val sasInfo = buildString {
                    append("MATRIX_KEY_VERIFICATION_SAS")
                    if (isIncoming) {
                        append(otherUserId)
                        append(otherDeviceId)
                        append(myUserId)
                        append(myDeviceId)
                        append(olmSAS.publicKey)
                    } else {
                        append(myUserId)
                        append(myDeviceId)
                        append(otherUserId)
                        append(otherDeviceId)
                    }
                    append(transactionId)
                }
                // decimal: generate five bytes by using HKDF.
                // emoji: generate six bytes by using HKDF.
                olmSAS.generateShortCode(sasInfo, 6)
            }
            KEY_AGREEMENT_V2 -> {
                val sasInfo = buildString {
                    append("MATRIX_KEY_VERIFICATION_SAS|")
                    if (isIncoming) {
                        append(otherUserId).append('|')
                        append(otherDeviceId).append('|')
                        append(otherKey).append('|')
                        append(myUserId).append('|')
                        append(myDeviceId).append('|')
                        append(olmSAS.publicKey).append('|')
                    } else {
                        append(myUserId).append('|')
                        append(myDeviceId).append('|')
                        append(olmSAS.publicKey).append('|')
                        append(otherUserId).append('|')
                        append(otherDeviceId).append('|')
                        append(otherKey).append('|')
                    }
                    append(transactionId)
                }
                olmSAS.generateShortCode(sasInfo, 6)
            }
            else -> {
                // Protocol has been checked earlier
                throw IllegalArgumentException()
            }
        }
    }

    fun computeMyMac(): ValidVerificationInfoMac {
        val baseInfo = buildString {
            append("MATRIX_KEY_VERIFICATION_MAC")
            append(myUserId)
            append(myDeviceId)
            append(otherUserId)
            append(otherDeviceId)
            append(transactionId)
        }

        //  Previously, with SAS verification, the m.key.verification.mac message only contained the user's device key.
        //  It should now contain both the device key and the MSK.
        //  So when Alice and Bob verify with SAS, the verification will verify the MSK.

        val keyMap = HashMap<String, String>()

        val keyId = "ed25519:$myDeviceId"
        val macString = macUsingAgreedMethod(myDeviceFingerprint, baseInfo + keyId)

        if (macString.isNullOrBlank()) {
            // Should not happen
            Timber.e("## SAS verification [$transactionId] failed to send KeyMac, empty key hashes.")
            throw IllegalStateException("Invalid mac for transaction ${transactionId}")
        }

        keyMap[keyId] = macString

        if (myTrustedMSK != null) {
            val crossSigningKeyId = "ed25519:$myTrustedMSK"
            macUsingAgreedMethod(myTrustedMSK, baseInfo + crossSigningKeyId)?.let { mskMacString ->
                keyMap[crossSigningKeyId] = mskMacString
            }
        }

        val keyStrings = macUsingAgreedMethod(keyMap.keys.sorted().joinToString(","), baseInfo + "KEY_IDS")

        if (keyStrings.isNullOrBlank()) {
            // Should not happen
            Timber.e("## SAS verification [$transactionId] failed to send KeyMac, empty key hashes.")
            throw IllegalStateException("Invalid key mac for transaction ${transactionId}")
        }

        return ValidVerificationInfoMac(
                transactionId,
                keyMap,
                keyStrings
        ).also {
            myMac = it
        }
    }

    sealed class MacVerificationResult {

        object MismatchKeys : MacVerificationResult()
        data class MismatchMacDevice(val deviceId: String) : MacVerificationResult()
        object MismatchMacCrossSigning : MacVerificationResult()
        object NoDevicesVerified : MacVerificationResult()

        data class Success(val verifiedDeviceId: List<String>, val otherMskTrusted: Boolean) : MacVerificationResult()
    }

    fun verifyMacs(
            theirMacSafe: ValidVerificationInfoMac,
            otherUserKnownDevices: List<CryptoDeviceInfo>,
            otherMasterKey: String?
    ): MacVerificationResult {
        Timber.v("## SAS verifying macs for id:$transactionId")

        // Bob’s device calculates the HMAC (as above) of its copies of Alice’s keys given in the message (as identified by their key ID),
        // as well as the HMAC of the comma-separated, sorted list of the key IDs given in the message.
        // Bob’s device compares these with the HMAC values given in the m.key.verification.mac message.
        // If everything matches, then consider Alice’s device keys as verified.
        val baseInfo = buildString {
            append("MATRIX_KEY_VERIFICATION_MAC")
            append(otherUserId)
            append(otherDeviceId)
            append(myUserId)
            append(myDeviceId)
            append(transactionId)
        }

        val commaSeparatedListOfKeyIds = theirMacSafe.mac.keys.sorted().joinToString(",")

        val keyStrings = macUsingAgreedMethod(commaSeparatedListOfKeyIds, baseInfo + "KEY_IDS")
        if (theirMacSafe.keys != keyStrings) {
            // WRONG!
            return MacVerificationResult.MismatchKeys
        }

        val verifiedDevices = ArrayList<String>()

        // cannot be empty because it has been validated
        theirMacSafe.mac.keys.forEach { entry ->
            val keyIDNoPrefix = entry.removePrefix("ed25519:")
            val otherDeviceKey = otherUserKnownDevices
                    .firstOrNull { it.deviceId == keyIDNoPrefix }
                    ?.fingerprint()
            if (otherDeviceKey == null) {
                Timber.w("## SAS Verification: Could not find device $keyIDNoPrefix to verify")
                // just ignore and continue
                return@forEach
            }
            val mac = macUsingAgreedMethod(otherDeviceKey, baseInfo + entry)
            if (mac != theirMacSafe.mac[entry]) {
                // WRONG!
                Timber.e("## SAS Verification: mac mismatch for $otherDeviceKey with id $keyIDNoPrefix")
                // cancel(CancelCode.MismatchedKeys)
                return MacVerificationResult.MismatchMacDevice(keyIDNoPrefix)
            }
            verifiedDevices.add(keyIDNoPrefix)
        }

        var otherMasterKeyIsVerified = false
        if (otherMasterKey != null) {
            // Did the user signed his master key
            theirMacSafe.mac.keys.forEach {
                val keyIDNoPrefix = it.removePrefix("ed25519:")
                if (keyIDNoPrefix == otherMasterKey) {
                    // Check the signature
                    val mac = macUsingAgreedMethod(otherMasterKey, baseInfo + it)
                    if (mac != theirMacSafe.mac[it]) {
                        // WRONG!
                        Timber.e("## SAS Verification: mac mismatch for MasterKey with id $keyIDNoPrefix")
                        return MacVerificationResult.MismatchMacCrossSigning
                    } else {
                        otherMasterKeyIsVerified = true
                    }
                }
            }
        }

        // if none of the keys could be verified, then error because the app
        // should be informed about that
        if (verifiedDevices.isEmpty() && !otherMasterKeyIsVerified) {
            Timber.e("## SAS Verification: No devices verified")
            return MacVerificationResult.NoDevicesVerified
        }

        return MacVerificationResult.Success(
                verifiedDevices,
                otherMasterKeyIsVerified
        ).also {
            // store and will persist when transaction is actually done
            verifiedSuccessInfo = it
        }
    }

    private fun macUsingAgreedMethod(message: String, info: String): String? {
        return when (accepted?.messageAuthenticationCode?.lowercase(Locale.ROOT)) {
            SAS_MAC_SHA256_LONGKDF -> olmSAS.calculateMacLongKdf(message, info)
            SAS_MAC_SHA256 -> olmSAS.calculateMac(message, info)
            else -> null
        }
    }
}
