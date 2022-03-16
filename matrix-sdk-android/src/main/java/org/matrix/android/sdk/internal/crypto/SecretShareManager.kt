/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.crypto.crosssigning.KEYBACKUP_SECRET_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.MASTER_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.SELF_SIGNING_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.USER_SIGNING_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.keyshare.GossipingRequestListener
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.internal.crypto.actions.EnsureOlmSessionsForDevicesAction
import org.matrix.android.sdk.internal.crypto.actions.MessageEncrypter
import org.matrix.android.sdk.internal.crypto.crosssigning.toBase64NoPadding
import org.matrix.android.sdk.internal.crypto.keysbackup.util.extractCurveKeyFromRecoveryKey
import org.matrix.android.sdk.internal.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.internal.crypto.model.event.SecretSendEventContent
import org.matrix.android.sdk.internal.crypto.model.rest.SecretShareRequest
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.crypto.tasks.SendToDeviceTask
import org.matrix.android.sdk.internal.crypto.tasks.createUniqueTxnId
import org.matrix.android.sdk.internal.session.SessionScope
import timber.log.Timber
import javax.inject.Inject

private val loggerTag = LoggerTag("SecretShareManager", LoggerTag.CRYPTO)

@SessionScope
internal class SecretShareManager @Inject constructor(
        private val credentials: Credentials,
        private val cryptoStore: IMXCryptoStore,
        private val cryptoCoroutineScope: CoroutineScope,
        private val messageEncrypter: MessageEncrypter,
        private val ensureOlmSessionsForDevicesAction: EnsureOlmSessionsForDevicesAction,
        private val sendToDeviceTask: SendToDeviceTask,
        private val coroutineDispatchers: MatrixCoroutineDispatchers
) {

    companion object {
        private const val SECRET_SHARE_WINDOW_DURATION = 5 * 60 * 1000 // 5 minutes
    }

    /**
     * Secret gossiping only occurs during a limited window period after interactive verification.
     * We keep track of recent verification in memory for that purpose (no need to persist)
     */
    private val recentlyVerifiedDevices = mutableMapOf<String, Long>()
    private val verifMutex = Mutex()

    /**
     * Secrets are exchanged as part of interactive verification,
     * so we can just store in memory.
     */
    private val outgoingSecretRequests = mutableListOf<SecretShareRequest>()

    // the listeners
    private val gossipingRequestListeners: MutableSet<GossipingRequestListener> = HashSet()

    fun addListener(listener: GossipingRequestListener) {
        synchronized(gossipingRequestListeners) {
            gossipingRequestListeners.add(listener)
        }
    }

    fun removeRoomKeysRequestListener(listener: GossipingRequestListener) {
        synchronized(gossipingRequestListeners) {
            gossipingRequestListeners.remove(listener)
        }
    }

    /**
     * Called when a session has been verified.
     * This information can be used by the manager to decide whether or not to fullfill gossiping requests.
     * This should be called as fast as possible after a successful self interactive verification
     */
    fun onVerificationCompleteForDevice(deviceId: String) {
        // For now we just keep an in memory cache
        cryptoCoroutineScope.launch {
            verifMutex.withLock {
                recentlyVerifiedDevices[deviceId] = System.currentTimeMillis()
            }
        }
    }

    suspend fun handleSecretRequest(toDevice: Event) {
        val request = toDevice.getClearContent().toModel<SecretShareRequest>()
                ?: return Unit.also {
                    Timber.tag(loggerTag.value)
                            .w("handleSecretRequest() : malformed request")
                }

//            val (action, requestingDeviceId, requestId, secretName) = it
        val secretName = request.secretName ?: return Unit.also {
            Timber.tag(loggerTag.value)
                    .v("handleSecretRequest() : Missing secret name")
        }

        val userId = toDevice.senderId ?: return Unit.also {
            Timber.tag(loggerTag.value)
                    .v("handleSecretRequest() : Missing secret name")
        }

        if (userId != credentials.userId) {
            // secrets are only shared between our own devices
            Timber.e("Ignoring secret share request from other users $userId")
            return
        }

        val deviceId = request.requestingDeviceId
                ?: return Unit.also {
                    Timber.tag(loggerTag.value)
                            .w("handleSecretRequest() : malformed request norequestingDeviceId ")
                }

        val device = cryptoStore.getUserDevice(credentials.userId, deviceId)
                ?: return Unit.also {
                    Timber.e("Received secret share request from unknown device $deviceId")
                }

        val isRequestingDeviceTrusted = device.isVerified
        val isRecentInteractiveVerification = hasBeenVerifiedLessThanFiveMinutesFromNow(device.deviceId)
        if (isRequestingDeviceTrusted && isRecentInteractiveVerification) {
            // we can share the secret

            val secretValue = when (secretName) {
                MASTER_KEY_SSSS_NAME       -> cryptoStore.getCrossSigningPrivateKeys()?.master
                SELF_SIGNING_KEY_SSSS_NAME -> cryptoStore.getCrossSigningPrivateKeys()?.selfSigned
                USER_SIGNING_KEY_SSSS_NAME -> cryptoStore.getCrossSigningPrivateKeys()?.user
                KEYBACKUP_SECRET_SSSS_NAME -> cryptoStore.getKeyBackupRecoveryKeyInfo()?.recoveryKey
                        ?.let {
                            extractCurveKeyFromRecoveryKey(it)?.toBase64NoPadding()
                        }
                else                       -> null
            }
            if (secretValue == null) {
                Timber.i("The secret is unknown $secretName, passing to app layer")
                val toList = synchronized(gossipingRequestListeners) { gossipingRequestListeners.toList() }
                toList.onEach { listener ->
                    listener.onSecretShareRequest(request)
                }
                return
            }

            val payloadJson = mapOf(
                    "type" to EventType.SEND_SECRET,
                    "content" to mapOf(
                            "request_id" to request.requestId,
                            "secret" to secretValue
                    )
            )

            // Is it possible that we don't have an olm session?
            val devicesByUser = mapOf(device.userId to listOf(device))
            val usersDeviceMap = try {
                ensureOlmSessionsForDevicesAction.handle(devicesByUser)
            } catch (failure: Throwable) {
                Timber.tag(loggerTag.value)
                        .w("Can't share secret ${request.secretName}: Failed to establish olm session")
                return
            }

            val olmSessionResult = usersDeviceMap.getObject(device.userId, device.deviceId)
            if (olmSessionResult?.sessionId == null) {
                Timber.tag(loggerTag.value)
                        .w("secret share: no session with this device $deviceId, probably because there were no one-time keys")
                return
            }

            val encodedPayload = messageEncrypter.encryptMessage(payloadJson, listOf(device))
            val sendToDeviceMap = MXUsersDevicesMap<Any>()
            sendToDeviceMap.setObject(device.userId, device.deviceId, encodedPayload)
            val sendToDeviceParams = SendToDeviceTask.Params(EventType.ENCRYPTED, sendToDeviceMap)
            try {
                // raise the retries for secret
                sendToDeviceTask.executeRetry(sendToDeviceParams, 6)
                Timber.tag(loggerTag.value)
                        .i("successfully shared secret $secretName to ${device.shortDebugString()}")
                // TODO add a trail for that in audit logs
            } catch (failure: Throwable) {
                Timber.tag(loggerTag.value)
                        .e(failure, "failed to send shared secret $secretName to ${device.shortDebugString()}")
            }
        } else {
            Timber.d(" Received secret share request from un-authorised device ${device.deviceId}")
        }
    }

    private suspend fun hasBeenVerifiedLessThanFiveMinutesFromNow(deviceId: String): Boolean {
        val verifTimestamp = verifMutex.withLock {
            recentlyVerifiedDevices[deviceId]
        } ?: return false

        val age = System.currentTimeMillis() - verifTimestamp

        return age < SECRET_SHARE_WINDOW_DURATION
    }

    suspend fun requestSecretTo(deviceId: String, secretName: String) {
        val cryptoDeviceInfo = cryptoStore.getUserDevice(credentials.userId, deviceId) ?: return Unit.also {
            Timber.tag(loggerTag.value)
                    .d("Can't request secret for $secretName unknown device $deviceId")
        }
        val toDeviceContent = SecretShareRequest(
                requestingDeviceId = credentials.deviceId,
                secretName = secretName,
                requestId = createUniqueTxnId()
        )

        verifMutex.withLock {
            outgoingSecretRequests.add(toDeviceContent)
        }

        val contentMap = MXUsersDevicesMap<Any>()
        contentMap.setObject(cryptoDeviceInfo.userId, cryptoDeviceInfo.deviceId, toDeviceContent)

        val params = SendToDeviceTask.Params(
                eventType = EventType.REQUEST_SECRET,
                contentMap = contentMap
        )
        try {
            withContext(coroutineDispatchers.io) {
                sendToDeviceTask.executeRetry(params, 3)
            }
            Timber.tag(loggerTag.value)
                    .d("Secret request sent for $secretName to ${cryptoDeviceInfo.shortDebugString()}")
            // TODO update the audit trail
        } catch (failure: Throwable) {
            Timber.tag(loggerTag.value)
                    .w("Failed to request secret $secretName to ${cryptoDeviceInfo.shortDebugString()}")
        }
    }

    suspend fun onSecretSendReceived(toDevice: Event, handleGossip: ((name: String, value: String) -> Boolean)) {
        Timber.tag(loggerTag.value)
                .i("onSecretSend() from ${toDevice.senderId} : onSecretSendReceived ${toDevice.content?.get("sender_key")}")
        if (!toDevice.isEncrypted()) {
            // secret send messages must be encrypted
            Timber.tag(loggerTag.value).e("onSecretSend() :Received unencrypted secret send event")
            return
        }

        // Was that sent by us?
        if (toDevice.senderId != credentials.userId) {
            Timber.tag(loggerTag.value).e("onSecretSend() : Ignore secret from other user ${toDevice.senderId}")
            return
        }

        val secretContent = toDevice.getClearContent().toModel<SecretSendEventContent>() ?: return

        val existingRequest = verifMutex.withLock {
            outgoingSecretRequests.firstOrNull { it.requestId == secretContent.requestId }
        }

        // As per spec:
        // Clients should ignore m.secret.send events received from devices that it did not send an m.secret.request event to.
        if (existingRequest?.secretName == null) {
            Timber.tag(loggerTag.value).i("onSecretSend() : Ignore secret that was not requested: ${secretContent.requestId}")
            return
        }
        // we don't need to cancel the request as we only request to one device
        // just forget about the request now
        verifMutex.withLock {
            outgoingSecretRequests.remove(existingRequest)
        }

        if (!handleGossip(existingRequest.secretName, secretContent.secretValue)) {
            // TODO Ask to application layer?
            Timber.tag(loggerTag.value).v("onSecretSend() : secret not handled by SDK")
        }
    }
}
