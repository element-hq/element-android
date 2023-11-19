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

package org.matrix.android.sdk.api.session.crypto

import android.content.Context
import androidx.annotation.Size
import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.listeners.ProgressListener
import org.matrix.android.sdk.api.session.crypto.crosssigning.CrossSigningService
import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupService
import org.matrix.android.sdk.api.session.crypto.keyshare.GossipingRequestListener
import org.matrix.android.sdk.api.session.crypto.model.AuditTrail
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.ImportRoomKeysResult
import org.matrix.android.sdk.api.session.crypto.model.IncomingRoomKeyRequest
import org.matrix.android.sdk.api.session.crypto.model.MXEncryptEventContentResult
import org.matrix.android.sdk.api.session.crypto.model.MXEventDecryptionResult
import org.matrix.android.sdk.api.session.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.content.RoomKeyWithHeldContent
import org.matrix.android.sdk.api.session.sync.model.DeviceListResponse
import org.matrix.android.sdk.api.session.sync.model.DeviceOneTimeKeysCountSyncResponse
import org.matrix.android.sdk.api.session.sync.model.SyncResponse
import org.matrix.android.sdk.api.session.sync.model.ToDeviceSyncResponse
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.crypto.model.SessionInfo
import org.matrix.android.sdk.internal.crypto.store.db.CryptoStoreAggregator

interface CryptoService {

    fun name(): String
    fun verificationService(): VerificationService

    fun crossSigningService(): CrossSigningService

    fun keysBackupService(): KeysBackupService

    suspend fun setDeviceName(deviceId: String, deviceName: String)

    suspend fun deleteDevice(deviceId: String, userInteractiveAuthInterceptor: UserInteractiveAuthInterceptor)

    suspend fun deleteDevices(@Size(min = 1) deviceIds: List<String>, userInteractiveAuthInterceptor: UserInteractiveAuthInterceptor)

    fun getCryptoVersion(context: Context, longFormat: Boolean): String

    fun isCryptoEnabled(): Boolean

    fun isRoomBlacklistUnverifiedDevices(roomId: String?): Boolean

    fun getLiveBlockUnverifiedDevices(roomId: String): LiveData<Boolean>

    fun setWarnOnUnknownDevices(warn: Boolean)

    suspend fun getUserDevices(userId: String): List<CryptoDeviceInfo>

    suspend fun getMyCryptoDevice(): CryptoDeviceInfo

    fun getGlobalBlacklistUnverifiedDevices(): Boolean

    fun setGlobalBlacklistUnverifiedDevices(block: Boolean)

    fun getLiveGlobalCryptoConfig(): LiveData<GlobalCryptoConfig>

    fun supportsDisablingKeyGossiping(): Boolean

    /**
     * Enable or disable key gossiping.
     * Default is true.
     * If set to false this device won't send key_request nor will accept key forwarded
     */
    fun enableKeyGossiping(enable: Boolean)

    fun isKeyGossipingEnabled(): Boolean

    /*
     * Tells if the current crypto implementation supports MSC3061
     */
    fun supportsShareKeysOnInvite(): Boolean

    fun supportsKeyWithheld(): Boolean
    fun supportsForwardedKeyWiththeld(): Boolean

    /**
     * As per MSC3061.
     * If true will make it possible to share part of e2ee room history
     * on invite depending on the room visibility setting.
     */
    fun enableShareKeyOnInvite(enable: Boolean)

    /**
     * As per MSC3061.
     * If true will make it possible to share part of e2ee room history
     * on invite depending on the room visibility setting.
     */
    fun isShareKeysOnInviteEnabled(): Boolean

    fun setRoomUnBlockUnverifiedDevices(roomId: String)

//    fun getDeviceTrackingStatus(userId: String): Int

    suspend fun importRoomKeys(
            roomKeysAsArray: ByteArray,
            password: String,
            progressListener: ProgressListener?
    ): ImportRoomKeysResult

    suspend fun exportRoomKeys(password: String): ByteArray

    fun setRoomBlockUnverifiedDevices(roomId: String, block: Boolean)

    suspend fun getCryptoDeviceInfo(userId: String, deviceId: String?): CryptoDeviceInfo?

    suspend fun getCryptoDeviceInfo(userId: String): List<CryptoDeviceInfo>

//    fun getCryptoDeviceInfoFlow(userId: String): Flow<List<CryptoDeviceInfo>>

    fun getLiveCryptoDeviceInfo(): LiveData<List<CryptoDeviceInfo>>

    fun getLiveCryptoDeviceInfoWithId(deviceId: String): LiveData<Optional<CryptoDeviceInfo>>

    fun getLiveCryptoDeviceInfo(userId: String): LiveData<List<CryptoDeviceInfo>>

    fun getLiveCryptoDeviceInfo(userIds: List<String>): LiveData<List<CryptoDeviceInfo>>

    suspend fun reRequestRoomKeyForEvent(event: Event)

    fun addRoomKeysRequestListener(listener: GossipingRequestListener)

    fun removeRoomKeysRequestListener(listener: GossipingRequestListener)

    suspend fun fetchDevicesList(): List<DeviceInfo>

    fun getMyDevicesInfo(): List<DeviceInfo>

    fun getMyDevicesInfoLive(): LiveData<List<DeviceInfo>>

    fun getMyDevicesInfoLive(deviceId: String): LiveData<Optional<DeviceInfo>>

    suspend fun fetchDeviceInfo(deviceId: String): DeviceInfo

    suspend fun inboundGroupSessionsCount(onlyBackedUp: Boolean): Int

    fun isRoomEncrypted(roomId: String): Boolean

    // TODO This could be removed from this interface
    suspend fun encryptEventContent(
            eventContent: Content,
            eventType: String,
            roomId: String
    ): MXEncryptEventContentResult

    fun discardOutboundSession(roomId: String)

    @Throws(MXCryptoError::class)
    suspend fun decryptEvent(event: Event, timeline: String): MXEventDecryptionResult

    fun getEncryptionAlgorithm(roomId: String): String?

    fun shouldEncryptForInvitedMembers(roomId: String): Boolean

    suspend fun downloadKeysIfNeeded(userIds: List<String>, forceDownload: Boolean = false): MXUsersDevicesMap<CryptoDeviceInfo>

    suspend fun getCryptoDeviceInfoList(userId: String): List<CryptoDeviceInfo>

//    fun getLiveCryptoDeviceInfoList(userId: String): Flow<List<CryptoDeviceInfo>>
//
//    fun getLiveCryptoDeviceInfoList(userIds: List<String>): Flow<List<CryptoDeviceInfo>>

    fun addNewSessionListener(newSessionListener: NewSessionListener)
    fun removeSessionListener(listener: NewSessionListener)

    fun supportKeyRequestInspection(): Boolean

    fun getOutgoingRoomKeyRequests(): List<OutgoingKeyRequest>
    fun getOutgoingRoomKeyRequestsPaged(): LiveData<PagedList<OutgoingKeyRequest>>

    fun getIncomingRoomKeyRequests(): List<IncomingRoomKeyRequest>
    fun getIncomingRoomKeyRequestsPaged(): LiveData<PagedList<IncomingRoomKeyRequest>>

    /**
     * Can be called by the app layer to accept a request manually.
     * Use carefully as it is prone to social attacks.
     */
    suspend fun manuallyAcceptRoomKeyRequest(request: IncomingRoomKeyRequest)

    fun getGossipingEventsTrail(): LiveData<PagedList<AuditTrail>>
    fun getGossipingEvents(): List<AuditTrail>

    // For testing shared session
    fun getSharedWithInfo(roomId: String?, sessionId: String): MXUsersDevicesMap<Int>
    fun getWithHeldMegolmSession(roomId: String, sessionId: String): RoomKeyWithHeldContent?

    /**
     * Perform any background tasks that can be done before a message is ready to
     * send, in order to speed up sending of the message.
     */
    suspend fun prepareToEncrypt(roomId: String)

    /**
     * Share all inbound sessions of the last chunk messages to the provided userId devices.
     */
    suspend fun sendSharedHistoryKeys(roomId: String, userId: String, sessionInfoSet: Set<SessionInfo>?)

    /**
     * When LL all room members might not be loaded when setting up encryption.
     * This is called after room members have been loaded
     * ... not sure if shoud be API
     */
    fun onE2ERoomMemberLoadedFromServer(roomId: String)

    suspend fun deviceWithIdentityKey(userId: String, senderKey: String, algorithm: String): CryptoDeviceInfo?
    suspend fun setDeviceVerification(trustLevel: DeviceTrustLevel, userId: String, deviceId: String)

    fun close()
    fun start()
    suspend fun onSyncWillProcess(isInitialSync: Boolean)
    fun isStarted(): Boolean

    suspend fun receiveSyncChanges(
            toDevice: ToDeviceSyncResponse?,
            deviceChanges: DeviceListResponse?,
            keyCounts: DeviceOneTimeKeysCountSyncResponse?,
            deviceUnusedFallbackKeyTypes: List<String>?,
            nextBatch: String?)

    suspend fun onLiveEvent(roomId: String, event: Event, isInitialSync: Boolean, cryptoStoreAggregator: CryptoStoreAggregator?)
    suspend fun onStateEvent(roomId: String, event: Event, cryptoStoreAggregator: CryptoStoreAggregator?) {}
    suspend fun onSyncCompleted(syncResponse: SyncResponse, cryptoStoreAggregator: CryptoStoreAggregator)
    fun logDbUsageInfo()
}
