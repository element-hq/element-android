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

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.paging.PagedList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixConfiguration
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.api.crypto.MXCryptoConfig
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.listeners.ProgressListener
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.crypto.CryptoService
import org.matrix.android.sdk.api.session.crypto.GlobalCryptoConfig
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.crypto.NewSessionListener
import org.matrix.android.sdk.api.session.crypto.OutgoingKeyRequest
import org.matrix.android.sdk.api.session.crypto.crosssigning.CrossSigningService
import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.api.session.crypto.keyshare.GossipingRequestListener
import org.matrix.android.sdk.api.session.crypto.model.AuditTrail
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.ForwardedRoomKeyContent
import org.matrix.android.sdk.api.session.crypto.model.ImportRoomKeysResult
import org.matrix.android.sdk.api.session.crypto.model.IncomingRoomKeyRequest
import org.matrix.android.sdk.api.session.crypto.model.MXEncryptEventContentResult
import org.matrix.android.sdk.api.session.crypto.model.MXEventDecryptionResult
import org.matrix.android.sdk.api.session.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.content.EncryptedEventContent
import org.matrix.android.sdk.api.session.events.model.content.EncryptionEventContent
import org.matrix.android.sdk.api.session.events.model.content.RoomKeyContent
import org.matrix.android.sdk.api.session.events.model.content.RoomKeyWithHeldContent
import org.matrix.android.sdk.api.session.events.model.content.SecretSendEventContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibilityContent
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.model.shouldShareHistory
import org.matrix.android.sdk.api.session.sync.model.DeviceListResponse
import org.matrix.android.sdk.api.session.sync.model.DeviceOneTimeKeysCountSyncResponse
import org.matrix.android.sdk.api.session.sync.model.SyncResponse
import org.matrix.android.sdk.api.session.sync.model.ToDeviceSyncResponse
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toOptional
import org.matrix.android.sdk.internal.crypto.keysbackup.RustKeyBackupService
import org.matrix.android.sdk.internal.crypto.model.SessionInfo
import org.matrix.android.sdk.internal.crypto.network.OutgoingRequestsProcessor
import org.matrix.android.sdk.internal.crypto.repository.WarnOnUnknownDeviceRepository
import org.matrix.android.sdk.internal.crypto.store.IMXCommonCryptoStore
import org.matrix.android.sdk.internal.crypto.store.db.CryptoStoreAggregator
import org.matrix.android.sdk.internal.crypto.tasks.DeleteDeviceTask
import org.matrix.android.sdk.internal.crypto.tasks.GetDeviceInfoTask
import org.matrix.android.sdk.internal.crypto.tasks.GetDevicesTask
import org.matrix.android.sdk.internal.crypto.tasks.SetDeviceNameTask
import org.matrix.android.sdk.internal.crypto.tasks.toDeviceTracingId
import org.matrix.android.sdk.internal.crypto.verification.RustVerificationService
import org.matrix.android.sdk.internal.di.DeviceId
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.session.StreamEventsManager
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

private val loggerTag = LoggerTag("RustCryptoService", LoggerTag.CRYPTO)

@SessionScope
internal class RustCryptoService @Inject constructor(
        @UserId private val myUserId: String,
        @DeviceId private val deviceId: String,
        // the crypto store
        private val cryptoStore: IMXCommonCryptoStore,
        // Set of parameters used to configure/customize the end-to-end crypto.
        private val mxCryptoConfig: MXCryptoConfig,
        // Actions
        private val warnOnUnknownDevicesRepository: WarnOnUnknownDeviceRepository,
        // Tasks
        private val deleteDeviceTask: DeleteDeviceTask,
        private val getDevicesTask: GetDevicesTask,
        private val getDeviceInfoTask: GetDeviceInfoTask,
        private val setDeviceNameTask: SetDeviceNameTask,
        private val cryptoSessionInfoProvider: CryptoSessionInfoProvider,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val cryptoCoroutineScope: CoroutineScope,
        private val olmMachine: OlmMachine,
        private val crossSigningService: CrossSigningService,
        private val verificationService: RustVerificationService,
        private val keysBackupService: RustKeyBackupService,
        private val megolmSessionImportManager: MegolmSessionImportManager,
        private val liveEventManager: dagger.Lazy<StreamEventsManager>,
        private val prepareToEncrypt: PrepareToEncryptUseCase,
        private val encryptEventContent: EncryptEventContentUseCase,
        private val getRoomUserIds: GetRoomUserIdsUseCase,
        private val outgoingRequestsProcessor: OutgoingRequestsProcessor,
        private val matrixConfiguration: MatrixConfiguration,
        private val perSessionBackupQueryRateLimiter: PerSessionBackupQueryRateLimiter,
) : CryptoService {

    private val isStarting = AtomicBoolean(false)
    private val isStarted = AtomicBoolean(false)

    override fun name() = "rust-sdk"

    override suspend fun onStateEvent(roomId: String, event: Event, cryptoStoreAggregator: CryptoStoreAggregator?) {
        when (event.type) {
            EventType.STATE_ROOM_ENCRYPTION -> onRoomEncryptionEvent(roomId, event)
            EventType.STATE_ROOM_MEMBER -> onRoomMembershipEvent(roomId, event)
            EventType.STATE_ROOM_HISTORY_VISIBILITY -> onRoomHistoryVisibilityEvent(roomId, event, cryptoStoreAggregator)
        }
    }

    override suspend fun onLiveEvent(roomId: String, event: Event, isInitialSync: Boolean, cryptoStoreAggregator: CryptoStoreAggregator?) {
        if (event.isStateEvent()) {
            when (event.getClearType()) {
                EventType.STATE_ROOM_ENCRYPTION -> onRoomEncryptionEvent(roomId, event)
                EventType.STATE_ROOM_MEMBER -> onRoomMembershipEvent(roomId, event)
                EventType.STATE_ROOM_HISTORY_VISIBILITY -> onRoomHistoryVisibilityEvent(roomId, event, cryptoStoreAggregator)
            }
        } else {
            if (!isInitialSync) {
                verificationService.onEvent(roomId, event)
            }
        }
    }

    override suspend fun setDeviceName(deviceId: String, deviceName: String) {
        val params = SetDeviceNameTask.Params(deviceId, deviceName)
        setDeviceNameTask.execute(params)
        try {
            downloadKeysIfNeeded(listOf(myUserId), true)
        } catch (failure: Throwable) {
            Timber.tag(loggerTag.value).w(failure, "setDeviceName: Failed to refresh of crypto device")
        }
    }

    override suspend fun deleteDevices(deviceIds: List<String>, userInteractiveAuthInterceptor: UserInteractiveAuthInterceptor) {
        val params = DeleteDeviceTask.Params(deviceIds, userInteractiveAuthInterceptor, null)
        deleteDeviceTask.execute(params)
    }

    override suspend fun deleteDevice(deviceId: String, userInteractiveAuthInterceptor: UserInteractiveAuthInterceptor) {
        deleteDevices(listOf(deviceId), userInteractiveAuthInterceptor)
    }

    override suspend fun getMyCryptoDevice(): CryptoDeviceInfo = withContext(coroutineDispatchers.io) {
        olmMachine.ownDevice()
    }

    override suspend fun fetchDevicesList(): List<DeviceInfo> {
        val devicesList: List<DeviceInfo>
        withContext(coroutineDispatchers.io) {
            devicesList = getDevicesTask.execute(Unit).devices.orEmpty()
            cryptoStore.saveMyDevicesInfo(devicesList)
        }
        return devicesList
    }

    override fun getMyDevicesInfo(): List<DeviceInfo> {
        return cryptoStore.getMyDevicesInfo()
    }

    override fun getMyDevicesInfoLive(): LiveData<List<DeviceInfo>> {
        return cryptoStore.getLiveMyDevicesInfo()
    }

    override suspend fun fetchDeviceInfo(deviceId: String): DeviceInfo {
        val params = GetDeviceInfoTask.Params(deviceId)
        return getDeviceInfoTask.execute(params)
    }

    override suspend fun inboundGroupSessionsCount(onlyBackedUp: Boolean): Int {
        return if (onlyBackedUp) {
            keysBackupService.getTotalNumbersOfBackedUpKeys()
        } else {
            keysBackupService.getTotalNumbersOfKeys()
        }
        // return cryptoStore.inboundGroupSessionsCount(onlyBackedUp)
    }

    /**
     * Tell if the MXCrypto is started.
     *
     * @return true if the crypto is started
     */
    override fun isStarted(): Boolean {
        return isStarted.get()
    }

    /**
     * Start the crypto module.
     * Device keys will be uploaded, then one time keys if there are not enough on the homeserver
     * and, then, if this is the first time, this new device will be announced to all other users
     * devices.
     *
     */
    override fun start() {
        internalStart()
        cryptoCoroutineScope.launch(coroutineDispatchers.io) {
            cryptoStore.open()
            // Just update
            tryOrNull { fetchDevicesList() }
            cryptoStore.tidyUpDataBase()
        }
    }

    private fun internalStart() {
        if (isStarted.get() || isStarting.get()) {
            return
        }
        isStarting.set(true)

        try {
            setRustLogger()
            Timber.tag(loggerTag.value).v(
                    "## CRYPTO | Successfully started up an Olm machine for " +
                            "$myUserId, $deviceId, identity keys: ${this.olmMachine.identityKeys()}"
            )
        } catch (throwable: Throwable) {
            Timber.tag(loggerTag.value).v("Failed create an Olm machine: $throwable")
        }

        // After the initial rust migration the current keys & signature might not be there
        // The session is then in an invalid state and can fire unexpected verify popups
        // this will only do network request once.
        cryptoCoroutineScope.launch(coroutineDispatchers.io) {
            tryOrNull {
                downloadKeysIfNeeded(listOf(myUserId), false)
            }
        }

        // We try to enable key backups, if the backup version on the server is trusted,
        // we're gonna continue backing up.
        cryptoCoroutineScope.launch {
            tryOrNull {
                keysBackupService.checkAndStartKeysBackup()
            }
        }

        isStarting.set(false)
        isStarted.set(true)
    }

    /**
     * Close the crypto.
     */
    override fun close() {
        cryptoCoroutineScope.coroutineContext.cancelChildren(CancellationException("Closing crypto module"))
        cryptoCoroutineScope.launch {
            withContext(NonCancellable) {
                cryptoStore.close()
            }
        }
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
     */
    override suspend fun onSyncCompleted(syncResponse: SyncResponse, cryptoStoreAggregator: CryptoStoreAggregator) {
        if (isStarted()) {
            cryptoStore.storeData(cryptoStoreAggregator)

            outgoingRequestsProcessor.processOutgoingRequests(olmMachine)
            // This isn't a copy paste error. Sending the outgoing requests may
            // claim one-time keys and establish 1-to-1 Olm sessions with devices, while some
            // outgoing requests are waiting for an Olm session to be established (e.g. forwarding
            // room keys or sharing secrets).

            // The second call sends out those requests that are waiting for the
            // keys claim request to be sent out.
            // This could be omitted but then devices might be waiting for the next
            outgoingRequestsProcessor.processOutgoingRequests(olmMachine)
        }
    }

    /**
     * Provides the device information for a user id and a device Id.
     *
     * @param userId the user id
     * @param deviceId the device id
     */
    override suspend fun getCryptoDeviceInfo(userId: String, deviceId: String?): CryptoDeviceInfo? {
        if (userId.isEmpty() || deviceId.isNullOrEmpty()) return null
        return withContext(coroutineDispatchers.io) { olmMachine.getCryptoDeviceInfo(userId, deviceId) }
    }

    override suspend fun getCryptoDeviceInfo(userId: String): List<CryptoDeviceInfo> {
        return withContext(coroutineDispatchers.io) {
            olmMachine.getCryptoDeviceInfo(userId)
        }
    }

    override fun getLiveCryptoDeviceInfo(): LiveData<List<CryptoDeviceInfo>> {
        return getLiveCryptoDeviceInfo(listOf(myUserId))
    }

    override fun getLiveCryptoDeviceInfo(userId: String): LiveData<List<CryptoDeviceInfo>> {
        return getLiveCryptoDeviceInfo(listOf(userId))
    }

    override fun getLiveCryptoDeviceInfo(userIds: List<String>): LiveData<List<CryptoDeviceInfo>> {
        return olmMachine.getLiveDevices(userIds)
    }

    override fun getLiveCryptoDeviceInfoWithId(deviceId: String): LiveData<Optional<CryptoDeviceInfo>> {
        return getLiveCryptoDeviceInfo().map {
            it.find { it.deviceId == deviceId }.toOptional()
        }
    }

    override suspend fun getCryptoDeviceInfoList(userId: String): List<CryptoDeviceInfo> {
        return olmMachine.getCryptoDeviceInfo(userId)
    }

    override fun getMyDevicesInfoLive(deviceId: String): LiveData<Optional<DeviceInfo>> {
        return cryptoStore.getLiveMyDevicesInfo(deviceId)
    }

//    override fun getLiveCryptoDeviceInfoList(userId: String) = getLiveCryptoDeviceInfoList(listOf(userId))
//
//    override fun getLiveCryptoDeviceInfoList(userIds: List<String>): Flow<List<CryptoDeviceInfo>> {
//        return olmMachine.getLiveDevices(userIds)
//    }

    /**
     * Configure a room to use encryption.
     *
     * @param roomId the room id to enable encryption in.
     * @param info the encryption config for the room.
     * @param membersId list of members to start tracking their devices
     * @return true if the operation succeeds.
     */
    private suspend fun setEncryptionInRoom(
            roomId: String,
            info: EncryptionEventContent?,
            membersId: List<String>
    ): Boolean {
        // If we already have encryption in this room, we should ignore this event
        // (for now at least. Maybe we should alert the user somehow?)
        val existingAlgorithm = cryptoStore.getRoomAlgorithm(roomId)
        val algorithm = info?.algorithm

        if (!existingAlgorithm.isNullOrEmpty() && existingAlgorithm != algorithm) {
            Timber.tag(loggerTag.value).e("setEncryptionInRoom() : Ignoring m.room.encryption event which requests a change of config in $roomId")
            return false
        }

        // TODO CHECK WITH VALERE
        cryptoStore.setAlgorithmInfo(roomId, info)

        if (algorithm != MXCRYPTO_ALGORITHM_MEGOLM) {
            Timber.tag(loggerTag.value).e("## CRYPTO | setEncryptionInRoom() : Unable to encrypt room $roomId with $algorithm")
            return false
        }

        // if encryption was not previously enabled in this room, we will have been
        // ignoring new device events for these users so far. We may well have
        // up-to-date lists for some users, for instance if we were sharing other
        // e2e rooms with them, so there is room for optimisation here, but for now
        // we just invalidate everyone in the room.
        if (null == existingAlgorithm) {
            Timber.tag(loggerTag.value).d("Enabling encryption in $roomId for the first time; invalidating device lists for all users therein")

            val userIds = ArrayList(membersId)
            olmMachine.updateTrackedUsers(userIds)
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
    override suspend fun getUserDevices(userId: String): MutableList<CryptoDeviceInfo> {
        return this.getCryptoDeviceInfoList(userId).toMutableList()
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
     */
    override suspend fun encryptEventContent(
            eventContent: Content,
            eventType: String,
            roomId: String
    ): MXEncryptEventContentResult {
        return encryptEventContent.invoke(eventContent, eventType, roomId)
    }

    override fun discardOutboundSession(roomId: String) {
        olmMachine.discardRoomKey(roomId)
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
        return try {
            olmMachine.decryptRoomEvent(event).also {
                liveEventManager.get().dispatchLiveEventDecrypted(event, it)
            }
        } catch (mxCryptoError: MXCryptoError) {
            liveEventManager.get().dispatchLiveEventDecryptionFailed(event, mxCryptoError)
            if (mxCryptoError is MXCryptoError.Base && (
                            mxCryptoError.errorType == MXCryptoError.ErrorType.UNKNOWN_INBOUND_SESSION_ID ||
                                    mxCryptoError.errorType == MXCryptoError.ErrorType.UNKNOWN_MESSAGE_INDEX)) {
                Timber.v("Try to perform a lazy migration from legacy store")
                /**
                 * It's a bit hacky, check how this can be better integrated with rust?
                 */
                val content = event.content?.toModel<EncryptedEventContent>() ?: throw mxCryptoError
                val roomId = event.roomId
                val sessionId = content.sessionId
                if (roomId != null && sessionId != null) {
                    perSessionBackupQueryRateLimiter.tryFromBackupIfPossible(sessionId, roomId)
                }
            }
            throw mxCryptoError
        }
    }

    /**
     * Handle an m.room.encryption event.
     *
     * @param roomId the roomId.
     * @param event the encryption event.
     */
    private suspend fun onRoomEncryptionEvent(roomId: String, event: Event) {
        if (!event.isStateEvent()) {
            // Ignore
            Timber.tag(loggerTag.value).w("Invalid encryption event")
            return
        }

        // Do not load members here, would defeat lazy loading
//        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
//            val params = LoadRoomMembersTask.Params(roomId)
//            try {
//                loadRoomMembersTask.execute(params)
//            } catch (throwable: Throwable) {
//                Timber.e(throwable, "## CRYPTO | onRoomEncryptionEvent ERROR FAILED TO SETUP CRYPTO ")
//            } finally {
        val userIds = getRoomUserIds(roomId)
        setEncryptionInRoom(roomId, event.content?.toModel<EncryptionEventContent>(), userIds)
//            }
//        }
    }

    override fun onE2ERoomMemberLoadedFromServer(roomId: String) {
        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
            val userIds = getRoomUserIds(roomId)
            // Because of LL we might want to update tracked users
            olmMachine.updateTrackedUsers(userIds)
        }
    }

    override suspend fun deviceWithIdentityKey(userId: String, senderKey: String, algorithm: String): CryptoDeviceInfo? {
        return olmMachine.getCryptoDeviceInfo(userId)
                .firstOrNull { it.identityKey() == senderKey }
    }

    override suspend fun setDeviceVerification(trustLevel: DeviceTrustLevel, userId: String, deviceId: String) {
        Timber.w("Rust stack only support API to set local trust")
        olmMachine.setDeviceLocalTrust(userId, deviceId, trustLevel.isLocallyVerified().orFalse())
    }

    /**
     * Handle a change in the membership state of a member of a room.
     *
     * @param roomId the roomId
     * @param event the membership event causing the change
     */
    private suspend fun onRoomMembershipEvent(roomId: String, event: Event) {
        // We only care about the memberships if this room is encrypted
        if (!isRoomEncrypted(roomId)) {
            return
        }
        event.stateKey?.let { userId ->
            val roomMember: RoomMemberContent? = event.content.toModel()
            val membership = roomMember?.membership
            if (membership == Membership.JOIN) {
                // make sure we are tracking the deviceList for this user.
                cryptoCoroutineScope.launch {
                    olmMachine.updateTrackedUsers(listOf(userId))
                }
            } else if (membership == Membership.INVITE &&
                    shouldEncryptForInvitedMembers(roomId) &&
                    isEncryptionEnabledForInvitedUser()) {
                // track the deviceList for this invited user.
                // Caution: there's a big edge case here in that federated servers do not
                // know what other servers are in the room at the time they've been invited.
                // They therefore will not send device updates if a user logs in whilst
                // their state is invite.
                olmMachine.updateTrackedUsers(listOf(userId))
            } else {
                // nop
            }
        }
    }

    private fun onRoomHistoryVisibilityEvent(roomId: String, event: Event, cryptoStoreAggregator: CryptoStoreAggregator?) {
        if (!event.isStateEvent()) return
        val eventContent = event.content.toModel<RoomHistoryVisibilityContent>()
        val historyVisibility = eventContent?.historyVisibility
        if (historyVisibility == null) {
            if (cryptoStoreAggregator != null) {
                cryptoStoreAggregator.setShouldShareHistoryData[roomId] = false
            } else {
                cryptoStore.setShouldShareHistory(roomId, false)
            }
        } else {
            if (cryptoStoreAggregator != null) {
                // encryption for the invited members will be blocked if the history visibility is "joined"
                cryptoStoreAggregator.setShouldEncryptForInvitedMembersData[roomId] = historyVisibility != RoomHistoryVisibility.JOINED
                cryptoStoreAggregator.setShouldShareHistoryData[roomId] = historyVisibility.shouldShareHistory()
            } else {
                // encryption for the invited members will be blocked if the history visibility is "joined"
                cryptoStore.setShouldEncryptForInvitedMembers(roomId, historyVisibility != RoomHistoryVisibility.JOINED)
                cryptoStore.setShouldShareHistory(roomId, historyVisibility.shouldShareHistory())
            }
        }
    }

    private fun notifyRoomKeyReceived(
            roomId: String?,
            sessionId: String,
    ) {
        megolmSessionImportManager.dispatchNewSession(roomId, sessionId)
        cryptoCoroutineScope.launch {
            keysBackupService.maybeBackupKeys()
        }
    }

    override suspend fun onSyncWillProcess(isInitialSync: Boolean) {
        // nothing no rust
    }

    override suspend fun receiveSyncChanges(
            toDevice: ToDeviceSyncResponse?,
            deviceChanges: DeviceListResponse?,
            keyCounts: DeviceOneTimeKeysCountSyncResponse?,
            deviceUnusedFallbackKeyTypes: List<String>?,
            nextBatch: String?,
    ) {
        // Decrypt and handle our to-device events
        val toDeviceEvents = this.olmMachine.receiveSyncChanges(toDevice, deviceChanges, keyCounts, deviceUnusedFallbackKeyTypes, nextBatch)

        // Notify the our listeners about room keys so decryption is retried.
        toDeviceEvents.events.orEmpty().forEach { event ->
            Timber.tag(loggerTag.value)
                    .d("[${myUserId.take(7)}|${deviceId}] Processed ToDevice event msgid:${event.toDeviceTracingId()} id:${event.eventId} type:${event.type}")

            if (event.getClearType() == EventType.ENCRYPTED) {
                // rust failed to decrypt it
                matrixConfiguration.cryptoAnalyticsPlugin?.onFailToDecryptToDevice(
                        Throwable("receiveSyncChanges")
                )
            }
            when (event.type) {
                EventType.ROOM_KEY -> {
                    val content = event.getClearContent().toModel<RoomKeyContent>() ?: return@forEach

                    val roomId = content.roomId
                    val sessionId = content.sessionId ?: return@forEach

                    notifyRoomKeyReceived(roomId, sessionId)
                    matrixConfiguration.cryptoAnalyticsPlugin?.onRoomKeyImported(sessionId, EventType.ROOM_KEY)
                }
                EventType.FORWARDED_ROOM_KEY -> {
                    val content = event.getClearContent().toModel<ForwardedRoomKeyContent>() ?: return@forEach

                    val roomId = content.roomId
                    val sessionId = content.sessionId ?: return@forEach

                    notifyRoomKeyReceived(roomId, sessionId)
                    matrixConfiguration.cryptoAnalyticsPlugin?.onRoomKeyImported(sessionId, EventType.FORWARDED_ROOM_KEY)
                }
                EventType.SEND_SECRET -> {
                    // The rust-sdk will clear this event if it's invalid, this will produce an invalid base64 error
                    // when we try to construct the recovery key.
                    val secretContent = event.getClearContent().toModel<SecretSendEventContent>() ?: return@forEach
                    this.keysBackupService.onSecretKeyGossip(secretContent.secretValue)
                }
                else -> {
                    this.verificationService.onEvent(null, event)
                }
            }
            liveEventManager.get().dispatchOnLiveToDevice(event)
        }
    }

    /**
     * Export the crypto keys.
     *
     * @param password the password
     * @return the exported keys
     */
    override suspend fun exportRoomKeys(password: String): ByteArray {
        val iterationCount = max(10000, MXMegolmExportEncryption.DEFAULT_ITERATION_COUNT)
        return olmMachine.exportKeys(password, iterationCount)
    }

    override fun setRoomBlockUnverifiedDevices(roomId: String, block: Boolean) {
        cryptoStore.blockUnverifiedDevicesInRoom(roomId, block)
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
        val result = olmMachine.importKeys(roomKeysAsArray, password, progressListener).also {
            megolmSessionImportManager.dispatchKeyImportResults(it)
        }
        keysBackupService.maybeBackupKeys()

        return result
    }

    /**
     * Update the warn status when some unknown devices are detected.
     *
     * @param warn true to warn when some unknown devices are detected.
     */
    override fun setWarnOnUnknownDevices(warn: Boolean) {
        // TODO this doesn't seem to be used anymore?
        warnOnUnknownDevicesRepository.setWarnOnUnknownDevices(warn)
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

    override fun getLiveGlobalCryptoConfig(): LiveData<GlobalCryptoConfig> {
        return cryptoStore.getLiveGlobalCryptoConfig()
    }

    // Until https://github.com/matrix-org/matrix-rust-sdk/issues/1364
    override fun supportsDisablingKeyGossiping() = false
    override fun enableKeyGossiping(enable: Boolean) {
        if (!enable) throw UnsupportedOperationException("Not supported by rust")
    }

    override fun isKeyGossipingEnabled(): Boolean {
        return true
    }

    override fun supportsShareKeysOnInvite() = false

    override fun supportsKeyWithheld() = true
    override fun supportsForwardedKeyWiththeld() = false

    override fun enableShareKeyOnInvite(enable: Boolean) {
        if (enable && !supportsShareKeysOnInvite()) {
            throw java.lang.UnsupportedOperationException("Enable share key on invite not implemented in rust")
        }
    }

    override fun isShareKeysOnInviteEnabled() = false

    override fun setRoomUnBlockUnverifiedDevices(roomId: String) {
        cryptoStore.blockUnverifiedDevicesInRoom(roomId, false)
    }

//    override fun getDeviceTrackingStatus(userId: String): Int {
//        olmMachine.isUserTracked(userId)
//    }

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
        return roomId?.let { cryptoStore.getBlockUnverifiedDevices(roomId) }
                ?: false
    }

    override fun getLiveBlockUnverifiedDevices(roomId: String): LiveData<Boolean> {
        return cryptoStore.getLiveBlockUnverifiedDevices(roomId)
    }

    /**
     * Re request the encryption keys required to decrypt an event.
     *
     * @param event the event to decrypt again.
     */
    override suspend fun reRequestRoomKeyForEvent(event: Event) {
        outgoingRequestsProcessor.processRequestRoomKey(olmMachine, event)
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

    override suspend fun downloadKeysIfNeeded(userIds: List<String>, forceDownload: Boolean): MXUsersDevicesMap<CryptoDeviceInfo> {
        return withContext(coroutineDispatchers.crypto) {
            olmMachine.ensureUserDevicesMap(userIds, forceDownload)
        }
    }

    override fun addNewSessionListener(newSessionListener: NewSessionListener) {
        megolmSessionImportManager.addListener(newSessionListener)
    }

    override fun removeSessionListener(listener: NewSessionListener) {
        megolmSessionImportManager.removeListener(listener)
    }
    /* ==========================================================================================
     * DEBUG INFO
     * ========================================================================================== */

    override fun toString(): String {
        return "DefaultCryptoService of $myUserId ($deviceId)"
    }

    // Until https://github.com/matrix-org/matrix-rust-sdk/issues/701
    // https://github.com/matrix-org/matrix-rust-sdk/issues/702
    override fun supportKeyRequestInspection() = false
    override fun getOutgoingRoomKeyRequests(): List<OutgoingKeyRequest> {
        throw UnsupportedOperationException("Not supported by rust")
    }

    override fun getOutgoingRoomKeyRequestsPaged(): LiveData<PagedList<OutgoingKeyRequest>> {
        throw UnsupportedOperationException("Not supported by rust")
//        return cryptoStore.getOutgoingRoomKeyRequestsPaged()
    }

    override fun getIncomingRoomKeyRequestsPaged(): LiveData<PagedList<IncomingRoomKeyRequest>> {
        throw UnsupportedOperationException("Not supported by rust")
    }

    override suspend fun manuallyAcceptRoomKeyRequest(request: IncomingRoomKeyRequest) {
        // TODO rust?
    }

    override fun getIncomingRoomKeyRequests(): List<IncomingRoomKeyRequest> {
        throw UnsupportedOperationException("Not supported by rust")
    }

    override fun getGossipingEventsTrail(): LiveData<PagedList<AuditTrail>> {
        throw UnsupportedOperationException("Not supported by rust")
    }

    override fun getGossipingEvents(): List<AuditTrail> {
        throw UnsupportedOperationException("Not supported by rust")
    }

    override fun getSharedWithInfo(roomId: String?, sessionId: String): MXUsersDevicesMap<Int> {
        throw UnsupportedOperationException("Not supported by rust")
    }

    override fun getWithHeldMegolmSession(roomId: String, sessionId: String): RoomKeyWithHeldContent? {
        throw UnsupportedOperationException("Not supported by rust")
    }

    override fun logDbUsageInfo() {
        // not available with rust
        // cryptoStore.logDbUsageInfo()
    }

    override suspend fun prepareToEncrypt(roomId: String) = prepareToEncrypt.invoke(roomId, ensureAllMembersAreLoaded = true)

    override suspend fun sendSharedHistoryKeys(roomId: String, userId: String, sessionInfoSet: Set<SessionInfo>?) {
        // TODO("Not yet implemented")
    }

    companion object {
        const val CRYPTO_MIN_FORCE_SESSION_PERIOD_MILLIS = 3_600_000 // one hour
    }
}
