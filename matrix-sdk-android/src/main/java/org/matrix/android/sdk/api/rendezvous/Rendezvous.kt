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
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.api.session.crypto.crosssigning.KEYBACKUP_SECRET_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.MASTER_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.SELF_SIGNING_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.USER_SIGNING_KEY_SSSS_NAME
import org.matrix.android.sdk.api.util.MatrixJsonParser
import org.matrix.android.sdk.api.rendezvous.channels.ECDHRendezvousChannel
import org.matrix.android.sdk.api.rendezvous.model.ECDHRendezvousCode
import org.matrix.android.sdk.api.rendezvous.model.RendezvousIntent
import org.matrix.android.sdk.api.rendezvous.transports.SimpleHttpRendezvousTransport
import timber.log.Timber

internal enum class PayloadType(val value: String) {
    @Json(name = "m.login.start") Start("m.login.start"),
    @Json(name = "m.login.finish") Finish("m.login.finish"),
    @Json(name = "m.login.progress") Progress("m.login.progress")
}

@JsonClass(generateAdapter = true)
internal data class Payload(
        @Json val type: PayloadType,
        @Json val intent: RendezvousIntent? = null,
        @Json val outcome: String? = null,
        @Json val protocols: List<String>? = null,
        @Json val protocol: String? = null,
        @Json val homeserver: String? = null,
        @Json val login_token: String? = null,
        @Json val device_id: String? = null,
        @Json val device_key: String? = null,
        @Json val verifying_device_id: String? = null,
        @Json val verifying_device_key: String? = null,
        @Json val master_key: String? = null
)

private val TAG = LoggerTag(Rendezvous::class.java.simpleName, LoggerTag.RENDEZVOUS).value

/**
 * Implementation of MSC3906 to sign in + E2EE set up using a QR code.
 */
class Rendezvous(
        val channel: RendezvousChannel,
        val theirIntent: RendezvousIntent,
) {
    companion object {
        fun buildChannelFromCode(code: String, onCancelled: (reason: RendezvousFailureReason) -> Unit): Rendezvous {
            val parsed = MatrixJsonParser.getMoshi().adapter(ECDHRendezvousCode::class.java).fromJson(code) ?: throw RuntimeException("Invalid code")

            val transport = SimpleHttpRendezvousTransport(onCancelled, parsed.rendezvous.transport.uri)

            return Rendezvous(
                    ECDHRendezvousChannel(transport, parsed.rendezvous.key),
                    parsed.intent
            )
        }
    }

    private val adapter = MatrixJsonParser.getMoshi().adapter(Payload::class.java)
    // not yet implemented: RendezvousIntent.RECIPROCATE_LOGIN_ON_EXISTING_DEVICE
    val ourIntent: RendezvousIntent = RendezvousIntent.LOGIN_ON_NEW_DEVICE

    private suspend fun areIntentsIncompatible(): Boolean {
        val incompatible = theirIntent == ourIntent

        Timber.tag(TAG).d("ourIntent: $ourIntent, theirIntent: $theirIntent, incompatible: $incompatible")

        if (incompatible) {
            send(Payload(PayloadType.Finish, intent = ourIntent))
            val reason = if (ourIntent == RendezvousIntent.LOGIN_ON_NEW_DEVICE) RendezvousFailureReason.OtherDeviceNotSignedIn else RendezvousFailureReason.OtherDeviceAlreadySignedIn
            channel.cancel(reason)
        }

        return incompatible
    }

    suspend fun startAfterScanningCode(): String? {
        val checksum = channel.connect();

        Timber.tag(TAG).i("Connected to secure channel with checksum: $checksum")

        if (areIntentsIncompatible()) {
            return null
        }

        // get protocols
        Timber.tag(TAG).i("Waiting for protocols");
        val protocolsResponse = receive()

        if (protocolsResponse?.protocols == null || !protocolsResponse.protocols.contains("login_token")) {
            send(Payload(PayloadType.Finish, outcome = "unsupported"))
            Timber.tag(TAG).i("No supported protocol")
            cancel(RendezvousFailureReason.Unknown)
            return null
        }

        send(Payload(PayloadType.Progress, protocol = "login_token"))

        return checksum
    }

    suspend fun waitForLoginOnNewDevice(authenticationService: AuthenticationService): Session? {
        Timber.tag(TAG).i("Waiting for login_token");

        val loginToken = receive()

        if (loginToken?.type == PayloadType.Finish) {
            when (loginToken.outcome) {
                "declined" -> {
                    Timber.tag(TAG).i("Login declined by other device")
                    channel.cancel(RendezvousFailureReason.UserDeclined)
                    return null
                }
                "unsupported" -> {
                    Timber.tag(TAG).i("Not supported")
                    channel.cancel(RendezvousFailureReason.HomeserverLacksSupport)
                    return null
                }
            }
            channel.cancel(RendezvousFailureReason.Unknown)
            return null
        }

        val homeserver = loginToken?.homeserver ?: throw RuntimeException("No homeserver returned")
        val login_token = loginToken.login_token ?: throw RuntimeException("No login token returned")

        Timber.tag(TAG).i("Got login_token: $login_token for $homeserver");

        val hsConfig = HomeServerConnectionConfig(homeServerUri = Uri.parse(homeserver))
        return authenticationService.loginUsingQrLoginToken(hsConfig, login_token)
    }

    suspend fun completeVerificationOnNewDevice(session: Session) {
        val userId = session.myUserId
        val crypto = session.cryptoService()
        val deviceId = crypto.getMyDevice().deviceId
        val deviceKey = crypto.getMyDevice().fingerprint()
        send(Payload(PayloadType.Progress, outcome = "success", device_id = deviceId, device_key = deviceKey))

        // await confirmation of verification

        val verificationResponse = receive()
        val verifyingDeviceId = verificationResponse?.verifying_device_id ?: throw RuntimeException("No verifying device id returned")
        val verifyingDeviceFromServer = crypto.getCryptoDeviceInfo(userId, verifyingDeviceId)
        if (verifyingDeviceFromServer?.fingerprint() != verificationResponse.verifying_device_key) {
            Timber.tag(TAG).w("Verifying device $verifyingDeviceId doesn't match: $verifyingDeviceFromServer")
            return;
        }

        // set other device as verified
        Timber.tag(TAG).i("Setting device $verifyingDeviceId as verified");
        crypto.setDeviceVerification(DeviceTrustLevel(locallyVerified = true, crossSigningVerified = false), userId, verifyingDeviceId)

        // TODO: what do we do with the master key?
//        verificationResponse.master_key ?.let {
//            // set master key as trusted
//            crypto.setDeviceVerification(DeviceTrustLevel(locallyVerified = true, crossSigningVerified = false), userId, it)
//        }

        // request secrets from the verifying device
        Timber.tag(TAG).i("Requesting secrets from $verifyingDeviceId")

        session.sharedSecretStorageService() .let {
            it.requestSecret(MASTER_KEY_SSSS_NAME, verifyingDeviceId)
            it.requestSecret(SELF_SIGNING_KEY_SSSS_NAME, verifyingDeviceId)
            it.requestSecret(USER_SIGNING_KEY_SSSS_NAME, verifyingDeviceId)
            it.requestSecret(KEYBACKUP_SECRET_SSSS_NAME, verifyingDeviceId)
        }
    }

    private suspend fun receive(): Payload? {
        val data = channel.receive()?: return null
        return adapter.fromJson(data.toString(Charsets.UTF_8))
    }

    private suspend fun send(payload: Payload) {
        channel.send(adapter.toJson(payload).toByteArray(Charsets.UTF_8));
    }

    suspend fun cancel(reason: RendezvousFailureReason) {
        channel.cancel(reason)
    }

    suspend fun close() {
        channel.close()
    }
}
