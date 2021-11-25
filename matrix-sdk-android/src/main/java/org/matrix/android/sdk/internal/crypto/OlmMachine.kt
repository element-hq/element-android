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
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.listeners.ProgressListener
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.crypto.crosssigning.MXCrossSigningInfo
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.sync.model.DeviceListResponse
import org.matrix.android.sdk.api.session.sync.model.DeviceOneTimeKeysCountSyncResponse
import org.matrix.android.sdk.api.session.sync.model.ToDeviceSyncResponse
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toOptional
import org.matrix.android.sdk.internal.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.internal.crypto.crosssigning.UserTrustResult
import org.matrix.android.sdk.internal.crypto.keysbackup.model.MegolmBackupAuthData
import org.matrix.android.sdk.internal.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.internal.crypto.model.ImportRoomKeysResult
import org.matrix.android.sdk.internal.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.internal.crypto.model.rest.RestKeyInfo
import org.matrix.android.sdk.internal.crypto.model.rest.UnsignedDeviceInfo
import org.matrix.android.sdk.internal.crypto.store.PrivateKeysInfo
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.network.parsing.CheckNumberType
import timber.log.Timber
import uniffi.olm.BackupKeys
import uniffi.olm.CrossSigningKeyExport
import uniffi.olm.CrossSigningStatus
import uniffi.olm.CryptoStoreException
import uniffi.olm.DecryptionException
import uniffi.olm.DeviceLists
import uniffi.olm.KeyRequestPair
import uniffi.olm.Logger
import uniffi.olm.MegolmV1BackupKey
import uniffi.olm.Request
import uniffi.olm.RequestType
import uniffi.olm.RoomKeyCounts
import uniffi.olm.setLogger
import java.io.File
import java.nio.charset.Charset
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import uniffi.olm.OlmMachine as InnerMachine
import uniffi.olm.ProgressListener as RustProgressListener
import uniffi.olm.UserIdentity as RustUserIdentity

class CryptoLogger : Logger {
    override fun log(logLine: String) {
        Timber.d(logLine)
    }
}

private class CryptoProgressListener(listener: ProgressListener?) : RustProgressListener {
    private val inner: ProgressListener? = listener

    override fun onProgress(progress: Int, total: Int) {
        if (this.inner != null) {
            this.inner.onProgress(progress, total)
        }
    }
}

internal class LiveDevice(
        internal var userIds: List<String>,
        private var observer: DeviceUpdateObserver
) : MutableLiveData<List<CryptoDeviceInfo>>() {

    override fun onActive() {
        observer.addDeviceUpdateListener(this)
    }

    override fun onInactive() {
        observer.removeDeviceUpdateListener(this)
    }
}

internal class LiveUserIdentity(
        internal var userId: String,
        private var observer: UserIdentityUpdateObserver
) : MutableLiveData<Optional<MXCrossSigningInfo>>() {
    override fun onActive() {
        observer.addUserIdentityUpdateListener(this)
    }

    override fun onInactive() {
        observer.removeUserIdentityUpdateListener(this)
    }
}

internal class LivePrivateCrossSigningKeys(
        private var observer: PrivateCrossSigningKeysUpdateObserver,
) : MutableLiveData<Optional<PrivateKeysInfo>>() {

    override fun onActive() {
        observer.addUserIdentityUpdateListener(this)
    }

    override fun onInactive() {
        observer.removeUserIdentityUpdateListener(this)
    }
}

fun setRustLogger() {
    setLogger(CryptoLogger() as Logger)
}

internal class DeviceUpdateObserver {
    internal val listeners = ConcurrentHashMap<LiveDevice, List<String>>()

    fun addDeviceUpdateListener(device: LiveDevice) {
        listeners[device] = device.userIds
    }

    fun removeDeviceUpdateListener(device: LiveDevice) {
        listeners.remove(device)
    }
}

internal class UserIdentityUpdateObserver {
    internal val listeners = ConcurrentHashMap<LiveUserIdentity, String>()

    fun addUserIdentityUpdateListener(userIdentity: LiveUserIdentity) {
        listeners[userIdentity] = userIdentity.userId
    }

    fun removeUserIdentityUpdateListener(userIdentity: LiveUserIdentity) {
        listeners.remove(userIdentity)
    }
}

internal class PrivateCrossSigningKeysUpdateObserver {
    internal val listeners = ConcurrentHashMap<LivePrivateCrossSigningKeys, Unit>()

    fun addUserIdentityUpdateListener(liveKeys: LivePrivateCrossSigningKeys) {
        listeners[liveKeys] = Unit
    }

    fun removeUserIdentityUpdateListener(liveKeys: LivePrivateCrossSigningKeys) {
        listeners.remove(liveKeys)
    }
}

internal class OlmMachine(
        user_id: String,
        device_id: String,
        path: File,
        deviceObserver: DeviceUpdateObserver,
        private val requestSender: RequestSender,
) {
    private val inner: InnerMachine = InnerMachine(user_id, device_id, path.toString())
    private val deviceUpdateObserver = deviceObserver
    private val userIdentityUpdateObserver = UserIdentityUpdateObserver()
    private val privateKeysUpdateObserver = PrivateCrossSigningKeysUpdateObserver()
    internal val verificationListeners = ArrayList<VerificationService.Listener>()

    /** Get our own user ID. */
    fun userId(): String {
        return this.inner.userId()
    }

    /** Get our own device ID. */
    fun deviceId(): String {
        return this.inner.deviceId()
    }

    /** Get our own public identity keys ID. */
    fun identityKeys(): Map<String, String> {
        return this.inner.identityKeys()
    }

    fun inner(): InnerMachine {
        return this.inner
    }

    /** Update all of our live device listeners. */
    private suspend fun updateLiveDevices() {
        for ((liveDevice, users) in deviceUpdateObserver.listeners) {
            val devices = getCryptoDeviceInfo(users)
            liveDevice.postValue(devices)
        }
    }

    private suspend fun updateLiveUserIdentities() {
        for ((liveIdentity, userId) in userIdentityUpdateObserver.listeners) {
            val identity = getIdentity(userId)?.toMxCrossSigningInfo().toOptional()
            liveIdentity.postValue(identity)
        }
    }

    private suspend fun updateLivePrivateKeys() {
        val keys = this.exportCrossSigningKeys().toOptional()

        for (liveKeys in privateKeysUpdateObserver.listeners.keys()) {
            liveKeys.postValue(keys)
        }
    }

    /**
     * Get our own device info as [CryptoDeviceInfo].
     */
    suspend fun ownDevice(): CryptoDeviceInfo {
        val deviceId = this.deviceId()

        val keys = this.identityKeys().map { (keyId, key) -> "$keyId:$deviceId" to key }.toMap()

        val crossSigningVerified = when (val ownIdentity = this.getIdentity(this.userId())) {
            is OwnUserIdentity -> ownIdentity.trustsOurOwnDevice()
            else               -> false
        }

        return CryptoDeviceInfo(
                this.deviceId(),
                this.userId(),
                // TODO pass the algorithms here.
                listOf(),
                keys,
                mapOf(),
                UnsignedDeviceInfo(),
                DeviceTrustLevel(crossSigningVerified, locallyVerified = true),
                false,
                null)
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
            withContext(Dispatchers.IO) { inner.outgoingRequests() }

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
            withContext(Dispatchers.IO) {
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
     */
    @Throws(CryptoStoreException::class)
    suspend fun receiveSyncChanges(
            toDevice: ToDeviceSyncResponse?,
            deviceChanges: DeviceListResponse?,
            keyCounts: DeviceOneTimeKeysCountSyncResponse?
    ): ToDeviceSyncResponse {
        val response = withContext(Dispatchers.IO) {
            val counts: MutableMap<String, Int> = mutableMapOf()

            if (keyCounts?.signedCurve25519 != null) {
                counts["signed_curve25519"] = keyCounts.signedCurve25519
            }

            val devices =
                    DeviceLists(deviceChanges?.changed.orEmpty(), deviceChanges?.left.orEmpty())
            val adapter =
                    MoshiProvider.providesMoshi().adapter(ToDeviceSyncResponse::class.java)
            val events = adapter.toJson(toDevice ?: ToDeviceSyncResponse())!!

            adapter.fromJson(inner.receiveSyncChanges(events, devices, counts)) ?: ToDeviceSyncResponse()
        }

        // We may get cross signing keys over a to-device event, update our listeners.
        this.updateLivePrivateKeys()

        return response
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
            withContext(Dispatchers.IO) { inner.updateTrackedUsers(users) }

    /**
     * Check if the given user is considered to be tracked.
     * A user can be marked for tracking using the
     * [OlmMachine.updateTrackedUsers] method.
     */
    @Throws(CryptoStoreException::class)
    fun isUserTracked(userId: String): Boolean {
        return this.inner.isUserTracked(userId)
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
            withContext(Dispatchers.IO) { inner.getMissingSessions(users) }

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
     * @return The list of [Request.ToDevice] that need to be sent out.
     */
    @Throws(CryptoStoreException::class)
    suspend fun shareRoomKey(roomId: String, users: List<String>): List<Request> =
            withContext(Dispatchers.IO) { inner.shareRoomKey(roomId, users) }

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
            withContext(Dispatchers.IO) {
                val adapter = MoshiProvider.providesMoshi().adapter<Content>(Map::class.java)
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
            withContext(Dispatchers.IO) {
                val adapter = MoshiProvider.providesMoshi().adapter(Event::class.java)
                val serializedEvent = adapter.toJson(event)
                try {
                    if (event.roomId.isNullOrBlank()) {
                        throw MXCryptoError.Base(MXCryptoError.ErrorType.MISSING_FIELDS, MXCryptoError.MISSING_FIELDS_REASON)
                    }
                    val decrypted = inner.decryptRoomEvent(serializedEvent, event.roomId)

                    val deserializationAdapter =
                            MoshiProvider.providesMoshi().adapter<JsonDict>(Map::class.java)
                    val clearEvent = deserializationAdapter.fromJson(decrypted.clearEvent)
                            ?: throw MXCryptoError.Base(MXCryptoError.ErrorType.MISSING_FIELDS, MXCryptoError.MISSING_FIELDS_REASON)

                    MXEventDecryptionResult(
                            clearEvent,
                            decrypted.senderCurve25519Key,
                            decrypted.claimedEd25519Key,
                            decrypted.forwardingCurve25519Chain)
                } catch (throwable: Throwable) {
                    val reason =
                            String.format(
                                    MXCryptoError.UNABLE_TO_DECRYPT_REASON,
                                    throwable.message,
                                    "m.megolm.v1.aes-sha2")
                    throw MXCryptoError.Base(MXCryptoError.ErrorType.UNABLE_TO_DECRYPT, reason)
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
            withContext(Dispatchers.IO) {
                val adapter = MoshiProvider.providesMoshi().adapter(Event::class.java)
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
            withContext(Dispatchers.IO) { inner.exportKeys(passphrase, rounds).toByteArray() }

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
            withContext(Dispatchers.IO) {
                val decodedKeys = String(keys, Charset.defaultCharset())

                val rustListener = CryptoProgressListener(listener)

                val result = inner.importKeys(decodedKeys, passphrase, rustListener)

                // TODO do we want to remove the cast here?
                ImportRoomKeysResult(result.total.toInt(), result.imported.toInt())
            }

    @Throws(CryptoStoreException::class)
    suspend fun importDecryptedKeys(
            keys: List<MegolmSessionData>,
            listener: ProgressListener?
    ): ImportRoomKeysResult =
            withContext(Dispatchers.IO) {
                val adapter = MoshiProvider.providesMoshi().adapter(List::class.java)
                val encodedKeys = adapter.toJson(keys)

                val rustListener = CryptoProgressListener(listener)

                val result = inner.importDecryptedKeys(encodedKeys, rustListener)

                ImportRoomKeysResult(result.total.toInt(), result.imported.toInt())
            }

    @Throws(CryptoStoreException::class)
    suspend fun getIdentity(userId: String): UserIdentities? {
        val identity = withContext(Dispatchers.IO) {
            inner.getIdentity(userId)
        }
        val adapter = MoshiProvider.providesMoshi().adapter(RestKeyInfo::class.java)

        return when (identity) {
            is RustUserIdentity.Other -> {
                val verified = this.inner().isIdentityVerified(userId)
                val masterKey = adapter.fromJson(identity.masterKey)!!.toCryptoModel().apply {
                    trustLevel = DeviceTrustLevel(verified, verified)
                }
                val selfSigningKey = adapter.fromJson(identity.selfSigningKey)!!.toCryptoModel().apply {
                    trustLevel = DeviceTrustLevel(verified, verified)
                }

                UserIdentity(identity.userId, masterKey, selfSigningKey, this, this.requestSender)
            }
            is RustUserIdentity.Own   -> {
                val masterKey = adapter.fromJson(identity.masterKey)!!.toCryptoModel()
                val selfSigningKey = adapter.fromJson(identity.selfSigningKey)!!.toCryptoModel()
                val userSigningKey = adapter.fromJson(identity.userSigningKey)!!.toCryptoModel()

                OwnUserIdentity(
                        identity.userId,
                        masterKey,
                        selfSigningKey,
                        userSigningKey,
                        identity.trustsOurOwnDevice,
                        this,
                        this.requestSender
                )
            }
            null                      -> null
        }
    }

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
        return if (userId == userId() && deviceId == deviceId()) {
            // Our own device isn't part of our store on the Rust side, return it
            // using our ownDevice method
            ownDevice()
        } else {
            getDevice(userId, deviceId)?.toCryptoDeviceInfo()
        }
    }

    @Throws(CryptoStoreException::class)
    suspend fun getDevice(userId: String, deviceId: String): Device? {
        val device = withContext(Dispatchers.IO) {
            inner.getDevice(userId, deviceId)
        } ?: return null

        return Device(this.inner, device, this.requestSender, this.verificationListeners)
    }

    suspend fun getUserDevices(userId: String): List<Device> {
        return withContext(Dispatchers.IO) {
            inner.getUserDevices(userId).map { Device(inner, it, requestSender, verificationListeners) }
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
        val devices = this.getUserDevices(userId).map { it.toCryptoDeviceInfo() }.toMutableList()

        // EA doesn't differentiate much between our own and other devices of
        // while the rust-sdk does, append our own device here.
        if (userId == this.userId()) {
            devices.add(this.ownDevice())
        }

        return devices
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
            val devices = this.getCryptoDeviceInfo(user)
            plainDevices.addAll(devices)
        }

        return plainDevices
    }

    @Throws
    suspend fun forceKeyDownload(userIds: List<String>) {
        withContext(Dispatchers.IO) {
            val requestId = UUID.randomUUID().toString()
            val response = requestSender.queryKeys(Request.KeysQuery(requestId, userIds))
            markRequestAsSent(requestId, RequestType.KEYS_QUERY, response)
        }
    }

    suspend fun getUserDevicesMap(userIds: List<String>): MXUsersDevicesMap<CryptoDeviceInfo> {
        val userMap = MXUsersDevicesMap<CryptoDeviceInfo>()

        for (user in userIds) {
            val devices = this.getCryptoDeviceInfo(user)

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
        val toDownload = if (forceDownload) {
            userIds
        } else {
            userIds.mapNotNull { userId ->
                userId.takeIf { !isUserTracked(it) }
            }.also {
                updateTrackedUsers(it)
            }
        }
        tryOrNull("Failed to download keys for $toDownload") {
            forceKeyDownload(toDownload)
        }
        return getUserDevicesMap(userIds)
    }

    suspend fun getLiveUserIdentity(userId: String): LiveData<Optional<MXCrossSigningInfo>> {
        val identity = this.getIdentity(userId)?.toMxCrossSigningInfo().toOptional()
        val liveIdentity = LiveUserIdentity(userId, this.userIdentityUpdateObserver)
        liveIdentity.value = identity

        return liveIdentity
    }

    suspend fun getLivePrivateCrossSigningKeys(): LiveData<Optional<PrivateKeysInfo>> {
        val keys = this.exportCrossSigningKeys().toOptional()
        val liveKeys = LivePrivateCrossSigningKeys(this.privateKeysUpdateObserver)
        liveKeys.value = keys

        return liveKeys
    }

    /**
     * Get all the devices of multiple users as a live version.
     *
     * The live version will update the list of devices if some of the data changes, or if new
     * devices arrive for a certain user.
     *
     * @param userIds The ids of the device owners.
     *
     * @return The list of Devices or an empty list if there aren't any.
     */
    suspend fun getLiveDevices(userIds: List<String>): LiveData<List<CryptoDeviceInfo>> {
        val plainDevices = getCryptoDeviceInfo(userIds)
        val devices = LiveDevice(userIds, deviceUpdateObserver)
        devices.value = plainDevices

        return devices
    }

    /** Discard the currently active room key for the given room if there is one. */
    @Throws(CryptoStoreException::class)
    fun discardRoomKey(roomId: String) {
        runBlocking { inner.discardRoomKey(roomId) }
    }

    /** Get all the verification requests we have with the given user
     *
     * @param userId The ID of the user for which we would like to fetch the
     * verification requests
     *
     * @return The list of [VerificationRequest] that we share with the given user
     */
    fun getVerificationRequests(userId: String): List<VerificationRequest> {
        return this.inner.getVerificationRequests(userId).map {
            VerificationRequest(
                    this.inner,
                    it,
                    this.requestSender,
                    this.verificationListeners,
            )
        }
    }

    /** Get a verification request for the given user with the given flow ID */
    fun getVerificationRequest(userId: String, flowId: String): VerificationRequest? {
        val request = this.inner.getVerificationRequest(userId, flowId)

        return if (request != null) {
            VerificationRequest(
                    this.inner,
                    request,
                    requestSender,
                    this.verificationListeners,
            )
        } else {
            null
        }
    }

    /** Get an active verification for the given user and given flow ID.
     *
     * @return Either a [SasVerification] verification or a [QrCodeVerification]
     * verification.
     */
    fun getVerification(userId: String, flowId: String): VerificationTransaction? {
        return when (val verification = this.inner.getVerification(userId, flowId)) {
            is uniffi.olm.Verification.QrCodeV1 -> {
                val request = this.getVerificationRequest(userId, flowId) ?: return null
                QrCodeVerification(inner, request, verification.qrcode, requestSender, verificationListeners)
            }
            is uniffi.olm.Verification.SasV1    -> {
                SasVerification(inner, verification.sas, requestSender, verificationListeners)
            }
            null                                -> {
                // This branch exists because scanning a QR code is tied to the QrCodeVerification,
                // i.e. instead of branching into a scanned QR code verification from the verification request,
                // like it's done for SAS verifications, the public API expects us to create an empty dummy
                // QrCodeVerification object that gets populated once a QR code is scanned.
                val request = getVerificationRequest(userId, flowId) ?: return null

                if (request.canScanQrCodes()) {
                    QrCodeVerification(inner, request, null, requestSender, verificationListeners)
                } else {
                    null
                }
            }
        }
    }

    suspend fun bootstrapCrossSigning(uiaInterceptor: UserInteractiveAuthInterceptor?) {
        val requests = withContext(Dispatchers.IO) {
            inner.bootstrapCrossSigning()
        }

        this.requestSender.uploadCrossSigningKeys(requests.uploadSigningKeysRequest, uiaInterceptor)
        this.requestSender.sendSignatureUpload(requests.signatureRequest)
    }

    /**
     * Get the status of our private cross signing keys, i.e. which private keys do we have stored locally.
     */
    fun crossSigningStatus(): CrossSigningStatus {
        return this.inner.crossSigningStatus()
    }

    suspend fun exportCrossSigningKeys(): PrivateKeysInfo? {
        val export = withContext(Dispatchers.IO) {
            inner.exportCrossSigningKeys()
        } ?: return null

        return PrivateKeysInfo(export.masterKey, export.selfSigningKey, export.userSigningKey)
    }

    suspend fun importCrossSigningKeys(export: PrivateKeysInfo): UserTrustResult {
        val rustExport = CrossSigningKeyExport(export.master, export.selfSigned, export.user)

        withContext(Dispatchers.IO) {
            inner.importCrossSigningKeys(rustExport)
        }

        this.updateLivePrivateKeys()
        // TODO map the errors from importCrossSigningKeys to the UserTrustResult
        return UserTrustResult.Success
    }

    suspend fun sign(message: String): Map<String, Map<String, String>> {
        return withContext(Dispatchers.Default) {
            inner.sign(message)
        }
    }

    @Throws(CryptoStoreException::class)
    suspend fun enableBackupV1(key: String, version: String) {
        return withContext(Dispatchers.Default) {
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
    fun roomKeyCounts(): RoomKeyCounts {
        // TODO convert this to a suspendable method
        return inner.roomKeyCounts()
    }

    @Throws(CryptoStoreException::class)
    fun getBackupKeys(): BackupKeys? {
        // TODO this needs to be suspendable
        return inner.getBackupKeys()
    }

    @Throws(CryptoStoreException::class)
    fun saveRecoveryKey(key: String?, version: String?) {
        // TODO convert this to a suspendable method
        inner.saveRecoveryKey(key, version)
    }

    @Throws(CryptoStoreException::class)
    suspend fun backupRoomKeys(): Request? {
        return withContext(Dispatchers.Default) {
            Timber.d("BACKUP CREATING REQUEST")
            val request = inner.backupRoomKeys()
            Timber.d("BACKUP CREATED REQUEST: $request")
            request
        }
    }

    @Throws(CryptoStoreException::class)
    suspend fun checkAuthDataSignature(authData: MegolmBackupAuthData): Boolean {
        return withContext(Dispatchers.Default) {
            val adapter = MoshiProvider
                    .providesMoshi()
                    .newBuilder()
                    .add(CheckNumberType.JSON_ADAPTER_FACTORY)
                    .build()
                    .adapter(MegolmBackupAuthData::class.java)
            val serializedAuthData = adapter.toJson(authData)
            inner.verifyBackup(serializedAuthData)
        }
    }
}
