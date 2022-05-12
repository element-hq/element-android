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
import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import kotlinx.coroutines.flow.Flow
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.listeners.ProgressListener
import org.matrix.android.sdk.api.session.crypto.crosssigning.CrossSigningService
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
import org.matrix.android.sdk.internal.crypto.NewSessionListener

interface CryptoService {

    fun verificationService(): VerificationService

    fun crossSigningService(): CrossSigningService

    fun keysBackupService(): KeysBackupService

    suspend fun setDeviceName(deviceId: String, deviceName: String)

    suspend fun deleteDevice(deviceId: String, userInteractiveAuthInterceptor: UserInteractiveAuthInterceptor)

    fun getCryptoVersion(context: Context, longFormat: Boolean): String

    fun isCryptoEnabled(): Boolean

    fun isRoomBlacklistUnverifiedDevices(roomId: String?): Boolean

    fun setWarnOnUnknownDevices(warn: Boolean)

    suspend fun getUserDevices(userId: String): MutableList<CryptoDeviceInfo>

    suspend fun getMyCryptoDevice(): CryptoDeviceInfo

    fun getGlobalBlacklistUnverifiedDevices(): Boolean

    fun setGlobalBlacklistUnverifiedDevices(block: Boolean)

    /**
     * Enable or disable key gossiping.
     * Default is true.
     * If set to false this device won't send key_request nor will accept key forwarded
     */
    fun enableKeyGossiping(enable: Boolean)

    fun isKeyGossipingEnabled(): Boolean

    fun setRoomUnBlacklistUnverifiedDevices(roomId: String)

    suspend fun importRoomKeys(roomKeysAsArray: ByteArray,
                               password: String,
                               progressListener: ProgressListener?): ImportRoomKeysResult

    suspend fun exportRoomKeys(password: String): ByteArray

    fun setRoomBlacklistUnverifiedDevices(roomId: String)

    suspend fun getCryptoDeviceInfo(userId: String, deviceId: String?): CryptoDeviceInfo?

    fun reRequestRoomKeyForEvent(event: Event)

    fun addRoomKeysRequestListener(listener: GossipingRequestListener)

    fun removeRoomKeysRequestListener(listener: GossipingRequestListener)

    suspend fun fetchDevicesList(): List<DeviceInfo>

    fun getMyDevicesInfo(): List<DeviceInfo>

    fun getLiveMyDevicesInfo(): LiveData<List<DeviceInfo>>

    suspend fun fetchDeviceInfo(deviceId: String): DeviceInfo

    suspend fun inboundGroupSessionsCount(onlyBackedUp: Boolean): Int

    fun isRoomEncrypted(roomId: String): Boolean

    // TODO This could be removed from this interface
    suspend fun encryptEventContent(eventContent: Content,
                            eventType: String,
                            roomId: String): MXEncryptEventContentResult

    fun discardOutboundSession(roomId: String)

    @Throws(MXCryptoError::class)
    suspend fun decryptEvent(event: Event, timeline: String): MXEventDecryptionResult

    fun getEncryptionAlgorithm(roomId: String): String?

    fun shouldEncryptForInvitedMembers(roomId: String): Boolean

    suspend fun downloadKeys(userIds: List<String>, forceDownload: Boolean = false): MXUsersDevicesMap<CryptoDeviceInfo>

    suspend fun getCryptoDeviceInfoList(userId: String): List<CryptoDeviceInfo>

    fun getLiveCryptoDeviceInfoList(userId: String): Flow<List<CryptoDeviceInfo>>

    fun getLiveCryptoDeviceInfoList(userIds: List<String>): Flow<List<CryptoDeviceInfo>>

    fun addNewSessionListener(newSessionListener: NewSessionListener)
    fun removeSessionListener(listener: NewSessionListener)

    fun getOutgoingRoomKeyRequests(): List<OutgoingKeyRequest>
    fun getOutgoingRoomKeyRequestsPaged(): LiveData<PagedList<OutgoingKeyRequest>>

    fun getIncomingRoomKeyRequests(): List<IncomingRoomKeyRequest>
    fun getIncomingRoomKeyRequestsPaged(): LiveData<PagedList<IncomingRoomKeyRequest>>

    /**
     * Can be called by the app layer to accept a request manually
     * Use carefully as it is prone to social attacks
     */
    suspend fun manuallyAcceptRoomKeyRequest(request: IncomingRoomKeyRequest)

    fun getGossipingEventsTrail(): LiveData<PagedList<AuditTrail>>
    fun getGossipingEvents(): List<AuditTrail>

    // For testing shared session
    fun getSharedWithInfo(roomId: String?, sessionId: String): MXUsersDevicesMap<Int>
    fun getWithHeldMegolmSession(roomId: String, sessionId: String): RoomKeyWithHeldContent?

    fun logDbUsageInfo()

    /**
     * Perform any background tasks that can be done before a message is ready to
     * send, in order to speed up sending of the message.
     */
    suspend fun prepareToEncrypt(roomId: String)

    /**
     * When LL all room members might not be loaded when setting up encryption.
     * This is called after room members have been loaded
     * ... not sure if shoud be API
     */
    fun onE2ERoomMemberLoadedFromServer(roomId: String)
}
