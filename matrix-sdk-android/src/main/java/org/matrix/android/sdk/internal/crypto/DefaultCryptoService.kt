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
import dagger.Lazy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.NoOpMatrixCallback
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.crypto.MXCryptoConfig
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.api.listeners.ProgressListener
import org.matrix.android.sdk.api.session.crypto.CryptoService
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.crypto.crosssigning.CrossSigningService
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupService
import org.matrix.android.sdk.api.session.crypto.keyshare.GossipingRequestListener
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibilityContent
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.auth.registration.handleUIA
import org.matrix.android.sdk.internal.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.internal.crypto.keysbackup.RustKeyBackupService
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.BackupKeysResult
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.CreateKeysBackupVersionBody
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.KeysBackupData
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.KeysVersion
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.KeysVersionResult
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.RoomKeysBackupData
import org.matrix.android.sdk.internal.crypto.keysbackup.tasks.CreateKeysBackupVersionTask
import org.matrix.android.sdk.internal.crypto.keysbackup.tasks.DeleteBackupTask
import org.matrix.android.sdk.internal.crypto.keysbackup.tasks.GetKeysBackupLastVersionTask
import org.matrix.android.sdk.internal.crypto.keysbackup.tasks.GetKeysBackupVersionTask
import org.matrix.android.sdk.internal.crypto.keysbackup.tasks.StoreSessionsDataTask
import org.matrix.android.sdk.internal.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.internal.crypto.model.ImportRoomKeysResult
import org.matrix.android.sdk.internal.crypto.model.MXEncryptEventContentResult
import org.matrix.android.sdk.internal.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.internal.crypto.model.event.RoomKeyContent
import org.matrix.android.sdk.internal.crypto.model.event.RoomKeyWithHeldContent
import org.matrix.android.sdk.internal.crypto.model.rest.DeviceInfo
import org.matrix.android.sdk.internal.crypto.model.rest.DevicesListResponse
import org.matrix.android.sdk.internal.crypto.model.rest.ForwardedRoomKeyContent
import org.matrix.android.sdk.internal.crypto.model.rest.KeysClaimResponse
import org.matrix.android.sdk.internal.crypto.model.rest.KeysQueryResponse
import org.matrix.android.sdk.internal.crypto.model.rest.KeysUploadResponse
import org.matrix.android.sdk.internal.crypto.model.rest.RestKeyInfo
import org.matrix.android.sdk.internal.crypto.repository.WarnOnUnknownDeviceRepository
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.crypto.tasks.ClaimOneTimeKeysForUsersDeviceTask
import org.matrix.android.sdk.internal.crypto.tasks.DefaultSendVerificationMessageTask
import org.matrix.android.sdk.internal.crypto.tasks.DeleteDeviceTask
import org.matrix.android.sdk.internal.crypto.tasks.DownloadKeysForUsersTask
import org.matrix.android.sdk.internal.crypto.tasks.GetDeviceInfoTask
import org.matrix.android.sdk.internal.crypto.tasks.GetDevicesTask
import org.matrix.android.sdk.internal.crypto.tasks.SendToDeviceTask
import org.matrix.android.sdk.internal.crypto.tasks.SendVerificationMessageTask
import org.matrix.android.sdk.internal.crypto.tasks.SetDeviceNameTask
import org.matrix.android.sdk.internal.crypto.tasks.UploadKeysTask
import org.matrix.android.sdk.internal.crypto.tasks.UploadSignaturesTask
import org.matrix.android.sdk.internal.crypto.tasks.UploadSigningKeysTask
import org.matrix.android.sdk.internal.crypto.verification.RustVerificationService
import org.matrix.android.sdk.internal.di.DeviceId
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.di.SessionFilesDirectory
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.extensions.foldToCallback
import org.matrix.android.sdk.internal.network.parsing.CheckNumberType
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.session.room.membership.LoadRoomMembersTask
import org.matrix.android.sdk.internal.session.sync.model.DeviceListResponse
import org.matrix.android.sdk.internal.session.sync.model.DeviceOneTimeKeysCountSyncResponse
import org.matrix.android.sdk.internal.session.sync.model.ToDeviceSyncResponse
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.TaskThread
import org.matrix.android.sdk.internal.task.configureWith
import org.matrix.android.sdk.internal.task.launchToCallback
import org.matrix.android.sdk.internal.util.MatrixCoroutineDispatchers
import timber.log.Timber
import uniffi.olm.OutgoingVerificationRequest
import uniffi.olm.Request
import uniffi.olm.RequestType
import uniffi.olm.SignatureUploadRequest
import uniffi.olm.UploadSigningKeysRequest
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.math.max

internal class RequestSender @Inject constructor(
        private val sendToDeviceTask: SendToDeviceTask,
        private val oneTimeKeysForUsersDeviceTask: ClaimOneTimeKeysForUsersDeviceTask,
        private val uploadKeysTask: UploadKeysTask,
        private val downloadKeysForUsersTask: DownloadKeysForUsersTask,
        private val signaturesUploadTask: UploadSignaturesTask,
        private val sendVerificationMessageTask: Lazy<DefaultSendVerificationMessageTask>,
        private val uploadSigningKeysTask: UploadSigningKeysTask,
        private val getKeysBackupLastVersionTask: GetKeysBackupLastVersionTask,
        private val getKeysBackupVersionTask: GetKeysBackupVersionTask,
        private val deleteBackupTask: DeleteBackupTask,
        private val createKeysBackupVersionTask: CreateKeysBackupVersionTask,
        private val backupRoomKeysTask: StoreSessionsDataTask,
        ) {
    companion object {
        const val REQUEST_RETRY_COUNT = 3
    }

    suspend fun claimKeys(request: Request.KeysClaim): String {
        val claimParams = ClaimOneTimeKeysForUsersDeviceTask.Params(request.oneTimeKeys)
        val response = oneTimeKeysForUsersDeviceTask.executeRetry(claimParams, REQUEST_RETRY_COUNT)
        val adapter = MoshiProvider
                .providesMoshi()
                .adapter(KeysClaimResponse::class.java)
        return adapter.toJson(response)!!
    }

    suspend fun queryKeys(request: Request.KeysQuery): String {
        val params = DownloadKeysForUsersTask.Params(request.users, null)
        val response = downloadKeysForUsersTask.executeRetry(params, REQUEST_RETRY_COUNT)
        val adapter = MoshiProvider.providesMoshi().adapter(KeysQueryResponse::class.java)
        return adapter.toJson(response)!!
    }

    suspend fun uploadKeys(request: Request.KeysUpload): String {
        val body = MoshiProvider.providesMoshi().adapter<JsonDict>(Map::class.java).fromJson(request.body)!!
        val params = UploadKeysTask.Params(body)

        val response = uploadKeysTask.executeRetry(params, REQUEST_RETRY_COUNT)
        val adapter = MoshiProvider.providesMoshi().adapter(KeysUploadResponse::class.java)

        return adapter.toJson(response)!!
    }

    suspend fun sendVerificationRequest(request: OutgoingVerificationRequest) {
        when (request) {
            is OutgoingVerificationRequest.InRoom   -> sendRoomMessage(request)
            is OutgoingVerificationRequest.ToDevice -> sendToDevice(request)
        }
    }

    suspend fun sendRoomMessage(request: OutgoingVerificationRequest.InRoom): String {
        return sendRoomMessage(request.eventType, request.roomId, request.content, request.requestId)
    }

    suspend fun sendRoomMessage(request: Request.RoomMessage): String {
        return sendRoomMessage(request.eventType, request.roomId, request.content, request.requestId)
    }

    suspend fun sendRoomMessage(eventType: String, roomId: String, content: String, transactionId: String): String {
        val adapter = MoshiProvider.providesMoshi().adapter<Content>(Map::class.java)
        val jsonContent = adapter.fromJson(content)
        val event = Event(eventType, transactionId, jsonContent, roomId = roomId)
        val params = SendVerificationMessageTask.Params(event)
        return this.sendVerificationMessageTask.get().executeRetry(params, REQUEST_RETRY_COUNT)
    }

    suspend fun sendSignatureUpload(request: Request.SignatureUpload) {
        sendSignatureUpload(request.body)
    }

    suspend fun sendSignatureUpload(request: SignatureUploadRequest) {
        sendSignatureUpload(request.body)
    }

    private suspend fun sendSignatureUpload(body: String) {
        val adapter = MoshiProvider.providesMoshi().adapter<Map<String, Map<String, Any>>>(Map::class.java)
        val signatures = adapter.fromJson(body)!!
        val params = UploadSignaturesTask.Params(signatures)
        this.signaturesUploadTask.executeRetry(params, REQUEST_RETRY_COUNT)
    }

    suspend fun uploadCrossSigningKeys(
            request: UploadSigningKeysRequest,
            interactiveAuthInterceptor: UserInteractiveAuthInterceptor?
    ) {
        val adapter = MoshiProvider.providesMoshi().adapter(RestKeyInfo::class.java)
        val masterKey = adapter.fromJson(request.masterKey)!!.toCryptoModel()
        val selfSigningKey = adapter.fromJson(request.selfSigningKey)!!.toCryptoModel()
        val userSigningKey = adapter.fromJson(request.userSigningKey)!!.toCryptoModel()

        val uploadSigningKeysParams = UploadSigningKeysTask.Params(
                masterKey,
                userSigningKey,
                selfSigningKey,
                null
        )

        try {
            uploadSigningKeysTask.execute(uploadSigningKeysParams)
        } catch (failure: Throwable) {
            if (interactiveAuthInterceptor == null
                    || !handleUIA(
                            failure = failure,
                            interceptor = interactiveAuthInterceptor,
                            retryBlock = { authUpdate ->
                                uploadSigningKeysTask.executeRetry(
                                        uploadSigningKeysParams.copy(userAuthParam = authUpdate),
                                        REQUEST_RETRY_COUNT
                                )
                            }
                    )
            ) {
                Timber.d("## UIA: propagate failure")
                throw failure
            }
        }
    }

    suspend fun sendToDevice(request: Request.ToDevice) {
        sendToDevice(request.eventType, request.body, request.requestId)
    }

    suspend fun sendToDevice(request: OutgoingVerificationRequest.ToDevice) {
        sendToDevice(request.eventType, request.body, request.requestId)
    }

    suspend fun sendToDevice(eventType: String, body: String, transactionId: String) {
        val adapter = MoshiProvider
                .providesMoshi()
                .newBuilder()
                .add(CheckNumberType.JSON_ADAPTER_FACTORY)
                .build()
                .adapter<Map<String, HashMap<String, Any>>>(Map::class.java)
        val jsonBody = adapter.fromJson(body)!!

        val userMap = MXUsersDevicesMap<Any>()
        userMap.join(jsonBody)

        val sendToDeviceParams = SendToDeviceTask.Params(eventType, userMap, transactionId)
        sendToDeviceTask.executeRetry(sendToDeviceParams, REQUEST_RETRY_COUNT)
    }

    suspend fun getKeyBackupVersion(version: String? = null): KeysVersionResult? {
        return try {
            if (version != null) {
                getKeysBackupVersionTask.execute(version)
            } else {
                getKeysBackupLastVersionTask.execute(Unit)
            }
        } catch (failure: Throwable) {
            if (failure is Failure.ServerError
                    && failure.error.code == MatrixError.M_NOT_FOUND) {
                null
            } else {
                throw failure
            }
        }
    }

    suspend fun createKeyBackup(body: CreateKeysBackupVersionBody): KeysVersion {
        return createKeysBackupVersionTask.execute(body)
    }

    suspend fun deleteKeyBackup(version: String) {
        val params = DeleteBackupTask.Params(version)
        deleteBackupTask.execute(params)
    }

    suspend fun backupRoomKeys(request: Request.KeysBackup): String {
        val adapter = MoshiProvider
                .providesMoshi()
                .newBuilder()
                .build()
                .adapter<MutableMap<String, RoomKeysBackupData>>(MutableMap::class.java)
        val keys = adapter.fromJson(request.rooms)!!
        Timber.d("BACKUP: CONVERTED KEYS TO HASHMAP $keys")
        /*
        val keyAdapter = MoshiProvider.providesMoshi().adapter(KeyBackupData::class.java)
        val keysBackupData = KeysBackupData()
        for (room in keys) {
            val sessions = room.value.getOrDefault("sessions", mapOf())

            for (session in sessions) {
                Timber.d("BACKUP: HEEELOO CONVERTING KEY ${session.value}")
                val key = keyAdapter.fromJson(session.value)!!
                Timber.d("BACKUP: HEEELOO CONVERTED KEY $key")

                keysBackupData
                        .roomIdToRoomKeysBackupData
                        .getOrPut(room.key, { RoomKeysBackupData() })
                        .sessionIdToKeyBackupData[session.key] = key
            }
        }


         */
        /*
        for ((roomId, backupData) in keys) {
            val roomData = backup.roomIdToRoomKeysBackupData.getOrPut(roomId, { RoomKeysBackupData() })
            for ((sessionId, key) in backupData.sessionIdToKeyBackupData) {
                Timber.d("BACKUP INSERTING KEY $key")
                roomData.sessionIdToKeyBackupData[sessionId] = key
            }
        }
         */
        val params = StoreSessionsDataTask.Params(request.version, KeysBackupData())
        val response = backupRoomKeysTask.execute(params)
        val responseAdapter = MoshiProvider.providesMoshi().adapter(BackupKeysResult::class.java)
        return responseAdapter.toJson(response)!!
    }
}

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
        @UserId
        private val userId: String,
        @DeviceId
        private val deviceId: String?,
        @SessionFilesDirectory
        private val dataDir: File,
        // the crypto store
        private val cryptoStore: IMXCryptoStore,
        // Set of parameters used to configure/customize the end-to-end crypto.
        private val mxCryptoConfig: MXCryptoConfig,
        // Actions
        private val warnOnUnknownDevicesRepository: WarnOnUnknownDeviceRepository,
        // Tasks
        private val deleteDeviceTask: DeleteDeviceTask,
        private val getDevicesTask: GetDevicesTask,
        private val getDeviceInfoTask: GetDeviceInfoTask,
        private val setDeviceNameTask: SetDeviceNameTask,
        private val loadRoomMembersTask: LoadRoomMembersTask,
        private val cryptoSessionInfoProvider: CryptoSessionInfoProvider,
        private val createKeysBackupVersionTask: CreateKeysBackupVersionTask,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val taskExecutor: TaskExecutor,
        private val cryptoCoroutineScope: CoroutineScope,
        private val sender: RequestSender,
) : CryptoService {

    private val isStarting = AtomicBoolean(false)
    private val isStarted = AtomicBoolean(false)

    private var olmMachine: OlmMachine? = null

    // The verification service.
    private var verificationService: RustVerificationService? = null

    // The cross signing service.
    private var crossSigningService: RustCrossSigningService? = null

    // The key backup service.
    private var keysBackupService: RustKeyBackupService? = null

    private val deviceObserver: DeviceUpdateObserver = DeviceUpdateObserver()

    // Locks for some of our operations
    private val keyClaimLock: Mutex = Mutex()
    private val outgoingRequestsLock: Mutex = Mutex()
    private val roomKeyShareLocks: ConcurrentHashMap<String, Mutex> = ConcurrentHashMap()

    // TODO does this need to be concurrent?
    private val newSessionListeners = ArrayList<NewSessionListener>()

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
            else                                    -> this.verificationService?.onEvent(event)
        }
    }

    private val gossipingBuffer = mutableListOf<Event>()

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
        // TODO we should provide olm and rust-sdk version from the rust-sdk
        return if (longFormat) "Rust SDK 0.3" else "0.3"
    }

    override fun getMyDevice(): CryptoDeviceInfo {
        return runBlocking { olmMachine!!.ownDevice() }
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
     * Start the crypto module.
     * Device keys will be uploaded, then one time keys if there are not enough on the homeserver
     * and, then, if this is the first time, this new device will be announced to all other users
     * devices.
     *
     */
    fun start() {
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
            /*
            TODO
            tryOrNull {
                keysBackupService.checkAndStartKeysBackup()
            }
            */
        }
    }

    private fun internalStart() {
        if (isStarted.get() || isStarting.get()) {
            return
        }
        isStarting.set(true)

        try {
            setRustLogger()
            val machine = OlmMachine(userId, deviceId!!, dataDir, deviceObserver, sender)
            olmMachine = machine
            verificationService = RustVerificationService(machine)
            crossSigningService = RustCrossSigningService(machine)
            keysBackupService = RustKeyBackupService(machine, sender, coroutineDispatchers, cryptoCoroutineScope)
            Timber.v(
                    "## CRYPTO | Successfully started up an Olm machine for " +
                            "${userId}, ${deviceId}, identity keys: ${this.olmMachine?.identityKeys()}")
        } catch (throwable: Throwable) {
            Timber.v("Failed create an Olm machine: $throwable")
        }

        tryOrNull {
            keysBackupService!!.checkAndStartKeysBackup()
        }

        // Open the store
        cryptoStore.open()

        isStarting.set(false)
        isStarted.set(true)
    }

    /**
     * Close the crypto
     */
    fun close() = runBlocking(coroutineDispatchers.crypto) {
        cryptoCoroutineScope.coroutineContext.cancelChildren(CancellationException("Closing crypto module"))
        cryptoStore.close()
    }

    // Always enabled on Matrix Android SDK2
    override fun isCryptoEnabled() = true

    /**
     * @return the Keys backup Service
     */
    override fun keysBackupService(): KeysBackupService {
        if (keysBackupService == null) {
            internalStart()
        }
        return keysBackupService!!
    }

    /**
     * @return the VerificationService
     */
    override fun verificationService(): VerificationService {
        // TODO yet another problem because the CryptoService is started in the
        // sync loop
        //
        // The `KeyRequestHandler` and `IncomingVerificationHandler` want to add
        // listeners to the verification service, they are initialized in the
        // `ActiveSessionHolder` class in the `setActiveSession()` method. In
        // the `setActiveSession()` method we call the `start()` method of the
        // handlers without first calling the `start()` method of the
        // `DefaultCryptoService`.
        //
        // The start method of the crypto service isn't part of the
        // `CryptoService` interface so it currently can't be called there. I'm
        // inclined to believe that it should be, and that it should be
        // initialized before anything else tries to do something with it.
        //
        // Let's initialize here as a workaround until we figure out if the
        // above conclusion is correct.
        if (verificationService == null) {
            internalStart()
        }

        return verificationService!!
    }

    override fun crossSigningService(): CrossSigningService {
        if (crossSigningService == null) {
            internalStart()
        }

        return crossSigningService!!
    }

    /**
     * A sync response has been received
     */
    suspend fun onSyncCompleted() {
        if (isStarted()) {
            sendOutgoingRequests()
            // This isn't a copy paste error. Sending the outgoing requests may
            // claim one-time keys and establish 1-to-1 Olm sessions with devices, while some
            // outgoing requests are waiting for an Olm session to be established (e.g. forwarding
            // room keys or sharing secrets).

            // The second call sends out those requests that are waiting for the
            // keys claim request to be sent out.
            // This could be omitted but then devices might be waiting for the next
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
     * Provides the device information for a user id and a device Id
     *
     * @param userId   the user id
     * @param deviceId the device id
     */
    override fun getDeviceInfo(userId: String, deviceId: String?): CryptoDeviceInfo? {
        return if (userId.isNotEmpty() && !deviceId.isNullOrEmpty()) {
            runBlocking {
                this@DefaultCryptoService.olmMachine?.getCryptoDeviceInfo(userId, deviceId)
            }
        } else {
            null
        }
    }

    override fun getCryptoDeviceInfo(userId: String): List<CryptoDeviceInfo> {
        return runBlocking {
            this@DefaultCryptoService.olmMachine?.getCryptoDeviceInfo(userId) ?: listOf()
        }
    }

    override fun getLiveCryptoDeviceInfo(userId: String): LiveData<List<CryptoDeviceInfo>> {
        return getLiveCryptoDeviceInfo(listOf(userId))
    }

    override fun getLiveCryptoDeviceInfo(userIds: List<String>): LiveData<List<CryptoDeviceInfo>> {
        return runBlocking {
            this@DefaultCryptoService.olmMachine?.getLiveDevices(userIds) ?: LiveDevice(userIds, deviceObserver)
        }
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
            Timber.e("## CRYPTO | setEncryptionInRoom() : Ignoring m.room.encryption event which requests a change of config in $roomId")
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
        return this.getCryptoDeviceInfo(userId).toMutableList()
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
        // moved to crypto scope to have up to date values
        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
            val algorithm = getEncryptionAlgorithm(roomId)

            if (algorithm != null) {
                val userIds = getRoomUserIds(roomId)
                val t0 = System.currentTimeMillis()
                Timber.v("## CRYPTO | encryptEventContent() starts")
                runCatching {
                    preshareRoomKey(roomId, userIds)
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
        olmMachine?.discardRoomKey(roomId)
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
        return runBlocking {
            olmMachine!!.decryptRoomEvent(event)
        }
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

    private fun notifyRoomKeyReceived(
            roomId: String,
            sessionId: String,
    ) {
        // The sender key is actually unused since it's unimportant for megolm
        // Our events don't contain the info so pass an empty string until we
        // change the listener definition
        val senderKey = ""

        newSessionListeners.forEach {
            try {
                it.onNewSession(roomId, senderKey, sessionId)
            } catch (e: Throwable) {
            }
        }
    }

    suspend fun receiveSyncChanges(
            toDevice: ToDeviceSyncResponse?,
            deviceChanges: DeviceListResponse?,
            keyCounts: DeviceOneTimeKeysCountSyncResponse?) {
        // Decrypt and handle our to-device events
        val toDeviceEvents = this.olmMachine!!.receiveSyncChanges(toDevice, deviceChanges, keyCounts)

        // Notify the our listeners about room keys so decryption is retried.
        if (toDeviceEvents.events != null) {
            toDeviceEvents.events.forEach { event ->
                when (event.type) {
                    EventType.ROOM_KEY           -> {
                        val content = event.getClearContent().toModel<RoomKeyContent>() ?: return@forEach

                        val roomId = content.sessionId ?: return@forEach
                        val sessionId = content.sessionId

                        notifyRoomKeyReceived(roomId, sessionId)
                    }
                    EventType.FORWARDED_ROOM_KEY -> {
                        val content = event.getClearContent().toModel<ForwardedRoomKeyContent>() ?: return@forEach

                        val roomId = content.sessionId ?: return@forEach
                        val sessionId = content.sessionId

                        notifyRoomKeyReceived(roomId, sessionId)
                    }
                    else                         -> {
                        this.verificationService?.onEvent(event)
                    }
                }
            }
        }
    }

    private suspend fun preshareRoomKey(roomId: String, roomMembers: List<String>) {
        keyClaimLock.withLock {
            val request = this.olmMachine!!.getMissingSessions(roomMembers)
            // This request can only be a keys claim request.
            if (request != null) {
                when (request) {
                    is Request.KeysClaim -> {
                        claimKeys(request)
                    }
                    else                 -> {
                    }
                }
            }
        }

        val keyShareLock = roomKeyShareLocks.getOrPut(roomId, { Mutex() })

        keyShareLock.withLock {
            coroutineScope {
                this@DefaultCryptoService.olmMachine!!.shareRoomKey(roomId, roomMembers).map {
                    when (it) {
                        is Request.ToDevice -> {
                            async {
                                sendToDevice(it)
                            }
                        }
                        else                -> {
                            // This request can only be a to-device request but
                            // we need to handle all our cases and put this
                            // async block for our joinAll to work.
                            async {}
                        }
                    }
                }.joinAll()
            }
        }
    }

    private suspend fun encrypt(roomId: String, eventType: String, content: Content): Content {
        return olmMachine!!.encrypt(roomId, eventType, content)
    }

    private suspend fun uploadKeys(request: Request.KeysUpload) {
        val response = this.sender.uploadKeys(request)
        this.olmMachine!!.markRequestAsSent(request.requestId, RequestType.KEYS_UPLOAD, response)
    }

    private suspend fun queryKeys(request: Request.KeysQuery) {
        try {
            val response = this.sender.queryKeys(request)
            this.olmMachine!!.markRequestAsSent(request.requestId, RequestType.KEYS_QUERY, response)
        } catch (throwable: Throwable) {
            Timber.e(throwable, "## CRYPTO | doKeyDownloadForUsers(): error")
        }
    }

    private suspend fun sendToDevice(request: Request.ToDevice) {
        this.sender.sendToDevice(request)
        olmMachine!!.markRequestAsSent(request.requestId, RequestType.TO_DEVICE, "{}")
    }

    private suspend fun claimKeys(request: Request.KeysClaim) {
        val response = this.sender.claimKeys(request)
        olmMachine!!.markRequestAsSent(request.requestId, RequestType.KEYS_CLAIM, response)
    }

    private suspend fun signatureUpload(request: Request.SignatureUpload) {
        this.sender.sendSignatureUpload(request)
        olmMachine!!.markRequestAsSent(request.requestId, RequestType.SIGNATURE_UPLOAD, "{}")
    }

    private suspend fun sendOutgoingRequests() {
        outgoingRequestsLock.withLock {
            coroutineScope {
                olmMachine!!.outgoingRequests().map {
                    when (it) {
                        is Request.KeysUpload      -> {
                            async {
                                uploadKeys(it)
                            }
                        }
                        is Request.KeysQuery       -> {
                            async {
                                queryKeys(it)
                            }
                        }
                        is Request.ToDevice        -> {
                            async {
                                sendToDevice(it)
                            }
                        }
                        is Request.KeysClaim       -> {
                            async {
                                claimKeys(it)
                            }
                        }
                        is Request.RoomMessage     -> {
                            async {
                                sender.sendRoomMessage(it)
                            }
                        }
                        is Request.SignatureUpload -> {
                            async {
                                signatureUpload(it)
                            }
                        }
                        is Request.KeysBackup      -> {
                            async {
                                TODO()
                            }
                        }
                    }
                }.joinAll()
            }
        }
    }

    /**
     * Export the crypto keys
     *
     * @param password the password
     * @return the exported keys
     */
    override suspend fun exportRoomKeys(password: String): ByteArray {
        val iterationCount = max(10000, MXMegolmExportEncryption.DEFAULT_ITERATION_COUNT)
        return olmMachine!!.exportKeys(password, iterationCount)
    }

    /**
     * Import the room keys
     *
     * @param roomKeysAsArray  the room keys as array.
     * @param password         the password
     * @param progressListener the progress listener
     * @return the result ImportRoomKeysResult
     */
    override suspend fun importRoomKeys(roomKeysAsArray: ByteArray,
                                        password: String,
                                        progressListener: ProgressListener?): ImportRoomKeysResult {
        return olmMachine!!.importKeys(roomKeysAsArray, password, progressListener)
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
        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
            val requestPair = olmMachine!!.requestRoomKey(event)

            val cancellation = requestPair.cancellation
            val request = requestPair.keyRequest

            if (cancellation != null) {
                when (cancellation) {
                    is Request.ToDevice -> {
                        sendToDevice(cancellation)
                    }
                }
            }

            when (request) {
                is Request.ToDevice -> {
                    sendToDevice(request)
                }
            }
        }
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
                if (forceDownload) {
                    // TODO replicate the logic from the device list manager
                    // where we would download the fresh info from the server.
                    this@DefaultCryptoService.olmMachine?.getUserDevicesMap(userIds) ?: MXUsersDevicesMap()
                } else {
                    this@DefaultCryptoService.olmMachine?.getUserDevicesMap(userIds) ?: MXUsersDevicesMap()
                }
            }.foldToCallback(callback)
        }
    }

    override fun addNewSessionListener(newSessionListener: NewSessionListener) {
        if (!newSessionListeners.contains(newSessionListener)) newSessionListeners.add(newSessionListener)
    }

    override fun removeSessionListener(listener: NewSessionListener) {
        newSessionListeners.remove(listener)
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

    override fun prepareToEncrypt(roomId: String, callback: MatrixCallback<Unit>) {
        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
            Timber.d("## CRYPTO | prepareToEncrypt() : Check room members up to date")
            // Ensure to load all room members
            try {
                loadRoomMembersTask.execute(LoadRoomMembersTask.Params(roomId))
            } catch (failure: Throwable) {
                Timber.e("## CRYPTO | prepareToEncrypt() : Failed to load room members")
                callback.onFailure(failure)
                return@launch
            }

            val userIds = getRoomUserIds(roomId)

            val algorithm = getEncryptionAlgorithm(roomId)

            if (algorithm == null) {
                val reason = String.format(MXCryptoError.UNABLE_TO_ENCRYPT_REASON, MXCryptoError.NO_MORE_ALGORITHM_REASON)
                Timber.e("## CRYPTO | prepareToEncrypt() : $reason")
                callback.onFailure(IllegalArgumentException("Missing algorithm"))
                return@launch
            }

            runCatching {
                preshareRoomKey(roomId, userIds)
            }.fold(
                    { callback.onSuccess(Unit) },
                    {
                        Timber.e("## CRYPTO | prepareToEncrypt() failed.")
                        callback.onFailure(it)
                    }
            )
        }
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
