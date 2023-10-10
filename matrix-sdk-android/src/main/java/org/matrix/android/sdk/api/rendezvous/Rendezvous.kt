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

package org.matrix.android.sdk.api.rendezvous

import android.net.Uri
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.rendezvous.channels.ECDHRendezvousChannel
import org.matrix.android.sdk.api.rendezvous.model.ECDHRendezvousCode
import org.matrix.android.sdk.api.rendezvous.model.Outcome
import org.matrix.android.sdk.api.rendezvous.model.Payload
import org.matrix.android.sdk.api.rendezvous.model.PayloadType
import org.matrix.android.sdk.api.rendezvous.model.Protocol
import org.matrix.android.sdk.api.rendezvous.model.RendezvousCode
import org.matrix.android.sdk.api.rendezvous.model.RendezvousError
import org.matrix.android.sdk.api.rendezvous.model.RendezvousIntent
import org.matrix.android.sdk.api.rendezvous.model.RendezvousTransportType
import org.matrix.android.sdk.api.rendezvous.model.SecureRendezvousChannelAlgorithm
import org.matrix.android.sdk.api.rendezvous.transports.SimpleHttpRendezvousTransport
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.api.util.MatrixJsonParser
import timber.log.Timber

/**
 * Implementation of MSC3906 to sign in + E2EE set up using a QR code.
 */
class Rendezvous(
        val channel: RendezvousChannel,
        val theirIntent: RendezvousIntent,
) {
    companion object {
        private val TAG = LoggerTag(Rendezvous::class.java.simpleName, LoggerTag.RENDEZVOUS).value

        @Throws(RendezvousError::class)
        fun buildChannelFromCode(code: String): Rendezvous {
            // we first check that the code is valid JSON and has right high-level structure
            val genericParsed = try {
                // we rely on moshi validating the code and throwing exception if invalid JSON or algorithm doesn't match
                MatrixJsonParser.getMoshi().adapter(RendezvousCode::class.java).fromJson(code)
            } catch (a: Throwable) {
                throw RendezvousError("Malformed code", RendezvousFailureReason.InvalidCode)
            } ?: throw RendezvousError("Code is null", RendezvousFailureReason.InvalidCode)

            // then we check that algorithm is supported
            if (!SecureRendezvousChannelAlgorithm.values().map { it.value }.contains(genericParsed.rendezvous.algorithm)) {
                throw RendezvousError("Unsupported algorithm", RendezvousFailureReason.UnsupportedAlgorithm)
            }

            // and, that the transport is supported
            if (!RendezvousTransportType.values().map { it.value }.contains(genericParsed.rendezvous.transport.type)) {
                throw RendezvousError("Unsupported transport", RendezvousFailureReason.UnsupportedTransport)
            }

            // now that we know the overall structure looks sensible, we rely on moshi validating the code and
            // throwing exception if other parts are invalid
            val supportedParsed = try {
                MatrixJsonParser.getMoshi().adapter(ECDHRendezvousCode::class.java).fromJson(code)
            } catch (a: Throwable) {
                throw RendezvousError("Malformed ECDH rendezvous code", RendezvousFailureReason.InvalidCode)
            } ?: throw RendezvousError("ECDH rendezvous code is null", RendezvousFailureReason.InvalidCode)

            val transport = SimpleHttpRendezvousTransport(supportedParsed.rendezvous.transport.uri)

            return Rendezvous(
                    ECDHRendezvousChannel(transport, supportedParsed.rendezvous.algorithm, supportedParsed.rendezvous.key),
                    supportedParsed.intent
            )
        }
    }

    private val adapter = MatrixJsonParser.getMoshi().adapter(Payload::class.java)

    // not yet implemented: RendezvousIntent.RECIPROCATE_LOGIN_ON_EXISTING_DEVICE
    val ourIntent: RendezvousIntent = RendezvousIntent.LOGIN_ON_NEW_DEVICE

    @Throws(RendezvousError::class)
    private suspend fun checkCompatibility() {
        val incompatible = theirIntent == ourIntent

        Timber.tag(TAG).d("ourIntent: $ourIntent, theirIntent: $theirIntent, incompatible: $incompatible")

        if (incompatible) {
            // inform the other side
            send(Payload(PayloadType.FINISH, intent = ourIntent))
            if (ourIntent == RendezvousIntent.LOGIN_ON_NEW_DEVICE) {
                throw RendezvousError("The other device isn't signed in", RendezvousFailureReason.OtherDeviceNotSignedIn)
            } else {
                throw RendezvousError("The other device is already signed in", RendezvousFailureReason.OtherDeviceAlreadySignedIn)
            }
        }
    }

    @Throws(RendezvousError::class)
    suspend fun startAfterScanningCode(): String {
        val checksum = channel.connect()

        Timber.tag(TAG).i("Connected to secure channel with checksum: $checksum")

        checkCompatibility()

        // get protocols
        Timber.tag(TAG).i("Waiting for protocols")
        val protocolsResponse = receive()

        if (protocolsResponse?.protocols == null || !protocolsResponse.protocols.contains(Protocol.LOGIN_TOKEN)) {
            send(Payload(PayloadType.FINISH, outcome = Outcome.UNSUPPORTED))
            throw RendezvousError("Unsupported protocols", RendezvousFailureReason.UnsupportedHomeserver)
        }

        send(Payload(PayloadType.PROGRESS, protocol = Protocol.LOGIN_TOKEN))

        return checksum
    }

    @Throws(RendezvousError::class)
    suspend fun waitForLoginOnNewDevice(authenticationService: AuthenticationService): Session {
        Timber.tag(TAG).i("Waiting for login_token")

        val loginToken = receive()

        if (loginToken?.type == PayloadType.FINISH) {
            when (loginToken.outcome) {
                Outcome.DECLINED -> {
                    throw RendezvousError("Login declined by other device", RendezvousFailureReason.UserDeclined)
                }
                Outcome.UNSUPPORTED -> {
                    throw RendezvousError("Homeserver lacks support", RendezvousFailureReason.UnsupportedHomeserver)
                }
                else -> {
                    throw RendezvousError("Unknown error", RendezvousFailureReason.Unknown)
                }
            }
        }

        val homeserver = loginToken?.homeserver ?: throw RendezvousError("No homeserver returned", RendezvousFailureReason.ProtocolError)
        val token = loginToken.loginToken ?: throw RendezvousError("No login token returned", RendezvousFailureReason.ProtocolError)

        Timber.tag(TAG).i("Got login_token now attempting to sign in with $homeserver")

        val hsConfig = HomeServerConnectionConfig(homeServerUri = Uri.parse(homeserver))
        return authenticationService.loginUsingQrLoginToken(hsConfig, token)
    }

    @Throws(RendezvousError::class)
    suspend fun completeVerificationOnNewDevice(session: Session) {
        val userId = session.myUserId
        val crypto = session.cryptoService()
        val deviceId = crypto.getMyCryptoDevice().deviceId
        val deviceKey = crypto.getMyCryptoDevice().fingerprint()
        send(Payload(PayloadType.PROGRESS, outcome = Outcome.SUCCESS, deviceId = deviceId, deviceKey = deviceKey))

        try {
            // explicitly download keys for ourself rather than racing with initial sync which might not complete in time
            crypto.downloadKeysIfNeeded(listOf(userId), false)
        } catch (e: Throwable) {
            // log as warning and continue as initial sync might still complete
            Timber.tag(TAG).w(e, "Failed to download keys for self")
        }

        // await confirmation of verification
        val verificationResponse = receive()
        if (verificationResponse?.outcome == Outcome.VERIFIED) {
            val verifyingDeviceId = verificationResponse.verifyingDeviceId
                    ?: throw RendezvousError("No verifying device id returned", RendezvousFailureReason.ProtocolError)
            val verifyingDeviceFromServer = crypto.getCryptoDeviceInfo(userId, verifyingDeviceId)
            if (verifyingDeviceFromServer?.fingerprint() != verificationResponse.verifyingDeviceKey) {
                Timber.tag(TAG).w(
                        "Verifying device $verifyingDeviceId key doesn't match: ${
                            verifyingDeviceFromServer?.fingerprint()
                        } vs ${verificationResponse.verifyingDeviceKey})"
                )
                // inform the other side
                send(Payload(PayloadType.FINISH, outcome = Outcome.E2EE_SECURITY_ERROR))
                throw RendezvousError("Key from verifying device doesn't match", RendezvousFailureReason.E2EESecurityIssue)
            }

            verificationResponse.masterKey?.let { masterKeyFromVerifyingDevice ->
                // verifying device provided us with a master key, so use it to check integrity

                // see what the homeserver told us
                val localMasterKey = crypto.crossSigningService().getMyCrossSigningKeys()?.masterKey()

                // n.b. if no local master key this is a problem, as well as it not matching
                if (localMasterKey?.unpaddedBase64PublicKey != masterKeyFromVerifyingDevice) {
                    Timber.tag(TAG).w("Master key from verifying device doesn't match: $masterKeyFromVerifyingDevice vs $localMasterKey")
                    // inform the other side
                    send(Payload(PayloadType.FINISH, outcome = Outcome.E2EE_SECURITY_ERROR))
                    throw RendezvousError("Master key from verifying device doesn't match", RendezvousFailureReason.E2EESecurityIssue)
                }

                // set other device as verified
                Timber.tag(TAG).i("Setting device $verifyingDeviceId as verified")
                crypto.setDeviceVerification(DeviceTrustLevel(locallyVerified = true, crossSigningVerified = false), userId, verifyingDeviceId)

                Timber.tag(TAG).i("Setting master key as trusted")
                crypto.crossSigningService().markMyMasterKeyAsTrusted()
            } ?: run {
                // set other device as verified anyway
                Timber.tag(TAG).i("Setting device $verifyingDeviceId as verified")
                crypto.setDeviceVerification(DeviceTrustLevel(locallyVerified = true, crossSigningVerified = false), userId, verifyingDeviceId)

                Timber.tag(TAG).i("No master key given by verifying device")
            }

            // request secrets from other sessions.
            Timber.tag(TAG).i("Requesting secrets from other sessions")

            session.sharedSecretStorageService().requestMissingSecrets()
        } else {
            Timber.tag(TAG).i("Not doing verification")
        }
    }

    @Throws(RendezvousError::class)
    private suspend fun receive(): Payload? {
        val data = channel.receive() ?: return null
        val payload = try {
            adapter.fromJson(data.toString(Charsets.UTF_8))
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to parse payload")
            throw RendezvousError("Invalid payload received", RendezvousFailureReason.Unknown)
        }

        return payload
    }

    private suspend fun send(payload: Payload) {
        channel.send(adapter.toJson(payload).toByteArray(Charsets.UTF_8))
    }

    suspend fun close() {
        channel.close()
    }
}
