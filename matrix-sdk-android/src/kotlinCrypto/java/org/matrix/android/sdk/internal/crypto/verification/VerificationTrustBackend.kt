/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.verification

import org.matrix.android.sdk.api.session.crypto.crosssigning.CrossSigningService
import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupService
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.internal.crypto.actions.SetDeviceVerificationAction
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.di.DeviceId
import org.matrix.android.sdk.internal.di.UserId
import javax.inject.Inject

internal class VerificationTrustBackend @Inject constructor(
        private val crossSigningService: dagger.Lazy<CrossSigningService>,
        private val setDeviceVerificationAction: SetDeviceVerificationAction,
        private val keysBackupService: dagger.Lazy<KeysBackupService>,
        private val cryptoStore: IMXCryptoStore,
        @UserId private val myUserId: String,
        @DeviceId private val myDeviceId: String,
) {

    suspend fun getUserMasterKeyBase64(userId: String): String? {
        return crossSigningService.get()?.getUserCrossSigningKeys(userId)?.masterKey()?.unpaddedBase64PublicKey
    }

    suspend fun getMyTrustedMasterKeyBase64(): String? {
        return cryptoStore.getMyCrossSigningInfo()
                ?.takeIf { it.isTrusted() }
                ?.masterKey()
                ?.unpaddedBase64PublicKey
    }

    fun canCrossSign(): Boolean {
        return crossSigningService.get().canCrossSign()
    }

    suspend fun trustUser(userId: String) {
        crossSigningService.get().trustUser(userId)
    }

    suspend fun trustOwnDevice(deviceId: String) {
        crossSigningService.get().trustDevice(deviceId)
    }

    suspend fun locallyTrustDevice(otherUserId: String, deviceId: String) {
        val actualTrustLevel = getUserDevice(otherUserId, deviceId)?.trustLevel
        setDeviceVerificationAction.handle(
                trustLevel = DeviceTrustLevel(
                        actualTrustLevel?.crossSigningVerified == true,
                        true
                ),
                userId = otherUserId,
                deviceId = deviceId
        )
    }

    suspend fun markMyMasterKeyAsTrusted() {
        crossSigningService.get().markMyMasterKeyAsTrusted()
        keysBackupService.get().checkAndStartKeysBackup()
    }

    fun getUserDevice(userId: String, deviceId: String): CryptoDeviceInfo? {
        return cryptoStore.getUserDevice(userId, deviceId)
    }

    fun getMyDevice(): CryptoDeviceInfo {
        return getUserDevice(myUserId, myDeviceId)!!
    }

    fun getUserDeviceList(userId: String): List<CryptoDeviceInfo> {
        return cryptoStore.getUserDeviceList(userId).orEmpty()
    }
//
//    suspend fun areMyCrossSigningKeysTrusted() : Boolean {
//        return crossSigningService.get().isUserTrusted(myUserId)
//    }

    fun getMyDeviceId() = myDeviceId
}
