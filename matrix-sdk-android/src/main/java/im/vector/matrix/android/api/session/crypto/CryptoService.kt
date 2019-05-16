/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.api.session.crypto

import android.content.Context
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.listeners.ProgressListener
import im.vector.matrix.android.api.session.crypto.keysbackup.KeysBackupService
import im.vector.matrix.android.api.session.crypto.keyshare.RoomKeysRequestListener
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationService
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.internal.crypto.MXEventDecryptionResult
import im.vector.matrix.android.internal.crypto.model.ImportRoomKeysResult
import im.vector.matrix.android.internal.crypto.model.MXDeviceInfo
import im.vector.matrix.android.internal.crypto.model.rest.RoomKeyRequestBody
import im.vector.matrix.android.internal.crypto.model.rest.DevicesListResponse

interface CryptoService {

    fun setDeviceName(deviceId: String, deviceName: String, callback: MatrixCallback<Unit>)

    fun deleteDevice(deviceId: String, accountPassword: String, callback: MatrixCallback<Unit>)

    fun getCryptoVersion(context: Context, longFormat: Boolean): String

    fun isCryptoEnabled(): Boolean

    fun getSasVerificationService(): SasVerificationService

    fun getKeysBackupService(): KeysBackupService

    fun isRoomBlacklistUnverifiedDevices(roomId: String, callback: MatrixCallback<Boolean>?)

    fun setWarnOnUnknownDevices(warn: Boolean)

    fun setDeviceVerification(verificationStatus: Int, deviceId: String, userId: String, callback: MatrixCallback<Unit>)

    fun getUserDevices(userId: String): MutableList<MXDeviceInfo>

    fun setDevicesKnown(devices: List<MXDeviceInfo>, callback: MatrixCallback<Unit>?)

    fun deviceWithIdentityKey(senderKey: String, algorithm: String): MXDeviceInfo?

    fun getMyDevice(): MXDeviceInfo

    fun getGlobalBlacklistUnverifiedDevices(callback: MatrixCallback<Boolean>?)

    fun setGlobalBlacklistUnverifiedDevices(block: Boolean, callback: MatrixCallback<Unit>?)

    fun setRoomUnBlacklistUnverifiedDevices(roomId: String, callback: MatrixCallback<Unit>)

    fun getDeviceTrackingStatus(userId: String): Int

    fun importRoomKeys(roomKeysAsArray: ByteArray, password: String, progressListener: ProgressListener?, callback: MatrixCallback<ImportRoomKeysResult>)

    fun exportRoomKeys(password: String, callback: MatrixCallback<ByteArray>)

    fun setRoomBlacklistUnverifiedDevices(roomId: String, callback: MatrixCallback<Unit>)

    fun getDeviceInfo(userId: String, deviceId: String?, callback: MatrixCallback<MXDeviceInfo?>)

    fun reRequestRoomKeyForEvent(event: Event)

    fun cancelRoomKeyRequest(requestBody: RoomKeyRequestBody)

    fun addRoomKeysRequestListener(listener: RoomKeysRequestListener)

    fun getDevicesList(callback: MatrixCallback<DevicesListResponse>)

    fun inboundGroupSessionsCount(onlyBackedUp: Boolean): Int

    /*
       fun start(isInitialSync: Boolean, aCallback: MatrixCallback<Unit>?)

       fun isStarted(): Boolean

       fun isStarting(): Boolean

       fun close()

       fun encryptEventContent(eventContent: Content,
                               eventType: String,
                               room: Room,
                               callback: MatrixCallback<MXEncryptEventContentResult>)

       fun onToDeviceEvent(event: Event)

       fun onSyncCompleted(syncResponse: SyncResponse, fromToken: String?, isCatchingUp: Boolean)

       fun getOlmDevice(): MXOlmDevice?

       fun checkUnknownDevices(userIds: List<String>, callback: MatrixCallback<Unit>)

       fun warnOnUnknownDevices(): Boolean

       @Throws(MXDecryptionException::class)
       fun decryptEvent(event: Event, timelineId: String?): MXEventDecryptionResult?

       fun resetReplayAttackCheckInTimeline(timelineId: String)


       @VisibleForTesting
       fun ensureOlmSessionsForUsers(users: List<String>, callback: MatrixCallback<MXUsersDevicesMap<MXOlmSessionResult>>)
    */

    fun decryptEvent(event: Event, timeline: String): MXEventDecryptionResult?
}