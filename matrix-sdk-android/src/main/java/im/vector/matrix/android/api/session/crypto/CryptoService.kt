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
import im.vector.matrix.android.api.session.events.model.Content
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.internal.crypto.MXEventDecryptionResult
import im.vector.matrix.android.internal.crypto.model.ImportRoomKeysResult
import im.vector.matrix.android.internal.crypto.model.MXDeviceInfo
import im.vector.matrix.android.internal.crypto.model.MXEncryptEventContentResult
import im.vector.matrix.android.internal.crypto.model.MXUsersDevicesMap
import im.vector.matrix.android.internal.crypto.model.rest.DevicesListResponse
import im.vector.matrix.android.internal.crypto.model.rest.RoomKeyRequestBody

interface CryptoService {

    fun setDeviceName(deviceId: String, deviceName: String, callback: MatrixCallback<Unit>)

    fun deleteDevice(deviceId: String, accountPassword: String, callback: MatrixCallback<Unit>)

    fun getCryptoVersion(context: Context, longFormat: Boolean): String

    fun isCryptoEnabled(): Boolean

    fun getSasVerificationService(): SasVerificationService

    fun getKeysBackupService(): KeysBackupService

    fun isRoomBlacklistUnverifiedDevices(roomId: String?): Boolean

    fun setWarnOnUnknownDevices(warn: Boolean)

    fun setDeviceVerification(verificationStatus: Int, deviceId: String, userId: String)

    fun getUserDevices(userId: String): MutableList<MXDeviceInfo>

    fun setDevicesKnown(devices: List<MXDeviceInfo>, callback: MatrixCallback<Unit>?)

    fun deviceWithIdentityKey(senderKey: String, algorithm: String): MXDeviceInfo?

    fun getMyDevice(): MXDeviceInfo

    fun getGlobalBlacklistUnverifiedDevices(): Boolean

    fun setGlobalBlacklistUnverifiedDevices(block: Boolean)

    fun setRoomUnBlacklistUnverifiedDevices(roomId: String)

    fun getDeviceTrackingStatus(userId: String): Int

    fun importRoomKeys(roomKeysAsArray: ByteArray, password: String, progressListener: ProgressListener?, callback: MatrixCallback<ImportRoomKeysResult>)

    fun exportRoomKeys(password: String, callback: MatrixCallback<ByteArray>)

    fun setRoomBlacklistUnverifiedDevices(roomId: String)

    fun getDeviceInfo(userId: String, deviceId: String?): MXDeviceInfo?

    fun reRequestRoomKeyForEvent(event: Event)

    fun cancelRoomKeyRequest(requestBody: RoomKeyRequestBody)

    fun addRoomKeysRequestListener(listener: RoomKeysRequestListener)

    fun getDevicesList(callback: MatrixCallback<DevicesListResponse>)

    fun inboundGroupSessionsCount(onlyBackedUp: Boolean): Int

    fun isRoomEncrypted(roomId: String): Boolean

    fun encryptEventContent(eventContent: Content,
                            eventType: String,
                            roomId: String,
                            callback: MatrixCallback<MXEncryptEventContentResult>)

    fun decryptEvent(event: Event, timeline: String): MXEventDecryptionResult?

    fun decryptEventAsync(event: Event, timeline: String, callback: MatrixCallback<MXEventDecryptionResult?>)

    fun getEncryptionAlgorithm(roomId: String): String?

    fun shouldEncryptForInvitedMembers(roomId: String): Boolean

    fun downloadKeys(userIds: List<String>, forceDownload: Boolean, callback: MatrixCallback<MXUsersDevicesMap<MXDeviceInfo>>)

    fun clearCryptoCache(callback: MatrixCallback<Unit>)

}