/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixConfiguration
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM_BACKUP
import org.matrix.android.sdk.api.listeners.ProgressListener
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.api.session.crypto.crosssigning.MXCrossSigningInfo
import org.matrix.android.sdk.api.session.crypto.crosssigning.PrivateKeysInfo
import org.matrix.android.sdk.api.session.crypto.crosssigning.UserTrustResult
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.ImportRoomKeysResult
import org.matrix.android.sdk.api.session.crypto.model.MXEventDecryptionResult
import org.matrix.android.sdk.api.session.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.api.session.crypto.model.MessageVerificationState
import org.matrix.android.sdk.api.session.crypto.model.UnsignedDeviceInfo
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.sync.model.DeviceListResponse
import org.matrix.android.sdk.api.session.sync.model.DeviceOneTimeKeysCountSyncResponse
import org.matrix.android.sdk.api.session.sync.model.ToDeviceSyncResponse
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toOptional
import org.matrix.android.sdk.internal.coroutines.builder.safeInvokeOnClose
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.DefaultKeysAlgorithmAndData
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.KeysAlgorithmAndData
import org.matrix.android.sdk.internal.crypto.model.MXInboundMegolmSessionWrapper
import org.matrix.android.sdk.internal.crypto.network.RequestSender
import org.matrix.android.sdk.internal.crypto.verification.SasVerification
import org.matrix.android.sdk.internal.crypto.verification.VerificationRequest
import org.matrix.android.sdk.internal.crypto.verification.VerificationsProvider
import org.matrix.android.sdk.internal.crypto.verification.qrcode.QrCodeVerification
import org.matrix.android.sdk.internal.di.DeviceId
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.di.SessionRustFilesDirectory
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.network.parsing.CheckNumberType
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.rustcomponents.sdk.crypto.BackupKeys
import org.matrix.rustcomponents.sdk.crypto.BackupRecoveryKey
import org.matrix.rustcomponents.sdk.crypto.CrossSigningKeyExport
import org.matrix.rustcomponents.sdk.crypto.CrossSigningStatus
import org.matrix.rustcomponents.sdk.crypto.CryptoStoreException
import org.matrix.rustcomponents.sdk.crypto.DecryptionException
import org.matrix.rustcomponents.sdk.crypto.DeviceLists
import org.matrix.rustcomponents.sdk.crypto.EncryptionSettings
import org.matrix.rustcomponents.sdk.crypto.KeyRequestPair
import org.matrix.rustcomponents.sdk.crypto.KeysImportResult
import org.matrix.rustcomponents.sdk.crypto.LocalTrust
import org.matrix.rustcomponents.sdk.crypto.Logger
import org.matrix.rustcomponents.sdk.crypto.MegolmV1BackupKey
import org.matrix.rustcomponents.sdk.crypto.Request
import org.matrix.rustcomponents.sdk.crypto.RequestType
import org.matrix.rustcomponents.sdk.crypto.RoomKeyCounts
import org.matrix.rustcomponents.sdk.crypto.ShieldColor
import org.matrix.rustcomponents.sdk.crypto.ShieldState
import org.matrix.rustcomponents.sdk.crypto.SignatureVerification
import org.matrix.rustcomponents.sdk.crypto.setLogger
import timber.log.Timber
import java.io.File
import java.nio.charset.Charset
import javax.inject.Inject
import org.matrix.rustcomponents.sdk.crypto.OlmMachine as InnerMachine
import org.matrix.rustcomponents.sdk.crypto.ProgressListener as RustProgressListener

class CryptoLogger : Logger {
    override fun log(logLine: String) {
        Timber.d(logLine)
    }
}

private class CryptoProgressListener(private val listener: ProgressListener?) : RustProgressListener {
    override fun onProgress(progress: Int, total: Int) {
        listener?.onProgress(progress, total)
    }
}

fun setRustLogger() {
    setLogger(CryptoLogger() as Logger)
}

@SessionScope
internal class OlmMachine @Inject constructor(
        @UserId userId: String,
        @DeviceId deviceId: String,
        @SessionRustFilesDirectory path: File,
        private val requestSender: RequestSender,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        baseMoshi: Moshi,
        private val verificationsProvider: VerificationsProvider,
        private val deviceFactory: Device.Factory,
        private val getUserIdentity: GetUserIdentityUseCase,
        private val ensureUsersKeys: EnsureUsersKeysUseCase,
        private val matrixConfiguration: MatrixConfiguration,
        private val megolmSessionImportManager: MegolmSessionImportManager,
        rustEncryptionConfiguration: RustEncryptionConfiguration,
) {

    private val inner: InnerMachine

    init {
        inner = InnerMachine(userId, deviceId, path.toString(), rustEncryptionConfiguration.getDatabasePassphrase())
    }

    private val flowCollectors = FlowCollectors()

    private val moshi = baseMoshi.newBuilder()
            .add(CheckNumberType.JSON_ADAPTER_FACTORY)
            .build()

    /** Get our own user ID. */
    fun userId(): String {
        return inner.userId()
    }

    /** Get our own device ID. */
    fun deviceId(): String {
        return inner.deviceId()
    }

    /** Get our own public identity keys ID. */
    fun identityKeys(): Map<String, String> {
        return inner.identityKeys()
    }

    fun inner(): InnerMachine {
        return inner
    }

    private suspend fun updateLiveDevices() {
        flowCollectors.forEachDevicesCollector {
            val devices = getCryptoDeviceInfo(it.userIds)
            it.trySend(devices)
        }
    }

    private suspend fun updateLiveUserIdentities() {
        flowCollectors.forEachIdentityCollector {
            val identity = getIdentity(it.userId)?.toMxCrossSigningInfo().toOptional()
            it.trySend(identity)
        }
    }

    private suspend fun updateLivePrivateKeys() {
        val keys = exportCrossSigningKeys().toOptional()
        flowCollectors.forEachPrivateKeysCollector {
            it.trySend(keys)
        }
    }

    /**
     * Get our own device info as [CryptoDeviceInfo].
     */
    suspend fun ownDevice(): CryptoDeviceInfo {
        val deviceId = deviceId()

        val keys = identityKeys().map { (keyId, key) -> "$keyId:$deviceId" to key }.toMap()

        val crossSigningVerified = when (val ownIdentity = getIdentity(userId())) {
            is OwnUserIdentity -> ownIdentity.trustsOurOwnDevice()
            else -> false
        }

        return CryptoDeviceInfo(
                deviceId(),
                userId(),
                // TODO pass the algorithms here.
                listOf(),
                keys,
                mapOf(),
                UnsignedDeviceInfo(),
                DeviceTrustLevel(crossSigningVerified, locallyVerified = true),
                false,
                null
        )
    }

    /**
     * Get the list of outgoing requests that need to be sent to the homeserver.
     *
     * After the request was sent out and a successful response was received the response body
     * should be passed back to the state machine using the [markRequestAsSent] method.
     *
     * @return the list of requests that needs to be sent to the homeserver
     */
    suspend fun outgoingRequests(): List<Request> =
            withContext(coroutineDispatchers.io) { inner.outgoingRequests() }

    /**
     * Mark a request that was sent to the server as sent.
     *
     * @param requestId The unique ID of the request that was sent out. This needs to be an UUID.
     *
     * @param requestType The type of the request that was sent out.
     *
     * @param responseBody The body of the response that was received.
     */
    @Throws(CryptoStoreException::class)
    suspend fun markRequestAsSent(
            requestId: String,
            requestType: RequestType,
            responseBody: String
    ) =
            withContext(coroutineDispatchers.io) {
                inner.markRequestAsSent(requestId, requestType, responseBody)
                if (requestType == RequestType.KEYS_QUERY) {
                    updateLiveDevices()
                    updateLiveUserIdentities()
                }
            }

    /**
     * Let the state machine know about E2EE related sync changes that we received from the server.
     *
     * This needs to be called after every sync, ideally before processing any other sync changes.
     *
     * @param toDevice A serialized array of to-device events we received in the current sync
     * response.
     *
     * @param deviceChanges The list of devices that have changed in some way since the previous
     * sync.
     *
     * @param keyCounts The map of uploaded one-time key types and counts.
     *
     * @param deviceUnusedFallbackKeyTypes The key algorithms for which the server has an unused fallback key for the device.
     *
     * @param nextBatch The batch token to pass in the next sync request.
     *
     * @return The handled events, decrypted if needed (secrets are zeroised).
     */
    @Throws(CryptoStoreException::class)
    suspend fun receiveSyncChanges(
            toDevice: ToDeviceSyncResponse?,
            deviceChanges: DeviceListResponse?,
            keyCounts: DeviceOneTimeKeysCountSyncResponse?,
            deviceUnusedFallbackKeyTypes: List<String>?,
            nextBatch: String?
    ): ToDeviceSyncResponse {
        val response = withContext(coroutineDispatchers.io) {
            val counts: MutableMap<String, Int> = mutableMapOf()

            if (keyCounts?.signedCurve25519 != null) {
                counts["signed_curve25519"] = keyCounts.signedCurve25519
            }

            val devices =
                    DeviceLists(deviceChanges?.changed.orEmpty(), deviceChanges?.left.orEmpty())

            val adapter = MoshiProvider.providesMoshi()
                    .newBuilder()
                    .add(CheckNumberType.JSON_ADAPTER_FACTORY)
                    .build()
                    .adapter(ToDeviceSyncResponse::class.java)
            val events = adapter.toJson(toDevice ?: ToDeviceSyncResponse())

            // field pass in the list of unused fallback keys here
            val receiveSyncChanges = inner.receiveSyncChanges(events, devices, counts, deviceUnusedFallbackKeyTypes, nextBatch ?: "")

            val outAdapter = moshi.adapter(Event::class.java)

            // we don't need to use `roomKeyInfos` as for now we are manually
            // checking the returned to devices to check for room keys.
            // XXX Anyhow there is now proper signaling we should soon stop parsing them manually
            receiveSyncChanges.toDeviceEvents.map {
                        outAdapter.fromJson(it) ?: Event()
            }
        }

        // We may get cross signing keys over a to-device event, update our listeners.
        updateLivePrivateKeys()

        return ToDeviceSyncResponse(events = response)
    }
//
//    suspend fun receiveUnencryptedVerificationEvent(roomId: String, event: Event) = withContext(coroutineDispatchers.io) {
//        val adapter = moshi
//                .adapter(Event::class.java)
//        val serializedEvent = adapter.toJson(event)
//        inner.receiveUnencryptedVerificationEvent(serializedEvent, roomId)
//    }

    suspend fun receiveVerificationEvent(roomId: String, event: Event) = withContext(coroutineDispatchers.io) {
        val adapter = moshi
                .adapter(Event::class.java)
        val serializedEvent = adapter.toJson(event)
        inner.receiveVerificationEvent(serializedEvent, roomId)
    }

    /**
     * Used for lazy migration of inboundGroupSession from EA to ER.
     */
    suspend fun importRoomKey(inbound: MXInboundMegolmSessionWrapper): Result<Unit> {
        Timber.v("Migration:: Tentative lazy migration")
        return withContext(coroutineDispatchers.io) {
            val export = inbound.exportKeys()
                    ?: return@withContext Result.failure(Exception("Failed to export key"))
            val result = importDecryptedKeys(listOf(export), null).also {
                Timber.v("Migration:: Tentative lazy migration result: ${it.totalNumberOfKeys}")
            }
            if (result.totalNumberOfKeys == 1) return@withContext Result.success(Unit)
            return@withContext Result.failure(Exception("Import failed"))
        }
    }

    /**
     * Mark the given list of users to be tracked, triggering a key query request for them.
     *
     * *Note*: Only users that aren't already tracked will be considered for an update. It's safe to
     * call this with already tracked users, it won't result in excessive keys query requests.
     *
     * @param users The users that should be queued up for a key query.
     */
    suspend fun updateTrackedUsers(users: List<String>) =
            withContext(coroutineDispatchers.io) { inner.updateTrackedUsers(users) }

    /**
     * Check if the given user is considered to be tracked.
     * A user can be marked for tracking using the
     * [OlmMachine.updateTrackedUsers] method.
     */
    @Throws(CryptoStoreException::class)
    fun isUserTracked(userId: String): Boolean {
        return inner.isUserTracked(userId)
    }

    /**
     * Generate one-time key claiming requests for all the users we are missing sessions for.
     *
     * After the request was sent out and a successful response was received the response body
     * should be passed back to the state machine using the [markRequestAsSent] method.
     *
     * This method should be called every time before a call to [shareRoomKey] is made.
     *
     * @param users The list of users for which we would like to establish 1:1 Olm sessions for.
     *
     * @return A [Request.KeysClaim] request that needs to be sent out to the server.
     */
    @Throws(CryptoStoreException::class)
    suspend fun getMissingSessions(users: List<String>): Request? =
            withContext(coroutineDispatchers.io) { inner.getMissingSessions(users) }

    /**
     * Share a room key with the given list of users for the given room.
     *
     * After the request was sent out and a successful response was received the response body
     * should be passed back to the state machine using the markRequestAsSent() method.
     *
     * This method should be called every time before a call to `encrypt()` with the given `room_id`
     * is made.
     *
     * @param roomId The unique id of the room, note that this doesn't strictly need to be a Matrix
     * room, it just needs to be an unique identifier for the group that will participate in the
     * conversation.
     *
     * @param users The list of users which are considered to be members of the room and should
     * receive the room key.
     *
     * @param settings The encryption settings for that room.
     *
     * @return The list of [Request.ToDevice] that need to be sent out.
     */
    @Throws(CryptoStoreException::class)
    suspend fun shareRoomKey(roomId: String, users: List<String>, settings: EncryptionSettings): List<Request> =
            withContext(coroutineDispatchers.io) {
                inner.shareRoomKey(roomId, users, settings)
            }

    /**
     * Encrypt the given event with the given type and content for the given room.
     *
     * **Note**: A room key needs to be shared with the group of users that are members
     * in the given room. If this is not done this method will panic.
     *
     * The usual flow to encrypt an event using this state machine is as follows:
     *
     * 1. Get the one-time key claim request to establish 1:1 Olm sessions for
     * the room members of the room we wish to participate in. This is done
     * using the [getMissingSessions] method. This method call should be locked per call.
     *
     * 2. Share a room key with all the room members using the [shareRoomKey].
     * This method call should be locked per room.
     *
     * 3. Encrypt the event using this method.
     *
     * 4. Send the encrypted event to the server.
     *
     * After the room key is shared steps 1 and 2 will become no-ops, unless there's some changes in
     * the room membership or in the list of devices a member has.
     *
     * @param roomId the ID of the room where the encrypted event will be sent to
     *
     * @param eventType the type of the event
     *
     * @param content the JSON content of the event
     *
     * @return The encrypted version of the [Content]
     */
    @Throws(CryptoStoreException::class)
    suspend fun encrypt(roomId: String, eventType: String, content: Content): Content =
            withContext(coroutineDispatchers.io) {
                val adapter = moshi.adapter<Content>(Map::class.java)
                val contentString = adapter.toJson(content)
                val encrypted = inner.encrypt(roomId, eventType, contentString)
                adapter.fromJson(encrypted)!!
            }

    /**
     * Decrypt the given event that was sent in the given room.
     *
     * # Arguments
     *
     * @param event The serialized encrypted version of the event.
     *
     * @return the decrypted version of the event as a [MXEventDecryptionResult].
     */
    @Throws(MXCryptoError::class)
    suspend fun decryptRoomEvent(event: Event): MXEventDecryptionResult =
            withContext(coroutineDispatchers.io) {
                val adapter = moshi.adapter(Event::class.java)
                try {
                    if (event.roomId.isNullOrBlank()) {
                        throw MXCryptoError.Base(MXCryptoError.ErrorType.MISSING_FIELDS, MXCryptoError.MISSING_FIELDS_REASON)
                    }
                    if (event.isRedacted()) {
                        // we shouldn't attempt to decrypt a redacted event because the content is cleared and decryption will fail because of null algorithm
                        // Workaround until https://github.com/matrix-org/matrix-rust-sdk/issues/1642
                        return@withContext MXEventDecryptionResult(
                                clearEvent = mapOf(
                                        "room_id" to event.roomId,
                                        "type" to EventType.MESSAGE,
                                        "content" to emptyMap<String, Any>(),
                                        "unsigned" to event.unsignedData.toContent()
                                )
                        )
                    }

                    val serializedEvent = adapter.toJson(event)
                    val decrypted = inner.decryptRoomEvent(serializedEvent, event.roomId, false, false)

                    val deserializationAdapter =
                            moshi.adapter<JsonDict>(Map::class.java)
                    val clearEvent = deserializationAdapter.fromJson(decrypted.clearEvent)
                            ?: throw MXCryptoError.Base(MXCryptoError.ErrorType.MISSING_FIELDS, MXCryptoError.MISSING_FIELDS_REASON)

                    MXEventDecryptionResult(
                            clearEvent = clearEvent,
                            senderCurve25519Key = decrypted.senderCurve25519Key,
                            claimedEd25519Key = decrypted.claimedEd25519Key,
                            forwardingCurve25519KeyChain = decrypted.forwardingCurve25519Chain,
                            messageVerificationState = decrypted.shieldState.toVerificationState(),
                    )
                } catch (throwable: Throwable) {
                    val reThrow = when (throwable) {
                        is DecryptionException.MissingRoomKey -> {
                            if (throwable.withheldCode != null) {
                                MXCryptoError.Base(MXCryptoError.ErrorType.KEYS_WITHHELD, throwable.withheldCode!!)
                            } else {
                                MXCryptoError.Base(MXCryptoError.ErrorType.UNKNOWN_INBOUND_SESSION_ID, throwable.error)
                            }
                        }
                        is DecryptionException.Megolm -> {
                            // TODO check if it's the correct binding?
                            // Could encapsulate more than that, need to update sdk
                            MXCryptoError.Base(MXCryptoError.ErrorType.UNKNOWN_MESSAGE_INDEX, throwable.error)
                        }
                        is DecryptionException.Identifier -> {
                            MXCryptoError.Base(MXCryptoError.ErrorType.BAD_EVENT_FORMAT, MXCryptoError.BAD_EVENT_FORMAT_TEXT_REASON)
                        }
                        else -> {
                            val reason = String.format(
                                    MXCryptoError.UNABLE_TO_DECRYPT_REASON,
                                    throwable.message,
                                    "m.megolm.v1.aes-sha2"
                            )
                            MXCryptoError.Base(MXCryptoError.ErrorType.UNABLE_TO_DECRYPT, reason)
                        }
                    }
                    matrixConfiguration.cryptoAnalyticsPlugin?.onFailedToDecryptRoomMessage(
                            reThrow,
                            (event.content?.get("session_id") as? String) ?: ""
                    )
                    throw reThrow
                }
            }

    private fun ShieldState.toVerificationState(): MessageVerificationState? {
        return when (this.color) {
            ShieldColor.NONE -> MessageVerificationState.VERIFIED
            ShieldColor.RED -> {
                when (this.message) {
                    "Encrypted by an unverified device." -> MessageVerificationState.UN_SIGNED_DEVICE
                    "Encrypted by a device not verified by its owner." -> MessageVerificationState.UN_SIGNED_DEVICE
                    "Encrypted by an unknown or deleted device." -> MessageVerificationState.UNKNOWN_DEVICE
                    else -> MessageVerificationState.UN_SIGNED_DEVICE
                }
            }
            ShieldColor.GREY -> {
                MessageVerificationState.UNSAFE_SOURCE
            }
        }
    }

    /**
     * Request the room key that was used to encrypt the given undecrypted event.
     *
     * @param event The that we're not able to decrypt and want to request a room key for.
     *
     * @return a key request pair, consisting of an optional key request cancellation and the key
     * request itself. The cancellation *must* be sent out before the request, otherwise devices
     * will ignore the key request.
     */
    @Throws(DecryptionException::class)
    suspend fun requestRoomKey(event: Event): KeyRequestPair =
            withContext(coroutineDispatchers.io) {
                val adapter = moshi.adapter(Event::class.java)
                val serializedEvent = adapter.toJson(event)

                inner.requestRoomKey(serializedEvent, event.roomId!!)
            }

    /**
     * Export all of our room keys.
     *
     * @param passphrase The passphrase that should be used to encrypt the key export.
     *
     * @param rounds The number of rounds that should be used when expanding the passphrase into an
     * key.
     *
     * @return the encrypted key export as a bytearray.
     */
    @Throws(CryptoStoreException::class)
    suspend fun exportKeys(passphrase: String, rounds: Int): ByteArray =
            withContext(coroutineDispatchers.io) {
                inner.exportRoomKeys(passphrase, rounds).toByteArray()
            }

    private fun KeysImportResult.fromOlm(): ImportRoomKeysResult {
        return ImportRoomKeysResult(
                this.total.toInt(),
                this.imported.toInt(),
                this.keys
        )
    }

    /**
     * Import room keys from the given serialized key export.
     *
     * @param keys The serialized version of the key export.
     *
     * @param passphrase The passphrase that was used to encrypt the key export.
     *
     * @param listener A callback that can be used to introspect the progress of the key import.
     */
    @Throws(CryptoStoreException::class)
    suspend fun importKeys(
            keys: ByteArray,
            passphrase: String,
            listener: ProgressListener?
    ): ImportRoomKeysResult =
            withContext(coroutineDispatchers.io) {
                val decodedKeys = String(keys, Charset.defaultCharset())

                val rustListener = CryptoProgressListener(listener)

                val result = inner.importRoomKeys(decodedKeys, passphrase, rustListener)

                result.fromOlm()
            }

    @Throws(CryptoStoreException::class)
    suspend fun importDecryptedKeys(
            keys: List<MegolmSessionData>,
            listener: ProgressListener?
    ): ImportRoomKeysResult =
            withContext(coroutineDispatchers.io) {
                val adapter = moshi.adapter(List::class.java)

                // If the key backup is too big we take the risk of causing OOM
                // when serializing to json
                // so let's chunk to avoid it
                var totalImported = 0L
                var accTotal = 0L
                val details = mutableMapOf<String, Map<String, List<String>>>()
                keys.chunked(500)
                        .forEach { keysSlice ->
                            val encodedKeys = adapter.toJson(keysSlice)
                            val rustListener = object : RustProgressListener {
                                override fun onProgress(progress: Int, total: Int) {
                                    val accProgress = (accTotal + progress).toInt()
                                    listener?.onProgress(accProgress, keys.size)
                                }
                            }

                            inner.importDecryptedRoomKeys(encodedKeys, rustListener).let {
                                totalImported += it.imported
                                accTotal += it.total
                                details.putAll(it.keys)
                            }
                        }
                ImportRoomKeysResult(totalImported.toInt(), accTotal.toInt(), details).also {
                    megolmSessionImportManager.dispatchKeyImportResults(it)
                }
            }

    @Throws(CryptoStoreException::class)
    suspend fun getIdentity(userId: String): UserIdentities? = getUserIdentity(userId)

    /**
     * Get a `Device` from the store.
     *
     * This method returns our own device as well.
     *
     * @param userId The id of the device owner.
     *
     * @param deviceId The id of the device itself.
     *
     * @return The Device if it found one.
     */
    @Throws(CryptoStoreException::class)
    suspend fun getCryptoDeviceInfo(userId: String, deviceId: String): CryptoDeviceInfo? {
        return getDevice(userId, deviceId)?.toCryptoDeviceInfo()
    }

    @Throws(CryptoStoreException::class)
    suspend fun getDevice(userId: String, deviceId: String): Device? {
        val innerDevice = withContext(coroutineDispatchers.io) {
            inner.getDevice(userId, deviceId, 30u)
        } ?: return null
        return deviceFactory.create(innerDevice)
    }

    suspend fun getUserDevices(userId: String): List<Device> {
        return withContext(coroutineDispatchers.io) {
            inner.getUserDevices(userId, 30u).map(deviceFactory::create)
        }
    }

    /**
     * Get all devices of an user.
     *
     * @param userId The id of the device owner.
     *
     * @return The list of Devices or an empty list if there aren't any.
     */
    @Throws(CryptoStoreException::class)
    suspend fun getCryptoDeviceInfo(userId: String): List<CryptoDeviceInfo> {
        return getUserDevices(userId).map { it.toCryptoDeviceInfo() }
    }

    /**
     * Get all the devices of multiple users.
     *
     * @param userIds The ids of the device owners.
     *
     * @return The list of Devices or an empty list if there aren't any.
     */
    private suspend fun getCryptoDeviceInfo(userIds: List<String>): List<CryptoDeviceInfo> {
        val plainDevices: ArrayList<CryptoDeviceInfo> = arrayListOf()

        for (user in userIds) {
            val devices = getCryptoDeviceInfo(user)
            plainDevices.addAll(devices)
        }

        return plainDevices
    }

    private suspend fun getUserDevicesMap(userIds: List<String>): MXUsersDevicesMap<CryptoDeviceInfo> {
        val userMap = MXUsersDevicesMap<CryptoDeviceInfo>()

        for (user in userIds) {
            val devices = getCryptoDeviceInfo(user)

            for (device in devices) {
                userMap.setObject(user, device.deviceId, device)
            }
        }

        return userMap
    }

    /**
     * If the user is untracked or forceDownload is set to true, a key query request will be made.
     * It will suspend until query response, and the device list will be returned.
     *
     * The key query request will be retried a few time in case of shaky connection, but could fail.
     */
    suspend fun ensureUserDevicesMap(userIds: List<String>, forceDownload: Boolean = false): MXUsersDevicesMap<CryptoDeviceInfo> {
        ensureUsersKeys(userIds, forceDownload)
        return getUserDevicesMap(userIds)
    }

    /**
     * If the user is untracked or forceDownload is set to true, a key query request will be made.
     * It will suspend until query response.
     *
     * The key query request will be retried a few time in case of shaky connection, but could fail.
     */
    suspend fun ensureUsersKeys(userIds: List<String>, forceDownload: Boolean = false) {
        ensureUsersKeys.invoke(userIds, forceDownload)
    }

    private fun getUserIdentityFlow(userId: String): Flow<Optional<MXCrossSigningInfo>> {
        return channelFlow {
            val userIdentityCollector = UserIdentityCollector(userId, this)
            val onClose = safeInvokeOnClose {
                flowCollectors.removeIdentityCollector(userIdentityCollector)
            }
            flowCollectors.addIdentityCollector(userIdentityCollector)
            val identity = getIdentity(userId)?.toMxCrossSigningInfo().toOptional()
            send(identity)
            onClose.await()
        }
    }

    fun getLiveUserIdentity(userId: String): LiveData<Optional<MXCrossSigningInfo>> {
        return getUserIdentityFlow(userId).asLiveData(coroutineDispatchers.io)
    }

    fun getLivePrivateCrossSigningKeys(): LiveData<Optional<PrivateKeysInfo>> {
        return getPrivateCrossSigningKeysFlow().asLiveData(coroutineDispatchers.io)
    }

    fun getPrivateCrossSigningKeysFlow(): Flow<Optional<PrivateKeysInfo>> {
        return channelFlow {
            val onClose = safeInvokeOnClose {
                flowCollectors.removePrivateKeysCollector(this)
            }
            flowCollectors.addPrivateKeysCollector(this)
            val keys = this@OlmMachine.exportCrossSigningKeys().toOptional()
            send(keys)
            onClose.await()
        }
    }

    /**
     * Get all the devices of multiple users as a live version.
     *
     * The live version will update the list of devices if some of the data changes, or if new
     * devices arrive for a certain user.
     *
     * @param userIds The ids of the device owners.
     *
     * @return The list of Devices or an empty list if there aren't any as a Flow.
     */
    fun getLiveDevices(userIds: List<String>): LiveData<List<CryptoDeviceInfo>> {
        return getDevicesFlow(userIds).asLiveData(coroutineDispatchers.io)
    }

    fun getDevicesFlow(userIds: List<String>): Flow<List<CryptoDeviceInfo>> {
        return channelFlow {
            val devicesCollector = DevicesCollector(userIds, this)
            val onClose = safeInvokeOnClose {
                flowCollectors.removeDevicesCollector(devicesCollector)
            }
            flowCollectors.addDevicesCollector(devicesCollector)
            val devices = getCryptoDeviceInfo(userIds)
            send(devices)
            onClose.await()
        }
    }

    /** Discard the currently active room key for the given room if there is one. */
    @Throws(CryptoStoreException::class)
    fun discardRoomKey(roomId: String) {
        runBlocking { inner.discardRoomKey(roomId) }
    }

    /**
     *  Get all the verification requests we have with the given user.
     *
     * @param userId The ID of the user for which we would like to fetch the
     * verification requests
     *
     * @return The list of [VerificationRequest] that we share with the given user
     */
    fun getVerificationRequests(userId: String): List<VerificationRequest> {
        return verificationsProvider.getVerificationRequests(userId)
    }

    /** Get a verification request for the given user with the given flow ID. */
    fun getVerificationRequest(userId: String, flowId: String): VerificationRequest? {
        return verificationsProvider.getVerificationRequest(userId, flowId)
    }

    /** Get an active verification for the given user and given flow ID.
     *
     * @return Either a [SasVerification] verification or a [QrCodeVerification]
     * verification.
     */
    fun getVerification(userId: String, flowId: String): VerificationTransaction? {
        return verificationsProvider.getVerification(userId, flowId)
    }

    suspend fun bootstrapCrossSigning(uiaInterceptor: UserInteractiveAuthInterceptor?) {
        val requests = withContext(coroutineDispatchers.io) {
            inner.bootstrapCrossSigning()
        }
        requestSender.uploadCrossSigningKeys(requests.uploadSigningKeysRequest, uiaInterceptor)
        requestSender.sendSignatureUpload(requests.signatureRequest)
    }

    /**
     * Get the status of our private cross signing keys, i.e. which private keys do we have stored locally.
     */
    fun crossSigningStatus(): CrossSigningStatus {
        return inner.crossSigningStatus()
    }

    suspend fun exportCrossSigningKeys(): PrivateKeysInfo? {
        val export = withContext(coroutineDispatchers.io) {
            inner.exportCrossSigningKeys()
        } ?: return null

        return PrivateKeysInfo(export.masterKey, export.selfSigningKey, export.userSigningKey)
    }

    suspend fun importCrossSigningKeys(export: PrivateKeysInfo): UserTrustResult {
        val rustExport = CrossSigningKeyExport(export.master, export.selfSigned, export.user)

        var result: UserTrustResult
        withContext(coroutineDispatchers.io) {
            result = try {
                inner.importCrossSigningKeys(rustExport)

                // Sign the cross signing keys with our device
                // Fail silently if signature upload fails??
                try {
                    getIdentity(userId())?.verify()
                } catch (failure: Throwable) {
                    Timber.e(failure, "Failed to sign x-keys with own device")
                }
                UserTrustResult.Success
            } catch (failure: Exception) {
                // KeyImportError?
                UserTrustResult.Failure(failure.localizedMessage ?: "Unknown Error")
            }
        }
        withContext(coroutineDispatchers.main) {
            this@OlmMachine.updateLivePrivateKeys()
        }
        return result
    }

    suspend fun sign(message: String): Map<String, Map<String, String>> {
        return withContext(coroutineDispatchers.computation) {
            inner.sign(message)
        }
    }

    suspend fun requestMissingSecretsFromOtherSessions(): Boolean {
        return withContext(coroutineDispatchers.io) {
            inner.queryMissingSecretsFromOtherSessions()
        }
    }
    @Throws(CryptoStoreException::class)
    suspend fun enableBackupV1(key: String, version: String) {
        return withContext(coroutineDispatchers.computation) {
            val backupKey = MegolmV1BackupKey(key, mapOf(), null, MXCRYPTO_ALGORITHM_MEGOLM_BACKUP)
            inner.enableBackupV1(backupKey, version)
        }
    }

    @Throws(CryptoStoreException::class)
    fun disableBackup() {
        inner.disableBackup()
    }

    fun backupEnabled(): Boolean {
        return inner.backupEnabled()
    }

    @Throws(CryptoStoreException::class)
    suspend fun roomKeyCounts(): RoomKeyCounts {
        return withContext(coroutineDispatchers.computation) {
            inner.roomKeyCounts()
        }
    }

    @Throws(CryptoStoreException::class)
    suspend fun getBackupKeys(): BackupKeys? {
        return withContext(coroutineDispatchers.computation) {
            inner.getBackupKeys()
        }
    }

    @Throws(CryptoStoreException::class)
    suspend fun saveRecoveryKey(key: BackupRecoveryKey?, version: String?) {
        withContext(coroutineDispatchers.computation) {
            inner.saveRecoveryKey(key, version)
        }
    }

    @Throws(CryptoStoreException::class)
    suspend fun backupRoomKeys(): Request? {
        return withContext(coroutineDispatchers.computation) {
            Timber.d("BACKUP CREATING REQUEST")
            val request = inner.backupRoomKeys()
            Timber.d("BACKUP CREATED REQUEST: $request")
            request
        }
    }

    @Throws(CryptoStoreException::class)
    suspend fun checkAuthDataSignature(authData: KeysAlgorithmAndData): SignatureVerification {
        return withContext(coroutineDispatchers.computation) {
            val adapter = moshi
                    .newBuilder()
                    .build()
                    .adapter(DefaultKeysAlgorithmAndData::class.java)
            val serializedAuthData = adapter.toJson(
                    DefaultKeysAlgorithmAndData(
                            algorithm = authData.algorithm,
                            authData = authData.authData
                    )
            )

            inner.verifyBackup(serializedAuthData)
        }
    }

    @Throws(CryptoStoreException::class)
    suspend fun setDeviceLocalTrust(userId: String, deviceId: String, trusted: Boolean) {
        withContext(coroutineDispatchers.io) {
            inner.setLocalTrust(userId, deviceId, if (trusted) LocalTrust.VERIFIED else LocalTrust.UNSET)
        }
    }
}
