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
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.jvm.Throws
import kotlin.math.max
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.NoOpMatrixCallback
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.crypto.MXCryptoConfig
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.listeners.ProgressListener
import org.matrix.android.sdk.api.session.crypto.CryptoService
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.crypto.crosssigning.KEYBACKUP_SECRET_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.MASTER_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.SELF_SIGNING_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.USER_SIGNING_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.keyshare.GossipingRequestListener
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibilityContent
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.OlmMachine
import org.matrix.android.sdk.internal.setRustLogger
import org.matrix.android.sdk.internal.crypto.actions.MegolmSessionDataImporter
import org.matrix.android.sdk.internal.crypto.algorithms.IMXEncrypting
import org.matrix.android.sdk.internal.crypto.algorithms.IMXWithHeldExtension
import org.matrix.android.sdk.internal.crypto.algorithms.megolm.MXMegolmEncryptionFactory
import org.matrix.android.sdk.internal.crypto.algorithms.olm.MXOlmEncryptionFactory
import org.matrix.android.sdk.internal.crypto.crosssigning.DefaultCrossSigningService
import org.matrix.android.sdk.internal.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.internal.crypto.keysbackup.DefaultKeysBackupService
import org.matrix.android.sdk.internal.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.internal.crypto.model.ImportRoomKeysResult
import org.matrix.android.sdk.internal.crypto.model.MXDeviceInfo
import org.matrix.android.sdk.internal.crypto.model.MXEncryptEventContentResult
import org.matrix.android.sdk.internal.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.internal.crypto.model.event.EncryptedEventContent
import org.matrix.android.sdk.internal.crypto.model.event.RoomKeyContent
import org.matrix.android.sdk.internal.crypto.model.event.RoomKeyWithHeldContent
import org.matrix.android.sdk.internal.crypto.model.event.SecretSendEventContent
import org.matrix.android.sdk.internal.crypto.model.rest.DeviceInfo
import org.matrix.android.sdk.internal.crypto.model.rest.DevicesListResponse
import org.matrix.android.sdk.internal.crypto.model.rest.KeysUploadResponse
import org.matrix.android.sdk.internal.crypto.model.rest.KeysClaimResponse
import org.matrix.android.sdk.internal.crypto.model.rest.KeysQueryResponse
import org.matrix.android.sdk.internal.crypto.model.rest.RoomKeyRequestBody
import org.matrix.android.sdk.internal.crypto.repository.WarnOnUnknownDeviceRepository
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.crypto.tasks.DeleteDeviceTask
import org.matrix.android.sdk.internal.crypto.tasks.DownloadKeysForUsersTask
import org.matrix.android.sdk.internal.crypto.tasks.GetDeviceInfoTask
import org.matrix.android.sdk.internal.crypto.tasks.GetDevicesTask
import org.matrix.android.sdk.internal.crypto.tasks.UploadKeysTask
import org.matrix.android.sdk.internal.crypto.tasks.SetDeviceNameTask
import org.matrix.android.sdk.internal.crypto.tasks.SendToDeviceTask
import org.matrix.android.sdk.internal.crypto.tasks.ClaimOneTimeKeysForUsersDeviceTask
import org.matrix.android.sdk.internal.crypto.verification.DefaultVerificationService
import org.matrix.android.sdk.internal.di.DeviceId
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.di.SessionFilesDirectory
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.extensions.foldToCallback
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.session.room.membership.LoadRoomMembersTask
import org.matrix.android.sdk.internal.session.sync.model.SyncResponse
import org.matrix.android.sdk.internal.session.sync.model.DeviceListResponse
import org.matrix.android.sdk.internal.session.sync.model.DeviceOneTimeKeysCountSyncResponse
import org.matrix.android.sdk.internal.session.sync.model.ToDeviceSyncResponse
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.TaskThread
import org.matrix.android.sdk.internal.task.configureWith
import org.matrix.android.sdk.internal.task.launchToCallback
import org.matrix.android.sdk.internal.util.MatrixCoroutineDispatchers
import org.matrix.olm.OlmManager
import timber.log.Timber
import uniffi.olm.Request
import uniffi.olm.RequestType

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
@SessionScope
internal class DefaultCryptoService @Inject constructor(
        // Olm Manager
        private val olmManager: OlmManager,
        @UserId
        private val userId: String,
        @DeviceId
        private val deviceId: String?,
        @SessionFilesDirectory
        private val dataDir: File,
        private val myDeviceInfoHolder: Lazy<MyDeviceInfoHolder>,
        // the crypto store
        private val cryptoStore: IMXCryptoStore,
        // Set of parameters used to configure/customize the end-to-end crypto.
        private val mxCryptoConfig: MXCryptoConfig,
        // Device list manager
        private val deviceListManager: DeviceListManager,
        // The key backup service.
        private val keysBackupService: DefaultKeysBackupService,
        //
        private val roomDecryptorProvider: RoomDecryptorProvider,
        // The verification service.
        private val verificationService: DefaultVerificationService,

        private val crossSigningService: DefaultCrossSigningService,
        // Actions
        private val megolmSessionDataImporter: MegolmSessionDataImporter,
        private val warnOnUnknownDevicesRepository: WarnOnUnknownDeviceRepository,
        // Tasks
        private val deleteDeviceTask: DeleteDeviceTask,
        private val getDevicesTask: GetDevicesTask,
        private val oneTimeKeysForUsersDeviceTask: ClaimOneTimeKeysForUsersDeviceTask,
        private val sendToDeviceTask: SendToDeviceTask,
        private val downloadKeysForUsersTask: DownloadKeysForUsersTask,
        private val getDeviceInfoTask: GetDeviceInfoTask,
        private val setDeviceNameTask: SetDeviceNameTask,
        private val uploadKeysTask: UploadKeysTask,
        private val loadRoomMembersTask: LoadRoomMembersTask,
        private val cryptoSessionInfoProvider: CryptoSessionInfoProvider,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val taskExecutor: TaskExecutor,
        private val cryptoCoroutineScope: CoroutineScope,
) : CryptoService {

    private val isStarting = AtomicBoolean(false)
    private val isStarted = AtomicBoolean(false)
    private var olmMachine: OlmMachine? = null

    suspend fun onStateEvent(roomId: String, event: Event) {
        when (event.getClearType()) {
            EventType.STATE_ROOM_ENCRYPTION         -> onRoomEncryptionEvent(roomId, event)
            EventType.STATE_ROOM_MEMBER             -> onRoomMembershipEvent(roomId, event)
            EventType.STATE_ROOM_HISTORY_VISIBILITY -> onRoomHistoryVisibilityEvent(roomId, event)
        }
    }

    suspend fun onLiveEvent(roomId: String, event: Event) {
        when (event.getClearType()) {
            EventType.STATE_ROOM_ENCRYPTION         -> onRoomEncryptionEvent(roomId, event)
            EventType.STATE_ROOM_MEMBER             -> onRoomMembershipEvent(roomId, event)
            EventType.STATE_ROOM_HISTORY_VISIBILITY -> onRoomHistoryVisibilityEvent(roomId, event)
        }
    }

    val gossipingBuffer = mutableListOf<Event>()

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
     * Provides the tracking status
     *
     * @param userId the user id
     * @return the tracking status
     */
    override fun getDeviceTrackingStatus(userId: String): Int {
        return cryptoStore.getDeviceTrackingStatus(userId, DeviceListManager.TRACKING_STATUS_NOT_TRACKED)
    }

    /**
     * Tell if the MXCrypto is started
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
    suspend fun start() {
        internalStart()
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

            // this can throw if no backup
            tryOrNull {
                keysBackupService.checkAndStartKeysBackup()
            }
        }
    }

    fun onSyncWillProcess(isInitialSync: Boolean) {
        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
            if (isInitialSync) {
                // TODO
            }
        }
    }

    private suspend fun internalStart() {
        if (isStarted.get() || isStarting.get()) {
            return
        }
        isStarting.set(true)
        Timber.v("HELLLO WORLD STARTING CRYPTO")

        try {
            setRustLogger()
            olmMachine = OlmMachine(userId, deviceId!!, dataDir)

            Timber.v("HELLLO WORLD STARTING $dataDir CRYPTO ${olmMachine?.identityKeys()}")
        } catch (throwable: Throwable) {
            Timber.v("HELLLO WORLD FAILED CRYPTO $throwable")
        }

        // Open the store
        cryptoStore.open()

        runCatching {
        }.fold(
                {
                    isStarting.set(false)
                    isStarted.set(true)
                },
                {
                    isStarting.set(false)
                    isStarted.set(false)
                    Timber.e(it, "Start failed")
                }
        )
    }

    /**
     * Close the crypto
     */
    fun close() = runBlocking(coroutineDispatchers.crypto) {
        cryptoCoroutineScope.coroutineContext.cancelChildren(CancellationException("Closing crypto module"))
        cryptoStore.close()
    }

    // Aways enabled on RiotX
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
     * A sync response has been received
     */
    suspend fun onSyncCompleted() {
        if (isStarted()) {
            sendOutgoingRequests()
        }

        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
            tryOrNull {
                gossipingBuffer.toList().let {
                    cryptoStore.saveGossipingEvents(it)
                }
                gossipingBuffer.clear()
            }
        }
    }

    /**
     * Find a device by curve25519 identity key
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
     * Provides the device information for a user id and a device Id
     *
     * @param userId   the user id
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
     * Set the devices as known
     *
     * @param devices  the devices. Note that the verified member of the devices in this list will not be updated by this method.
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
     * @param userId     the owner of the device
     * @param deviceId   the unique identifier for the device.
     */
    override fun setDeviceVerification(trustLevel: DeviceTrustLevel, userId: String, deviceId: String) {
        // TODO
    }

    /**
     * Configure a room to use encryption.
     *
     * @param roomId             the room id to enable encryption in.
     * @param algorithm          the encryption config for the room.
     * @param membersId          list of members to start tracking their devices
     * @return true if the operation succeeds.
     */
    private suspend fun setEncryptionInRoom(roomId: String,
                                            algorithm: String?,
                                            membersId: List<String>): Boolean {
        // If we already have encryption in this room, we should ignore this event
        // (for now at least. Maybe we should alert the user somehow?)
        val existingAlgorithm = cryptoStore.getRoomAlgorithm(roomId)

        if (!existingAlgorithm.isNullOrEmpty() && existingAlgorithm != algorithm) {
            Timber.e("## CRYPTO | setEncryptionInRoom() : Ignoring m.room.encryption event which requests a change of config in $roomId")
            return false
        }

        if (algorithm != MXCRYPTO_ALGORITHM_MEGOLM) {
            Timber.e("## CRYPTO | setEncryptionInRoom() : Unable to encrypt room $roomId with $algorithm")
            return false
        }

        cryptoStore.storeRoomAlgorithm(roomId, algorithm)

        // if encryption was not previously enabled in this room, we will have been
        // ignoring new device events for these users so far. We may well have
        // up-to-date lists for some users, for instance if we were sharing other
        // e2e rooms with them, so there is room for optimisation here, but for now
        // we just invalidate everyone in the room.
        if (null == existingAlgorithm) {
            Timber.v("Enabling encryption in $roomId for the first time; invalidating device lists for all users therein")

            val userIds = ArrayList(membersId)
            olmMachine!!.updateTrackedUsers(userIds)
        }

        return true
    }

    /**
     * Tells if a room is encrypted with MXCRYPTO_ALGORITHM_MEGOLM
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
     * @param eventType    the type of the event.
     * @param roomId       the room identifier the event will be sent.
     * @param callback     the asynchronous callback
     */
    override fun encryptEventContent(eventContent: Content,
                                     eventType: String,
                                     roomId: String,
                                     callback: MatrixCallback<MXEncryptEventContentResult>) {
        // moved to crypto scope to have uptodate values
        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
            val algorithm = getEncryptionAlgorithm(roomId)

            if (algorithm != null) {
                val userIds = getRoomUserIds(roomId)
                val t0 = System.currentTimeMillis()
                Timber.v("## CRYPTO | encryptEventContent() starts")
                runCatching {
                    preshareGroupSession(roomId, userIds)
                    val content = encrypt(roomId, eventType, eventContent)
                    Timber.v("## CRYPTO | encryptEventContent() : succeeds after ${System.currentTimeMillis() - t0} ms")
                    MXEncryptEventContentResult(content, EventType.ENCRYPTED)
                }.foldToCallback(callback)
            } else {
                val reason = String.format(MXCryptoError.UNABLE_TO_ENCRYPT_REASON, MXCryptoError.NO_MORE_ALGORITHM_REASON)
                Timber.e("## CRYPTO | encryptEventContent() : $reason")
                callback.onFailure(Failure.CryptoError(MXCryptoError.Base(MXCryptoError.ErrorType.UNABLE_TO_ENCRYPT, reason)))
            }
        }
    }

    override fun discardOutboundSession(roomId: String) {
        // TODO
    }

    /**
     * Decrypt an event
     *
     * @param event    the raw event.
     * @param timeline the id of the timeline where the event is decrypted. It is used to prevent replay attack.
     * @return the MXEventDecryptionResult data, or throw in case of error
     */
    @Throws(MXCryptoError::class)
    override fun decryptEvent(event: Event, timeline: String): MXEventDecryptionResult {
        val decrypted = runBlocking {
            olmMachine!!.decryptRoomEvent(event)
        }

        return decrypted
    }

    /**
     * Decrypt an event asynchronously
     *
     * @param event    the raw event.
     * @param timeline the id of the timeline where the event is decrypted. It is used to prevent replay attack.
     * @param callback the callback to return data or null
     */
    override fun decryptEventAsync(event: Event, timeline: String, callback: MatrixCallback<MXEventDecryptionResult>) {
        // This isn't really used anywhere, maybe just remove it?
        // TODO
    }

    /**
     * Handle an m.room.encryption event.
     *
     * @param event the encryption event.
     */
    private fun onRoomEncryptionEvent(roomId: String, event: Event) {
        if (!event.isStateEvent()) {
            // Ignore
            Timber.w("Invalid encryption event")
            return
        }
        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
            val params = LoadRoomMembersTask.Params(roomId)
            try {
                loadRoomMembersTask.execute(params)
            } catch (throwable: Throwable) {
                Timber.e(throwable, "## CRYPTO | onRoomEncryptionEvent ERROR FAILED TO SETUP CRYPTO ")
            } finally {
                val userIds = getRoomUserIds(roomId)
                olmMachine!!.updateTrackedUsers(userIds)
                setEncryptionInRoom(roomId, event.content?.get("algorithm")?.toString(), userIds)
            }
        }
    }

    private fun getRoomUserIds(roomId: String): List<String> {
        val encryptForInvitedMembers = isEncryptionEnabledForInvitedUser()
                && shouldEncryptForInvitedMembers(roomId)
        return cryptoSessionInfoProvider.getRoomUserIds(roomId, encryptForInvitedMembers)
    }

    /**
     * Handle a change in the membership state of a member of a room.
     *
     * @param event the membership event causing the change
     */
    private suspend fun onRoomMembershipEvent(roomId: String, event: Event) {
        // We only care about the memberships if this room is encrypted
        if (isRoomEncrypted(roomId)) {
            return
        }

        event.stateKey?.let { userId ->
            val roomMember: RoomMemberContent? = event.content.toModel()
            val membership = roomMember?.membership
            if (membership == Membership.JOIN) {
                // make sure we are tracking the deviceList for this user.
                olmMachine!!.updateTrackedUsers(listOf(userId))
            } else if (membership == Membership.INVITE
                    && shouldEncryptForInvitedMembers(roomId)
                    && isEncryptionEnabledForInvitedUser()) {
                // track the deviceList for this invited user.
                // Caution: there's a big edge case here in that federated servers do not
                // know what other servers are in the room at the time they've been invited.
                // They therefore will not send device updates if a user logs in whilst
                // their state is invite.
                olmMachine!!.updateTrackedUsers(listOf(userId))
            }
        }
    }

    private fun onRoomHistoryVisibilityEvent(roomId: String, event: Event) {
        val eventContent = event.content.toModel<RoomHistoryVisibilityContent>()
        eventContent?.historyVisibility?.let {
            cryptoStore.setShouldEncryptForInvitedMembers(roomId, it != RoomHistoryVisibility.JOINED)
        }
    }

    suspend fun receiveSyncChanges(
        toDevice: ToDeviceSyncResponse?,
        deviceChanges: DeviceListResponse?,
        keyCounts: DeviceOneTimeKeysCountSyncResponse?) {
            olmMachine!!.receiveSyncChanges(toDevice, deviceChanges, keyCounts)
    }

    private suspend fun preshareGroupSession(roomId: String, roomMembers: List<String>) {
        val request = olmMachine!!.getMissingSessions(roomMembers)

        if (request != null) {
            // This request can only be a keys claim request.
            when (request) {
                is Request.KeysClaim -> {
                    claimKeys(request)
                }
            }
        }

        for (toDeviceRequest in olmMachine!!.shareGroupSession(roomId, roomMembers)) {
            // This request can only be a to-device request.
            when (toDeviceRequest) {
                is Request.ToDevice -> {
                    sendToDevice(toDeviceRequest)
                }
            }
        }
    }

    private suspend fun encrypt(roomId: String, eventType: String, content: Content): Content {
        return olmMachine!!.encrypt(roomId, eventType, content)
    }

    private suspend fun uploadKeys(outgoingRequest: Request.KeysUpload) {
        val body = MoshiProvider
            .providesMoshi()
            .adapter<JsonDict>(Map::class.java)
            .fromJson(outgoingRequest.body)!!

        val request = UploadKeysTask.Params(body)

        val response = uploadKeysTask.execute(request)
        val adapter = MoshiProvider
            .providesMoshi()
            .adapter<KeysUploadResponse>(KeysUploadResponse::class.java)

        val json_response = adapter.toJson(response)!!

        olmMachine!!.markRequestAsSent(
            outgoingRequest.requestId,
            RequestType.KEYS_UPLOAD,
            json_response
        )
    }

    private suspend fun queryKeys(outgoingRequest: Request.KeysQuery) {
        Timber.v("HELLO KEYS QUERY REQUEST ${outgoingRequest.users}")
        val params = DownloadKeysForUsersTask.Params(outgoingRequest.users, null)

        try {
            val response = downloadKeysForUsersTask.execute(params)
            val adapter = MoshiProvider.providesMoshi().adapter<KeysQueryResponse>(KeysQueryResponse::class.java)
            val json_response = adapter.toJson(response)!!
            olmMachine!!.markRequestAsSent(outgoingRequest.requestId, RequestType.KEYS_QUERY, json_response)
        } catch (throwable: Throwable) {
            Timber.e(throwable, "## CRYPTO | doKeyDownloadForUsers(): error")
        }
    }

    private suspend fun sendToDevice(request: Request.ToDevice) {
        // TODO this produces floats for the Olm type fields, which
        // are integers originally.
        val adapter = MoshiProvider
            .providesMoshi()
            .adapter<Map<String, HashMap<String, Any>>>(Map::class.java)
        val body = adapter.fromJson(request.body)!!

        val userMap = MXUsersDevicesMap<Any>()
        userMap.join(body)

        val sendToDeviceParams = SendToDeviceTask.Params(request.eventType, userMap)
        sendToDeviceTask.execute(sendToDeviceParams)
        olmMachine!!.markRequestAsSent(request.requestId, RequestType.TO_DEVICE, "{}")
    }

    private suspend fun claimKeys(request: Request.KeysClaim) {
        val claimParams = ClaimOneTimeKeysForUsersDeviceTask.Params(request.oneTimeKeys)
        val response = oneTimeKeysForUsersDeviceTask.execute(claimParams)
        val adapter = MoshiProvider
            .providesMoshi()
            .adapter<KeysClaimResponse>(KeysClaimResponse::class.java)
        val json_response = adapter.toJson(response)!!

        olmMachine!!.markRequestAsSent(request.requestId, RequestType.KEYS_CLAIM, json_response)
    }

    private suspend fun sendOutgoingRequests() {
        // TODO these requests should be sent out in parallel
        for (outgoingRequest in olmMachine!!.outgoingRequests()) {
            when (outgoingRequest) {
                is Request.KeysUpload -> {
                    uploadKeys(outgoingRequest)
                }
                is Request.KeysQuery -> {
                    queryKeys(outgoingRequest)
                }
                is Request.ToDevice -> {
                    // Timber.v("HELLO TO DEVICE REQUEST ${outgoingRequest.body}")
                }
            }
        }
    }

    /**
     * Export the crypto keys
     *
     * @param password the password
     * @param callback the exported keys
     */
    override fun exportRoomKeys(password: String, callback: MatrixCallback<ByteArray>) {
        cryptoCoroutineScope.launch(coroutineDispatchers.main) {
            runCatching {
                exportRoomKeys(password, MXMegolmExportEncryption.DEFAULT_ITERATION_COUNT)
            }.foldToCallback(callback)
        }
    }

    /**
     * Export the crypto keys
     *
     * @param password         the password
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
     * Import the room keys
     *
     * @param roomKeysAsArray  the room keys as array.
     * @param password         the password
     * @param progressListener the progress listener
     * @param callback         the asynchronous callback.
     */
    override fun importRoomKeys(roomKeysAsArray: ByteArray,
                                password: String,
                                progressListener: ProgressListener?,
                                callback: MatrixCallback<ImportRoomKeysResult>) {
        cryptoCoroutineScope.launch(coroutineDispatchers.main) {
            runCatching {
                withContext(coroutineDispatchers.crypto) {
                    Timber.v("## CRYPTO | importRoomKeys starts")

                    val t0 = System.currentTimeMillis()
                    val roomKeys = MXMegolmExportEncryption.decryptMegolmKeyFile(roomKeysAsArray, password)
                    val t1 = System.currentTimeMillis()

                    Timber.v("## CRYPTO | importRoomKeys : decryptMegolmKeyFile done in ${t1 - t0} ms")

                    val importedSessions = MoshiProvider.providesMoshi()
                            .adapter<List<MegolmSessionData>>(Types.newParameterizedType(List::class.java, MegolmSessionData::class.java))
                            .fromJson(roomKeys)

                    val t2 = System.currentTimeMillis()

                    Timber.v("## CRYPTO | importRoomKeys : JSON parsing ${t2 - t1} ms")

                    if (importedSessions == null) {
                        throw Exception("Error")
                    }

                    megolmSessionDataImporter.handle(
                            megolmSessionsData = importedSessions,
                            fromBackup = false,
                            progressListener = progressListener
                    )
                }
            }.foldToCallback(callback)
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
     * Set the global override for whether the client should ever send encrypted
     * messages to unverified devices.
     * If false, it can still be overridden per-room.
     * If true, it overrides the per-room settings.
     *
     * @param block    true to unilaterally blacklist all
     */
    override fun setGlobalBlacklistUnverifiedDevices(block: Boolean) {
        cryptoStore.setGlobalBlacklistUnverifiedDevices(block)
    }

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
     * @param roomId   the room id
     * @param add      true to add the room id to the list, false to remove it.
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
     * @param roomId   the room id
     */
    override fun setRoomBlacklistUnverifiedDevices(roomId: String) {
        setRoomBlacklistUnverifiedDevices(roomId, true)
    }

    /**
     * Remove this room to the ones which don't encrypt messages to unverified devices.
     *
     * @param roomId   the room id
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
        // TODO
    }

    /**
     * Add a GossipingRequestListener listener.
     *
     * @param listener listener
     */
    override fun addRoomKeysRequestListener(listener: GossipingRequestListener) {
        // TODO
    }

    /**
     * Add a GossipingRequestListener listener.
     *
     * @param listener listener
     */
    override fun removeRoomKeysRequestListener(listener: GossipingRequestListener) {
        // TODO
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

    override fun getOutgoingRoomKeyRequests(): List<OutgoingRoomKeyRequest> {
        return cryptoStore.getOutgoingRoomKeyRequests()
    }

    override fun getOutgoingRoomKeyRequestsPaged(): LiveData<PagedList<OutgoingRoomKeyRequest>> {
        return cryptoStore.getOutgoingRoomKeyRequestsPaged()
    }

    override fun getIncomingRoomKeyRequestsPaged(): LiveData<PagedList<IncomingRoomKeyRequest>> {
        return cryptoStore.getIncomingRoomKeyRequestsPaged()
    }

    override fun getIncomingRoomKeyRequests(): List<IncomingRoomKeyRequest> {
        return cryptoStore.getIncomingRoomKeyRequests()
    }

    override fun getGossipingEventsTrail(): LiveData<PagedList<Event>> {
        return cryptoStore.getGossipingEventsTrail()
    }

    override fun getGossipingEvents(): List<Event> {
        return cryptoStore.getGossipingEvents()
    }

    override fun getSharedWithInfo(roomId: String?, sessionId: String): MXUsersDevicesMap<Int> {
        return cryptoStore.getSharedWithInfo(roomId, sessionId)
    }

    override fun getWithHeldMegolmSession(roomId: String, sessionId: String): RoomKeyWithHeldContent? {
        return cryptoStore.getWithHeldMegolmSession(roomId, sessionId)
    }

    override fun logDbUsageInfo() {
        cryptoStore.logDbUsageInfo()
    }

    /* ==========================================================================================
     * For test only
     * ========================================================================================== */

    @VisibleForTesting
    val cryptoStoreForTesting = cryptoStore

    companion object {
        const val CRYPTO_MIN_FORCE_SESSION_PERIOD_MILLIS = 3_600_000 // one hour
    }
}
