/*
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

package org.matrix.android.sdk.internal.crypto

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import com.squareup.moshi.Types
import dagger.Lazy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.NoOpMatrixCallback
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_OLM
import org.matrix.android.sdk.api.crypto.MXCryptoConfig
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.listeners.ProgressListener
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.crypto.CryptoService
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.crypto.NewSessionListener
import org.matrix.android.sdk.api.session.crypto.OutgoingKeyRequest
import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.api.session.crypto.crosssigning.KEYBACKUP_SECRET_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.MASTER_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.SELF_SIGNING_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.USER_SIGNING_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.keyshare.GossipingRequestListener
import org.matrix.android.sdk.api.session.crypto.model.AuditTrail
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.DevicesListResponse
import org.matrix.android.sdk.api.session.crypto.model.ImportRoomKeysResult
import org.matrix.android.sdk.api.session.crypto.model.IncomingRoomKeyRequest
import org.matrix.android.sdk.api.session.crypto.model.MXDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.MXEncryptEventContentResult
import org.matrix.android.sdk.api.session.crypto.model.MXEventDecryptionResult
import org.matrix.android.sdk.api.session.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.api.session.crypto.model.RoomKeyShareRequest
import org.matrix.android.sdk.api.session.crypto.model.TrailType
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.content.RoomKeyContent
import org.matrix.android.sdk.api.session.events.model.content.RoomKeyWithHeldContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibilityContent
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.model.shouldShareHistory
import org.matrix.android.sdk.api.session.sync.model.SyncResponse
import org.matrix.android.sdk.internal.crypto.actions.MegolmSessionDataImporter
import org.matrix.android.sdk.internal.crypto.actions.SetDeviceVerificationAction
import org.matrix.android.sdk.internal.crypto.algorithms.IMXEncrypting
import org.matrix.android.sdk.internal.crypto.algorithms.IMXGroupEncryption
import org.matrix.android.sdk.internal.crypto.algorithms.megolm.MXMegolmEncryptionFactory
import org.matrix.android.sdk.internal.crypto.algorithms.olm.MXOlmEncryptionFactory
import org.matrix.android.sdk.internal.crypto.crosssigning.DefaultCrossSigningService
import org.matrix.android.sdk.internal.crypto.keysbackup.DefaultKeysBackupService
import org.matrix.android.sdk.internal.crypto.model.MXKey.Companion.KEY_SIGNED_CURVE_25519_TYPE
import org.matrix.android.sdk.internal.crypto.model.SessionInfo
import org.matrix.android.sdk.internal.crypto.model.toRest
import org.matrix.android.sdk.internal.crypto.repository.WarnOnUnknownDeviceRepository
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.crypto.tasks.DeleteDeviceTask
import org.matrix.android.sdk.internal.crypto.tasks.GetDeviceInfoTask
import org.matrix.android.sdk.internal.crypto.tasks.GetDevicesTask
import org.matrix.android.sdk.internal.crypto.tasks.SetDeviceNameTask
import org.matrix.android.sdk.internal.crypto.tasks.UploadKeysTask
import org.matrix.android.sdk.internal.crypto.verification.DefaultVerificationService
import org.matrix.android.sdk.internal.crypto.verification.VerificationMessageProcessor
import org.matrix.android.sdk.internal.di.DeviceId
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.extensions.foldToCallback
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.session.StreamEventsManager
import org.matrix.android.sdk.internal.session.room.membership.LoadRoomMembersTask
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.TaskThread
import org.matrix.android.sdk.internal.task.configureWith
import org.matrix.android.sdk.internal.task.launchToCallback
import org.matrix.android.sdk.internal.util.JsonCanonicalizer
import org.matrix.android.sdk.internal.util.time.Clock
import org.matrix.olm.OlmManager
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.math.max

/**
 * A `CryptoService` class instance manages the end-to-end crypto for a session.
 *
 *
 * Messages posted by the user are automatically redirected to CryptoService in order to be encrypted
 * before sending.
 * In the other hand, received events goes through CryptoService for decrypting.
 * CryptoService maintains all necessary keys and their sharing with other devices required for the crypto.
 * Specially, it tracks all room membership changes events in order to do keys updates.
 */

private val loggerTag = LoggerTag("DefaultCryptoService", LoggerTag.CRYPTO)

@SessionScope
internal class DefaultCryptoService @Inject constructor(
        // Olm Manager
        private val olmManager: OlmManager,
        @UserId
        private val userId: String,
        @DeviceId
        private val deviceId: String?,
        private val clock: Clock,
        private val myDeviceInfoHolder: Lazy<MyDeviceInfoHolder>,
        // the crypto store
        private val cryptoStore: IMXCryptoStore,
        // Room encryptors store
        private val roomEncryptorsStore: RoomEncryptorsStore,
        // Olm device
        private val olmDevice: MXOlmDevice,
        // Set of parameters used to configure/customize the end-to-end crypto.
        private val mxCryptoConfig: MXCryptoConfig,
        // Device list manager
        private val deviceListManager: DeviceListManager,
        // The key backup service.
        private val keysBackupService: DefaultKeysBackupService,
        //
        private val objectSigner: ObjectSigner,
        //
        private val oneTimeKeysUploader: OneTimeKeysUploader,
        //
        private val roomDecryptorProvider: RoomDecryptorProvider,
        // The verification service.
        private val verificationService: DefaultVerificationService,

        private val crossSigningService: DefaultCrossSigningService,
        //
        private val incomingKeyRequestManager: IncomingKeyRequestManager,
        private val secretShareManager: SecretShareManager,
        //
        private val outgoingKeyRequestManager: OutgoingKeyRequestManager,
        // Actions
        private val setDeviceVerificationAction: SetDeviceVerificationAction,
        private val megolmSessionDataImporter: MegolmSessionDataImporter,
        private val warnOnUnknownDevicesRepository: WarnOnUnknownDeviceRepository,
        // Repository
        private val megolmEncryptionFactory: MXMegolmEncryptionFactory,
        private val olmEncryptionFactory: MXOlmEncryptionFactory,
        // Tasks
        private val deleteDeviceTask: DeleteDeviceTask,
        private val getDevicesTask: GetDevicesTask,
        private val getDeviceInfoTask: GetDeviceInfoTask,
        private val setDeviceNameTask: SetDeviceNameTask,
        private val uploadKeysTask: UploadKeysTask,
        private val loadRoomMembersTask: LoadRoomMembersTask,
        private val cryptoSessionInfoProvider: CryptoSessionInfoProvider,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val taskExecutor: TaskExecutor,
        private val cryptoCoroutineScope: CoroutineScope,
        private val eventDecryptor: EventDecryptor,
        private val verificationMessageProcessor: VerificationMessageProcessor,
        private val liveEventManager: Lazy<StreamEventsManager>
) : CryptoService {

    private val isStarting = AtomicBoolean(false)
    private val isStarted = AtomicBoolean(false)

    fun onStateEvent(roomId: String, event: Event) {
        when (event.type) {
            EventType.STATE_ROOM_ENCRYPTION -> onRoomEncryptionEvent(roomId, event)
            EventType.STATE_ROOM_MEMBER -> onRoomMembershipEvent(roomId, event)
            EventType.STATE_ROOM_HISTORY_VISIBILITY -> onRoomHistoryVisibilityEvent(roomId, event)
        }
    }

    fun onLiveEvent(roomId: String, event: Event, isInitialSync: Boolean) {
        // handle state events
        if (event.isStateEvent()) {
            when (event.type) {
                EventType.STATE_ROOM_ENCRYPTION -> onRoomEncryptionEvent(roomId, event)
                EventType.STATE_ROOM_MEMBER -> onRoomMembershipEvent(roomId, event)
                EventType.STATE_ROOM_HISTORY_VISIBILITY -> onRoomHistoryVisibilityEvent(roomId, event)
            }
        }

        // handle verification
        if (!isInitialSync) {
            if (event.type != null && verificationMessageProcessor.shouldProcess(event.type)) {
                cryptoCoroutineScope.launch(coroutineDispatchers.dmVerif) {
                    verificationMessageProcessor.process(event)
                }
            }
        }
    }

//    val gossipingBuffer = mutableListOf<Event>()

    override fun setDeviceName(deviceId: String, deviceName: String, callback: MatrixCallback<Unit>) {
        setDeviceNameTask
                .configureWith(SetDeviceNameTask.Params(deviceId, deviceName)) {
                    this.executionThread = TaskThread.CRYPTO
                    this.callback = object : MatrixCallback<Unit> {
                        override fun onSuccess(data: Unit) {
                            // bg refresh of crypto device
                            downloadKeys(listOf(userId), true, NoOpMatrixCallback())
                            callback.onSuccess(data)
                        }

                        override fun onFailure(failure: Throwable) {
                            callback.onFailure(failure)
                        }
                    }
                }
                .executeBy(taskExecutor)
    }

    override fun deleteDevice(deviceId: String, userInteractiveAuthInterceptor: UserInteractiveAuthInterceptor, callback: MatrixCallback<Unit>) {
        deleteDeviceTask
                .configureWith(DeleteDeviceTask.Params(deviceId, userInteractiveAuthInterceptor, null)) {
                    this.executionThread = TaskThread.CRYPTO
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    override fun getCryptoVersion(context: Context, longFormat: Boolean): String {
        return if (longFormat) olmManager.getDetailedVersion(context) else olmManager.version
    }

    override fun getMyDevice(): CryptoDeviceInfo {
        return myDeviceInfoHolder.get().myDevice
    }

    override fun fetchDevicesList(callback: MatrixCallback<DevicesListResponse>) {
        getDevicesTask
                .configureWith {
                    //                    this.executionThread = TaskThread.CRYPTO
                    this.callback = object : MatrixCallback<DevicesListResponse> {
                        override fun onFailure(failure: Throwable) {
                            callback.onFailure(failure)
                        }

                        override fun onSuccess(data: DevicesListResponse) {
                            // Save in local DB
                            cryptoStore.saveMyDevicesInfo(data.devices.orEmpty())
                            callback.onSuccess(data)
                        }
                    }
                }
                .executeBy(taskExecutor)
    }

    override fun getLiveMyDevicesInfo(): LiveData<List<DeviceInfo>> {
        return cryptoStore.getLiveMyDevicesInfo()
    }

    override fun getMyDevicesInfo(): List<DeviceInfo> {
        return cryptoStore.getMyDevicesInfo()
    }

    override fun getDeviceInfo(deviceId: String, callback: MatrixCallback<DeviceInfo>) {
        getDeviceInfoTask
                .configureWith(GetDeviceInfoTask.Params(deviceId)) {
                    this.executionThread = TaskThread.CRYPTO
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    override fun inboundGroupSessionsCount(onlyBackedUp: Boolean): Int {
        return cryptoStore.inboundGroupSessionsCount(onlyBackedUp)
    }

    /**
     * Provides the tracking status.
     *
     * @param userId the user id
     * @return the tracking status
     */
    override fun getDeviceTrackingStatus(userId: String): Int {
        return cryptoStore.getDeviceTrackingStatus(userId, DeviceListManager.TRACKING_STATUS_NOT_TRACKED)
    }

    /**
     * Tell if the MXCrypto is started.
     *
     * @return true if the crypto is started
     */
    fun isStarted(): Boolean {
        return isStarted.get()
    }

    /**
     * Tells if the MXCrypto is starting.
     *
     * @return true if the crypto is starting
     */
    fun isStarting(): Boolean {
        return isStarting.get()
    }

    /**
     * Start the crypto module.
     * Device keys will be uploaded, then one time keys if there are not enough on the homeserver
     * and, then, if this is the first time, this new device will be announced to all other users
     * devices.
     *
     */
    fun start() {
        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
            internalStart()
        }
        // Just update
        fetchDevicesList(NoOpMatrixCallback())

        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
            cryptoStore.tidyUpDataBase()
        }
    }

    fun ensureDevice() {
        cryptoCoroutineScope.launchToCallback(coroutineDispatchers.crypto, NoOpMatrixCallback()) {
            // Open the store
            cryptoStore.open()

            if (!cryptoStore.areDeviceKeysUploaded()) {
                // Schedule upload of OTK
                oneTimeKeysUploader.updateOneTimeKeyCount(0)
            }

            // this can throw if no network
            tryOrNull {
                uploadDeviceKeys()
            }

            oneTimeKeysUploader.maybeUploadOneTimeKeys()
            // this can throw if no backup
            tryOrNull {
                keysBackupService.checkAndStartKeysBackup()
            }
        }
    }

    fun onSyncWillProcess(isInitialSync: Boolean) {
        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
            if (isInitialSync) {
                try {
                    // On initial sync, we start all our tracking from
                    // scratch, so mark everything as untracked. onCryptoEvent will
                    // be called for all e2e rooms during the processing of the sync,
                    // at which point we'll start tracking all the users of that room.
                    deviceListManager.invalidateAllDeviceLists()
                    // always track my devices?
                    deviceListManager.startTrackingDeviceList(listOf(userId))
                    deviceListManager.refreshOutdatedDeviceLists()
                } catch (failure: Throwable) {
                    Timber.tag(loggerTag.value).e(failure, "onSyncWillProcess ")
                }
            }
        }
    }

    private fun internalStart() {
        if (isStarted.get() || isStarting.get()) {
            return
        }
        isStarting.set(true)

        // Open the store
        cryptoStore.open()

        isStarting.set(false)
        isStarted.set(true)
    }

    /**
     * Close the crypto.
     */
    fun close() = runBlocking(coroutineDispatchers.crypto) {
        cryptoCoroutineScope.coroutineContext.cancelChildren(CancellationException("Closing crypto module"))
        incomingKeyRequestManager.close()
        outgoingKeyRequestManager.close()
        olmDevice.release()
        cryptoStore.close()
    }

    // Always enabled on Matrix Android SDK2
    override fun isCryptoEnabled() = true

    /**
     * @return the Keys backup Service
     */
    override fun keysBackupService() = keysBackupService

    /**
     * @return the VerificationService
     */
    override fun verificationService() = verificationService

    override fun crossSigningService() = crossSigningService

    /**
     * A sync response has been received.
     *
     * @param syncResponse the syncResponse
     */
    fun onSyncCompleted(syncResponse: SyncResponse) {
        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
            runCatching {
                if (syncResponse.deviceLists != null) {
                    deviceListManager.handleDeviceListsChanges(syncResponse.deviceLists.changed, syncResponse.deviceLists.left)
                }
                if (syncResponse.deviceOneTimeKeysCount != null) {
                    val currentCount = syncResponse.deviceOneTimeKeysCount.signedCurve25519 ?: 0
                    oneTimeKeysUploader.updateOneTimeKeyCount(currentCount)
                }

                // unwedge if needed
                try {
                    eventDecryptor.unwedgeDevicesIfNeeded()
                } catch (failure: Throwable) {
                    Timber.tag(loggerTag.value).w("unwedgeDevicesIfNeeded failed")
                }

                // There is a limit of to_device events returned per sync.
                // If we are in a case of such limited to_device sync we can't try to generate/upload
                // new otk now, because there might be some pending olm pre-key to_device messages that would fail if we rotate
                // the old otk too early. In this case we want to wait for the pending to_device before doing anything
                // As per spec:
                // If there is a large queue of send-to-device messages, the server should limit the number sent in each /sync response.
                // 100 messages is recommended as a reasonable limit.
                // The limit is not part of the spec, so it's probably safer to handle that when there are no more to_device ( so we are sure
                // that there are no pending to_device
                val toDevices = syncResponse.toDevice?.events.orEmpty()
                if (isStarted() && toDevices.isEmpty()) {
                    // Make sure we process to-device messages before generating new one-time-keys #2782
                    deviceListManager.refreshOutdatedDeviceLists()
                    // The presence of device_unused_fallback_key_types indicates that the server supports fallback keys.
                    // If there's no unused signed_curve25519 fallback key we need a new one.
                    if (syncResponse.deviceUnusedFallbackKeyTypes != null &&
                            // Generate a fallback key only if the server does not already have an unused fallback key.
                            !syncResponse.deviceUnusedFallbackKeyTypes.contains(KEY_SIGNED_CURVE_25519_TYPE)) {
                        oneTimeKeysUploader.needsNewFallback()
                    }

                    oneTimeKeysUploader.maybeUploadOneTimeKeys()
                }

                // Process pending key requests
                try {
                    if (toDevices.isEmpty()) {
                        // this is not blocking
                        outgoingKeyRequestManager.requireProcessAllPendingKeyRequests()
                    } else {
                        Timber.tag(loggerTag.value)
                                .w("Don't process key requests yet as there might be more to_device to catchup")
                    }
                } catch (failure: Throwable) {
                    // just for safety but should not throw
                    Timber.tag(loggerTag.value).w("failed to process pending request")
                }

                try {
                    incomingKeyRequestManager.processIncomingRequests()
                } catch (failure: Throwable) {
                    // just for safety but should not throw
                    Timber.tag(loggerTag.value).w("failed to process incoming room key requests")
                }
            }
        }
    }

    /**
     * Find a device by curve25519 identity key.
     *
     * @param senderKey the curve25519 key to match.
     * @param algorithm the encryption algorithm.
     * @return the device info, or null if not found / unsupported algorithm / crypto released
     */
    override fun deviceWithIdentityKey(senderKey: String, algorithm: String): CryptoDeviceInfo? {
        return if (algorithm != MXCRYPTO_ALGORITHM_MEGOLM && algorithm != MXCRYPTO_ALGORITHM_OLM) {
            // We only deal in olm keys
            null
        } else cryptoStore.deviceWithIdentityKey(senderKey)
    }

    /**
     * Provides the device information for a user id and a device Id.
     *
     * @param userId the user id
     * @param deviceId the device id
     */
    override fun getDeviceInfo(userId: String, deviceId: String?): CryptoDeviceInfo? {
        return if (userId.isNotEmpty() && !deviceId.isNullOrEmpty()) {
            cryptoStore.getUserDevice(userId, deviceId)
        } else {
            null
        }
    }

    override fun getCryptoDeviceInfo(userId: String): List<CryptoDeviceInfo> {
        return cryptoStore.getUserDeviceList(userId).orEmpty()
    }

    override fun getLiveCryptoDeviceInfo(): LiveData<List<CryptoDeviceInfo>> {
        return cryptoStore.getLiveDeviceList()
    }

    override fun getLiveCryptoDeviceInfo(userId: String): LiveData<List<CryptoDeviceInfo>> {
        return cryptoStore.getLiveDeviceList(userId)
    }

    override fun getLiveCryptoDeviceInfo(userIds: List<String>): LiveData<List<CryptoDeviceInfo>> {
        return cryptoStore.getLiveDeviceList(userIds)
    }

    /**
     * Set the devices as known.
     *
     * @param devices the devices. Note that the verified member of the devices in this list will not be updated by this method.
     * @param callback the asynchronous callback
     */
    override fun setDevicesKnown(devices: List<MXDeviceInfo>, callback: MatrixCallback<Unit>?) {
        // build a devices map
        val devicesIdListByUserId = devices.groupBy({ it.userId }, { it.deviceId })

        for ((userId, deviceIds) in devicesIdListByUserId) {
            val storedDeviceIDs = cryptoStore.getUserDevices(userId)

            // sanity checks
            if (null != storedDeviceIDs) {
                var isUpdated = false

                deviceIds.forEach { deviceId ->
                    val device = storedDeviceIDs[deviceId]

                    // assume if the device is either verified or blocked
                    // it means that the device is known
                    if (device?.isUnknown == true) {
                        device.trustLevel = DeviceTrustLevel(crossSigningVerified = false, locallyVerified = false)
                        isUpdated = true
                    }
                }

                if (isUpdated) {
                    cryptoStore.storeUserDevices(userId, storedDeviceIDs)
                }
            }
        }

        callback?.onSuccess(Unit)
    }

    /**
     * Update the blocked/verified state of the given device.
     *
     * @param trustLevel the new trust level
     * @param userId the owner of the device
     * @param deviceId the unique identifier for the device.
     */
    override fun setDeviceVerification(trustLevel: DeviceTrustLevel, userId: String, deviceId: String) {
        setDeviceVerificationAction.handle(trustLevel, userId, deviceId)
    }

    /**
     * Configure a room to use encryption.
     *
     * @param roomId the room id to enable encryption in.
     * @param algorithm the encryption config for the room.
     * @param inhibitDeviceQuery true to suppress device list query for users in the room (for now)
     * @param membersId list of members to start tracking their devices
     * @return true if the operation succeeds.
     */
    private suspend fun setEncryptionInRoom(
            roomId: String,
            algorithm: String?,
            inhibitDeviceQuery: Boolean,
            membersId: List<String>
    ): Boolean {
        // If we already have encryption in this room, we should ignore this event
        // (for now at least. Maybe we should alert the user somehow?)
        val existingAlgorithm = cryptoStore.getRoomAlgorithm(roomId)

        if (existingAlgorithm == algorithm) {
            // ignore
            Timber.tag(loggerTag.value).e("setEncryptionInRoom() : Ignoring m.room.encryption for same alg ($algorithm) in  $roomId")
            return false
        }

        val encryptingClass = MXCryptoAlgorithms.hasEncryptorClassForAlgorithm(algorithm)

        // Always store even if not supported
        cryptoStore.storeRoomAlgorithm(roomId, algorithm)

        if (!encryptingClass) {
            Timber.tag(loggerTag.value).e("setEncryptionInRoom() : Unable to encrypt room $roomId with $algorithm")
            return false
        }

        val alg: IMXEncrypting? = when (algorithm) {
            MXCRYPTO_ALGORITHM_MEGOLM -> megolmEncryptionFactory.create(roomId)
            MXCRYPTO_ALGORITHM_OLM -> olmEncryptionFactory.create(roomId)
            else -> null
        }

        if (alg != null) {
            roomEncryptorsStore.put(roomId, alg)
        }

        // if encryption was not previously enabled in this room, we will have been
        // ignoring new device events for these users so far. We may well have
        // up-to-date lists for some users, for instance if we were sharing other
        // e2e rooms with them, so there is room for optimisation here, but for now
        // we just invalidate everyone in the room.
        if (null == existingAlgorithm) {
            Timber.tag(loggerTag.value).d("Enabling encryption in $roomId for the first time; invalidating device lists for all users therein")

            val userIds = ArrayList(membersId)

            deviceListManager.startTrackingDeviceList(userIds)

            if (!inhibitDeviceQuery) {
                deviceListManager.refreshOutdatedDeviceLists()
            }
        }

        return true
    }

    /**
     * Tells if a room is encrypted with MXCRYPTO_ALGORITHM_MEGOLM.
     *
     * @param roomId the room id
     * @return true if the room is encrypted with algorithm MXCRYPTO_ALGORITHM_MEGOLM
     */
    override fun isRoomEncrypted(roomId: String): Boolean {
        return cryptoSessionInfoProvider.isRoomEncrypted(roomId)
    }

    /**
     * @return the stored device keys for a user.
     */
    override fun getUserDevices(userId: String): MutableList<CryptoDeviceInfo> {
        return cryptoStore.getUserDevices(userId)?.values?.toMutableList() ?: ArrayList()
    }

    private fun isEncryptionEnabledForInvitedUser(): Boolean {
        return mxCryptoConfig.enableEncryptionForInvitedMembers
    }

    override fun getEncryptionAlgorithm(roomId: String): String? {
        return cryptoStore.getRoomAlgorithm(roomId)
    }

    /**
     * Determine whether we should encrypt messages for invited users in this room.
     * <p>
     * Check here whether the invited members are allowed to read messages in the room history
     * from the point they were invited onwards.
     *
     * @return true if we should encrypt messages for invited users.
     */
    override fun shouldEncryptForInvitedMembers(roomId: String): Boolean {
        return cryptoStore.shouldEncryptForInvitedMembers(roomId)
    }

    /**
     * Encrypt an event content according to the configuration of the room.
     *
     * @param eventContent the content of the event.
     * @param eventType the type of the event.
     * @param roomId the room identifier the event will be sent.
     * @param callback the asynchronous callback
     */
    override fun encryptEventContent(
            eventContent: Content,
            eventType: String,
            roomId: String,
            callback: MatrixCallback<MXEncryptEventContentResult>
    ) {
        // moved to crypto scope to have uptodate values
        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
            val userIds = getRoomUserIds(roomId)
            var alg = roomEncryptorsStore.get(roomId)
            if (alg == null) {
                val algorithm = getEncryptionAlgorithm(roomId)
                if (algorithm != null) {
                    if (setEncryptionInRoom(roomId, algorithm, false, userIds)) {
                        alg = roomEncryptorsStore.get(roomId)
                    }
                }
            }
            val safeAlgorithm = alg
            if (safeAlgorithm != null) {
                val t0 = clock.epochMillis()
                Timber.tag(loggerTag.value).v("encryptEventContent() starts")
                runCatching {
                    val content = safeAlgorithm.encryptEventContent(eventContent, eventType, userIds)
                    Timber.tag(loggerTag.value).v("## CRYPTO | encryptEventContent() : succeeds after ${clock.epochMillis() - t0} ms")
                    MXEncryptEventContentResult(content, EventType.ENCRYPTED)
                }.foldToCallback(callback)
            } else {
                val algorithm = getEncryptionAlgorithm(roomId)
                val reason = String.format(
                        MXCryptoError.UNABLE_TO_ENCRYPT_REASON,
                        algorithm ?: MXCryptoError.NO_MORE_ALGORITHM_REASON
                )
                Timber.tag(loggerTag.value).e("encryptEventContent() : failed $reason")
                callback.onFailure(Failure.CryptoError(MXCryptoError.Base(MXCryptoError.ErrorType.UNABLE_TO_ENCRYPT, reason)))
            }
        }
    }

    override fun discardOutboundSession(roomId: String) {
        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
            val roomEncryptor = roomEncryptorsStore.get(roomId)
            if (roomEncryptor is IMXGroupEncryption) {
                roomEncryptor.discardSessionKey()
            } else {
                Timber.tag(loggerTag.value).e("discardOutboundSession() for:$roomId: Unable to handle IMXGroupEncryption")
            }
        }
    }

    /**
     * Decrypt an event.
     *
     * @param event the raw event.
     * @param timeline the id of the timeline where the event is decrypted. It is used to prevent replay attack.
     * @return the MXEventDecryptionResult data, or throw in case of error
     */
    @Throws(MXCryptoError::class)
    override suspend fun decryptEvent(event: Event, timeline: String): MXEventDecryptionResult {
        return internalDecryptEvent(event, timeline)
    }

    /**
     * Decrypt an event asynchronously.
     *
     * @param event the raw event.
     * @param timeline the id of the timeline where the event is decrypted. It is used to prevent replay attack.
     * @param callback the callback to return data or null
     */
    override fun decryptEventAsync(event: Event, timeline: String, callback: MatrixCallback<MXEventDecryptionResult>) {
        eventDecryptor.decryptEventAsync(event, timeline, callback)
    }

    /**
     * Decrypt an event.
     *
     * @param event the raw event.
     * @param timeline the id of the timeline where the event is decrypted. It is used to prevent replay attack.
     * @return the MXEventDecryptionResult data, or null in case of error
     */
    @Throws(MXCryptoError::class)
    private suspend fun internalDecryptEvent(event: Event, timeline: String): MXEventDecryptionResult {
        return eventDecryptor.decryptEvent(event, timeline)
    }

    /**
     * Reset replay attack data for the given timeline.
     *
     * @param timelineId the timeline id
     */
    fun resetReplayAttackCheckInTimeline(timelineId: String) {
        olmDevice.resetReplayAttackCheckInTimeline(timelineId)
    }

    /**
     * Handle the 'toDevice' event.
     *
     * @param event the event
     */
    fun onToDeviceEvent(event: Event) {
        // event have already been decrypted
        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
            when (event.getClearType()) {
                EventType.ROOM_KEY, EventType.FORWARDED_ROOM_KEY -> {
                    // Keys are imported directly, not waiting for end of sync
                    onRoomKeyEvent(event)
                }
                EventType.REQUEST_SECRET -> {
                    secretShareManager.handleSecretRequest(event)
                }
                EventType.ROOM_KEY_REQUEST -> {
                    event.getClearContent().toModel<RoomKeyShareRequest>()?.let { req ->
                        // We'll always get these because we send room key requests to
                        // '*' (ie. 'all devices') which includes the sending device,
                        // so ignore requests from ourself because apart from it being
                        // very silly, it won't work because an Olm session cannot send
                        // messages to itself.
                        if (req.requestingDeviceId != deviceId) { // ignore self requests
                            event.senderId?.let { incomingKeyRequestManager.addNewIncomingRequest(it, req) }
                        }
                    }
                }
                EventType.SEND_SECRET -> {
                    onSecretSendReceived(event)
                }
                in EventType.ROOM_KEY_WITHHELD.values -> {
                    onKeyWithHeldReceived(event)
                }
                else -> {
                    // ignore
                }
            }
        }
        liveEventManager.get().dispatchOnLiveToDevice(event)
    }

    /**
     * Handle a key event.
     *
     * @param event the key event.
     */
    private fun onRoomKeyEvent(event: Event) {
        val roomKeyContent = event.getClearContent().toModel<RoomKeyContent>() ?: return
        Timber.tag(loggerTag.value).i("onRoomKeyEvent() from: ${event.senderId} type<${event.getClearType()}> , sessionId<${roomKeyContent.sessionId}>")
        if (roomKeyContent.roomId.isNullOrEmpty() || roomKeyContent.algorithm.isNullOrEmpty()) {
            Timber.tag(loggerTag.value).e("onRoomKeyEvent() : missing fields")
            return
        }
        val alg = roomDecryptorProvider.getOrCreateRoomDecryptor(roomKeyContent.roomId, roomKeyContent.algorithm)
        if (alg == null) {
            Timber.tag(loggerTag.value).e("GOSSIP onRoomKeyEvent() : Unable to handle keys for ${roomKeyContent.algorithm}")
            return
        }
        alg.onRoomKeyEvent(event, keysBackupService)
    }

    private fun onKeyWithHeldReceived(event: Event) {
        val withHeldContent = event.getClearContent().toModel<RoomKeyWithHeldContent>() ?: return Unit.also {
            Timber.tag(loggerTag.value).i("Malformed onKeyWithHeldReceived() : missing fields")
        }
        val senderId = event.senderId ?: return Unit.also {
            Timber.tag(loggerTag.value).i("Malformed onKeyWithHeldReceived() : missing fields")
        }
        withHeldContent.sessionId ?: return
        withHeldContent.algorithm ?: return
        withHeldContent.roomId ?: return
        withHeldContent.senderKey ?: return
        outgoingKeyRequestManager.onRoomKeyWithHeld(
                sessionId = withHeldContent.sessionId,
                algorithm = withHeldContent.algorithm,
                roomId = withHeldContent.roomId,
                senderKey = withHeldContent.senderKey,
                fromDevice = withHeldContent.fromDevice,
                event = Event(
                        type = EventType.ROOM_KEY_WITHHELD.stable,
                        senderId = senderId,
                        content = event.getClearContent()
                )
        )
    }

    private suspend fun onSecretSendReceived(event: Event) {
        secretShareManager.onSecretSendReceived(event) { secretName, secretValue ->
            handleSDKLevelGossip(secretName, secretValue)
        }
    }

    /**
     * Returns true if handled by SDK, otherwise should be sent to application layer.
     */
    private fun handleSDKLevelGossip(
            secretName: String?,
            secretValue: String
    ): Boolean {
        return when (secretName) {
            MASTER_KEY_SSSS_NAME -> {
                crossSigningService.onSecretMSKGossip(secretValue)
                true
            }
            SELF_SIGNING_KEY_SSSS_NAME -> {
                crossSigningService.onSecretSSKGossip(secretValue)
                true
            }
            USER_SIGNING_KEY_SSSS_NAME -> {
                crossSigningService.onSecretUSKGossip(secretValue)
                true
            }
            KEYBACKUP_SECRET_SSSS_NAME -> {
                keysBackupService.onSecretKeyGossip(secretValue)
                true
            }
            else -> false
        }
    }

    /**
     * Handle an m.room.encryption event.
     *
     * @param roomId the room Id
     * @param event the encryption event.
     */
    private fun onRoomEncryptionEvent(roomId: String, event: Event) {
        if (!event.isStateEvent()) {
            // Ignore
            Timber.tag(loggerTag.value).w("Invalid encryption event")
            return
        }
        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
            val userIds = getRoomUserIds(roomId)
            setEncryptionInRoom(roomId, event.content?.get("algorithm")?.toString(), true, userIds)
        }
    }

    private fun getRoomUserIds(roomId: String): List<String> {
        val encryptForInvitedMembers = isEncryptionEnabledForInvitedUser() &&
                shouldEncryptForInvitedMembers(roomId)
        return cryptoSessionInfoProvider.getRoomUserIds(roomId, encryptForInvitedMembers)
    }

    /**
     * Handle a change in the membership state of a member of a room.
     *
     * @param roomId the room Id
     * @param event the membership event causing the change
     */
    private fun onRoomMembershipEvent(roomId: String, event: Event) {
        roomEncryptorsStore.get(roomId) ?: /* No encrypting in this room */ return

        event.stateKey?.let { userId ->
            val roomMember: RoomMemberContent? = event.content.toModel()
            val membership = roomMember?.membership
            if (membership == Membership.JOIN) {
                // make sure we are tracking the deviceList for this user.
                deviceListManager.startTrackingDeviceList(listOf(userId))
            } else if (membership == Membership.INVITE &&
                    shouldEncryptForInvitedMembers(roomId) &&
                    isEncryptionEnabledForInvitedUser()) {
                // track the deviceList for this invited user.
                // Caution: there's a big edge case here in that federated servers do not
                // know what other servers are in the room at the time they've been invited.
                // They therefore will not send device updates if a user logs in whilst
                // their state is invite.
                deviceListManager.startTrackingDeviceList(listOf(userId))
            }
        }
    }

    private fun onRoomHistoryVisibilityEvent(roomId: String, event: Event) {
        if (!event.isStateEvent()) return
        val eventContent = event.content.toModel<RoomHistoryVisibilityContent>()
        val historyVisibility = eventContent?.historyVisibility
        if (historyVisibility == null) {
            cryptoStore.setShouldShareHistory(roomId, false)
        } else {
            cryptoStore.setShouldEncryptForInvitedMembers(roomId, historyVisibility != RoomHistoryVisibility.JOINED)
            cryptoStore.setShouldShareHistory(roomId, historyVisibility.shouldShareHistory())
        }
    }

    /**
     * Upload my user's device keys.
     */
    private suspend fun uploadDeviceKeys() {
        if (cryptoStore.areDeviceKeysUploaded()) {
            Timber.tag(loggerTag.value).d("Keys already uploaded, nothing to do")
            return
        }
        // Prepare the device keys data to send
        // Sign it
        val canonicalJson = JsonCanonicalizer.getCanonicalJson(Map::class.java, getMyDevice().signalableJSONDictionary())
        var rest = getMyDevice().toRest()

        rest = rest.copy(
                signatures = objectSigner.signObject(canonicalJson)
        )

        val uploadDeviceKeysParams = UploadKeysTask.Params(rest, null, null)
        uploadKeysTask.execute(uploadDeviceKeysParams)

        cryptoStore.setDeviceKeysUploaded(true)
    }

    /**
     * Export the crypto keys.
     *
     * @param password the password
     * @return the exported keys
     */
    override suspend fun exportRoomKeys(password: String): ByteArray {
        return exportRoomKeys(password, MXMegolmExportEncryption.DEFAULT_ITERATION_COUNT)
    }

    /**
     * Export the crypto keys.
     *
     * @param password the password
     * @param anIterationCount the encryption iteration count (0 means no encryption)
     */
    private suspend fun exportRoomKeys(password: String, anIterationCount: Int): ByteArray {
        return withContext(coroutineDispatchers.crypto) {
            val iterationCount = max(0, anIterationCount)

            val exportedSessions = cryptoStore.getInboundGroupSessions().mapNotNull { it.exportKeys() }

            val adapter = MoshiProvider.providesMoshi()
                    .adapter(List::class.java)

            MXMegolmExportEncryption.encryptMegolmKeyFile(adapter.toJson(exportedSessions), password, iterationCount)
        }
    }

    /**
     * Import the room keys.
     *
     * @param roomKeysAsArray the room keys as array.
     * @param password the password
     * @param progressListener the progress listener
     * @return the result ImportRoomKeysResult
     */
    override suspend fun importRoomKeys(
            roomKeysAsArray: ByteArray,
            password: String,
            progressListener: ProgressListener?
    ): ImportRoomKeysResult {
        return withContext(coroutineDispatchers.crypto) {
            Timber.tag(loggerTag.value).v("importRoomKeys starts")

            val t0 = clock.epochMillis()
            val roomKeys = MXMegolmExportEncryption.decryptMegolmKeyFile(roomKeysAsArray, password)
            val t1 = clock.epochMillis()

            Timber.tag(loggerTag.value).v("importRoomKeys : decryptMegolmKeyFile done in ${t1 - t0} ms")

            val importedSessions = MoshiProvider.providesMoshi()
                    .adapter<List<MegolmSessionData>>(Types.newParameterizedType(List::class.java, MegolmSessionData::class.java))
                    .fromJson(roomKeys)

            val t2 = clock.epochMillis()

            Timber.tag(loggerTag.value).v("importRoomKeys : JSON parsing ${t2 - t1} ms")

            if (importedSessions == null) {
                throw Exception("Error")
            }

            megolmSessionDataImporter.handle(
                    megolmSessionsData = importedSessions,
                    fromBackup = false,
                    progressListener = progressListener
            )
        }
    }

    /**
     * Update the warn status when some unknown devices are detected.
     *
     * @param warn true to warn when some unknown devices are detected.
     */
    override fun setWarnOnUnknownDevices(warn: Boolean) {
        warnOnUnknownDevicesRepository.setWarnOnUnknownDevices(warn)
    }

    /**
     * Check if the user ids list have some unknown devices.
     * A success means there is no unknown devices.
     * If there are some unknown devices, a MXCryptoError.UnknownDevice exception is triggered.
     *
     * @param userIds the user ids list
     * @param callback the asynchronous callback.
     */
    fun checkUnknownDevices(userIds: List<String>, callback: MatrixCallback<Unit>) {
        // force the refresh to ensure that the devices list is up-to-date
        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
            runCatching {
                val keys = deviceListManager.downloadKeys(userIds, true)
                val unknownDevices = getUnknownDevices(keys)
                if (unknownDevices.map.isNotEmpty()) {
                    // trigger an an unknown devices exception
                    throw Failure.CryptoError(MXCryptoError.UnknownDevice(unknownDevices))
                }
            }.foldToCallback(callback)
        }
    }

    /**
     * Set the global override for whether the client should ever send encrypted
     * messages to unverified devices.
     * If false, it can still be overridden per-room.
     * If true, it overrides the per-room settings.
     *
     * @param block true to unilaterally blacklist all
     */
    override fun setGlobalBlacklistUnverifiedDevices(block: Boolean) {
        cryptoStore.setGlobalBlacklistUnverifiedDevices(block)
    }

    override fun enableKeyGossiping(enable: Boolean) {
        cryptoStore.enableKeyGossiping(enable)
    }

    override fun isKeyGossipingEnabled() = cryptoStore.isKeyGossipingEnabled()

    override fun isShareKeysOnInviteEnabled() = cryptoStore.isShareKeysOnInviteEnabled()

    override fun enableShareKeyOnInvite(enable: Boolean) = cryptoStore.enableShareKeyOnInvite(enable)

    /**
     * Tells whether the client should ever send encrypted messages to unverified devices.
     * The default value is false.
     * This function must be called in the getEncryptingThreadHandler() thread.
     *
     * @return true to unilaterally blacklist all unverified devices.
     */
    override fun getGlobalBlacklistUnverifiedDevices(): Boolean {
        return cryptoStore.getGlobalBlacklistUnverifiedDevices()
    }

    /**
     * Tells whether the client should encrypt messages only for the verified devices
     * in this room.
     * The default value is false.
     *
     * @param roomId the room id
     * @return true if the client should encrypt messages only for the verified devices.
     */
// TODO add this info in CryptoRoomEntity?
    override fun isRoomBlacklistUnverifiedDevices(roomId: String?): Boolean {
        return roomId?.let { cryptoStore.getRoomsListBlacklistUnverifiedDevices().contains(it) }
                ?: false
    }

    /**
     * Manages the room black-listing for unverified devices.
     *
     * @param roomId the room id
     * @param add true to add the room id to the list, false to remove it.
     */
    private fun setRoomBlacklistUnverifiedDevices(roomId: String, add: Boolean) {
        val roomIds = cryptoStore.getRoomsListBlacklistUnverifiedDevices().toMutableList()

        if (add) {
            if (roomId !in roomIds) {
                roomIds.add(roomId)
            }
        } else {
            roomIds.remove(roomId)
        }

        cryptoStore.setRoomsListBlacklistUnverifiedDevices(roomIds)
    }

    /**
     * Add this room to the ones which don't encrypt messages to unverified devices.
     *
     * @param roomId the room id
     */
    override fun setRoomBlacklistUnverifiedDevices(roomId: String) {
        setRoomBlacklistUnverifiedDevices(roomId, true)
    }

    /**
     * Remove this room to the ones which don't encrypt messages to unverified devices.
     *
     * @param roomId the room id
     */
    override fun setRoomUnBlacklistUnverifiedDevices(roomId: String) {
        setRoomBlacklistUnverifiedDevices(roomId, false)
    }

    /**
     * Re request the encryption keys required to decrypt an event.
     *
     * @param event the event to decrypt again.
     */
    override fun reRequestRoomKeyForEvent(event: Event) {
        outgoingKeyRequestManager.requestKeyForEvent(event, true)
    }

    override fun requestRoomKeyForEvent(event: Event) {
        outgoingKeyRequestManager.requestKeyForEvent(event, false)
    }

    /**
     * Add a GossipingRequestListener listener.
     *
     * @param listener listener
     */
    override fun addRoomKeysRequestListener(listener: GossipingRequestListener) {
        incomingKeyRequestManager.addRoomKeysRequestListener(listener)
        secretShareManager.addListener(listener)
    }

    /**
     * Add a GossipingRequestListener listener.
     *
     * @param listener listener
     */
    override fun removeRoomKeysRequestListener(listener: GossipingRequestListener) {
        incomingKeyRequestManager.removeRoomKeysRequestListener(listener)
        secretShareManager.removeListener(listener)
    }

    /**
     * Provides the list of unknown devices.
     *
     * @param devicesInRoom the devices map
     * @return the unknown devices map
     */
    private fun getUnknownDevices(devicesInRoom: MXUsersDevicesMap<CryptoDeviceInfo>): MXUsersDevicesMap<CryptoDeviceInfo> {
        val unknownDevices = MXUsersDevicesMap<CryptoDeviceInfo>()
        val userIds = devicesInRoom.userIds
        for (userId in userIds) {
            devicesInRoom.getUserDeviceIds(userId)?.forEach { deviceId ->
                devicesInRoom.getObject(userId, deviceId)
                        ?.takeIf { it.isUnknown }
                        ?.let {
                            unknownDevices.setObject(userId, deviceId, it)
                        }
            }
        }

        return unknownDevices
    }

    override fun downloadKeys(userIds: List<String>, forceDownload: Boolean, callback: MatrixCallback<MXUsersDevicesMap<CryptoDeviceInfo>>) {
        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
            runCatching {
                deviceListManager.downloadKeys(userIds, forceDownload)
            }.foldToCallback(callback)
        }
    }

    override fun addNewSessionListener(newSessionListener: NewSessionListener) {
        roomDecryptorProvider.addNewSessionListener(newSessionListener)
    }

    override fun removeSessionListener(listener: NewSessionListener) {
        roomDecryptorProvider.removeSessionListener(listener)
    }
/* ==========================================================================================
 * DEBUG INFO
 * ========================================================================================== */

    override fun toString(): String {
        return "DefaultCryptoService of $userId ($deviceId)"
    }

    override fun getOutgoingRoomKeyRequests(): List<OutgoingKeyRequest> {
        return cryptoStore.getOutgoingRoomKeyRequests()
    }

    override fun getOutgoingRoomKeyRequestsPaged(): LiveData<PagedList<OutgoingKeyRequest>> {
        return cryptoStore.getOutgoingRoomKeyRequestsPaged()
    }

    override fun getIncomingRoomKeyRequests(): List<IncomingRoomKeyRequest> {
        return cryptoStore.getGossipingEvents()
                .mapNotNull {
                    IncomingRoomKeyRequest.fromEvent(it)
                }
    }

    override fun getIncomingRoomKeyRequestsPaged(): LiveData<PagedList<IncomingRoomKeyRequest>> {
        return cryptoStore.getGossipingEventsTrail(TrailType.IncomingKeyRequest) {
            IncomingRoomKeyRequest.fromEvent(it)
                    ?: IncomingRoomKeyRequest(localCreationTimestamp = 0L)
        }
    }

    /**
     * If you registered a `GossipingRequestListener`, you will be notified of key request
     * that was not accepted by the SDK. You can call back this manually to accept anyhow.
     */
    override suspend fun manuallyAcceptRoomKeyRequest(request: IncomingRoomKeyRequest) {
        incomingKeyRequestManager.manuallyAcceptRoomKeyRequest(request)
    }

    override fun getGossipingEventsTrail(): LiveData<PagedList<AuditTrail>> {
        return cryptoStore.getGossipingEventsTrail()
    }

    override fun getGossipingEvents(): List<AuditTrail> {
        return cryptoStore.getGossipingEvents()
    }

    override fun getSharedWithInfo(roomId: String?, sessionId: String): MXUsersDevicesMap<Int> {
        return cryptoStore.getSharedWithInfo(roomId, sessionId)
    }

    override fun getWithHeldMegolmSession(roomId: String, sessionId: String): RoomKeyWithHeldContent? {
        return cryptoStore.getWithHeldMegolmSession(roomId, sessionId)
    }

    override fun prepareToEncrypt(roomId: String, callback: MatrixCallback<Unit>) {
        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
            Timber.tag(loggerTag.value).d("prepareToEncrypt() roomId:$roomId Check room members up to date")
            // Ensure to load all room members
            try {
                loadRoomMembersTask.execute(LoadRoomMembersTask.Params(roomId))
            } catch (failure: Throwable) {
                Timber.tag(loggerTag.value).e("prepareToEncrypt() : Failed to load room members")
                // we probably shouldn't block sending on that (but questionable)
                // but some members won't be able to decrypt
            }

            val userIds = getRoomUserIds(roomId)
            val alg = roomEncryptorsStore.get(roomId)
                    ?: getEncryptionAlgorithm(roomId)
                            ?.let { setEncryptionInRoom(roomId, it, false, userIds) }
                            ?.let { roomEncryptorsStore.get(roomId) }

            if (alg == null) {
                val reason = String.format(MXCryptoError.UNABLE_TO_ENCRYPT_REASON, MXCryptoError.NO_MORE_ALGORITHM_REASON)
                Timber.tag(loggerTag.value).e("prepareToEncrypt() : $reason")
                callback.onFailure(IllegalArgumentException("Missing algorithm"))
                return@launch
            }

            runCatching {
                (alg as? IMXGroupEncryption)?.preshareKey(userIds)
            }.fold(
                    { callback.onSuccess(Unit) },
                    {
                        Timber.tag(loggerTag.value).e(it, "prepareToEncrypt() failed.")
                        callback.onFailure(it)
                    }
            )
        }
    }

    override suspend fun sendSharedHistoryKeys(roomId: String, userId: String, sessionInfoSet: Set<SessionInfo>?) {
        deviceListManager.downloadKeys(listOf(userId), false)
        val userDevices = cryptoStore.getUserDeviceList(userId)
        val sessionToShare = sessionInfoSet.orEmpty().mapNotNull { sessionInfo ->
            // Get inbound session from sessionId and sessionKey
            withContext(coroutineDispatchers.crypto) {
                olmDevice.getInboundGroupSession(
                        sessionId = sessionInfo.sessionId,
                        senderKey = sessionInfo.senderKey,
                        roomId = roomId
                ).takeIf { it.wrapper.sessionData.sharedHistory }
            }
        }

        userDevices?.forEach { deviceInfo ->
            // Lets share the provided inbound sessions for every user device
            sessionToShare.forEach { inboundGroupSession ->
                val encryptor = roomEncryptorsStore.get(roomId)
                encryptor?.shareHistoryKeysWithDevice(inboundGroupSession, deviceInfo)
                Timber.i("## CRYPTO | Sharing inbound session")
            }
        }
    }

    /* ==========================================================================================
     * For test only
     * ========================================================================================== */

    @VisibleForTesting
    val cryptoStoreForTesting = cryptoStore

    @VisibleForTesting
    val olmDeviceForTest = olmDevice

    companion object {
        const val CRYPTO_MIN_FORCE_SESSION_PERIOD_MILLIS = 3_600_000 // one hour
    }
}
