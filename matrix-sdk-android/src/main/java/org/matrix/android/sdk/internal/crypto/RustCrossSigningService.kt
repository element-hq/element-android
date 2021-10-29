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
import kotlinx.coroutines.runBlocking
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.NoOpMatrixCallback
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.crypto.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.crypto.crosssigning.CrossSigningService
import org.matrix.android.sdk.api.session.crypto.crosssigning.MXCrossSigningInfo
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.crypto.crosssigning.DeviceTrustResult
import org.matrix.android.sdk.internal.crypto.crosssigning.UserTrustResult
import org.matrix.android.sdk.internal.crypto.crosssigning.isVerified
import org.matrix.android.sdk.internal.crypto.store.PrivateKeysInfo
import org.matrix.android.sdk.internal.di.SessionId
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.extensions.foldToCallback
import javax.inject.Inject

internal class RustCrossSigningService @Inject constructor(
        @SessionId private val sessionId: String,
        @UserId private val myUserId: String,
        private val olmMachineProvider: OlmMachineProvider
) : CrossSigningService {

    val olmMachine = olmMachineProvider.olmMachine
    /**
     * Is our own device signed by our own cross signing identity
     */
    override fun isCrossSigningVerified(): Boolean {
        return when (val identity = runBlocking { olmMachine.getIdentity(olmMachine.userId()) }) {
            is OwnUserIdentity -> identity.trustsOurOwnDevice()
            else               -> false
        }
    }

    override fun isUserTrusted(otherUserId: String): Boolean {
        // This seems to be used only in tests.
        return this.checkUserTrust(otherUserId).isVerified()
    }

    /**
     * Will not force a download of the key, but will verify signatures trust chain.
     * Checks that my trusted user key has signed the other user UserKey
     */
    override fun checkUserTrust(otherUserId: String): UserTrustResult {
        val identity = runBlocking { olmMachine.getIdentity(olmMachine.userId()) }

        // While UserTrustResult has many different states, they are by the callers
        // converted to a boolean value immediately, thus we don't need to support
        // all the values.
        return if (identity != null) {
            val verified = runBlocking { identity.verified() }

            if (verified) {
                UserTrustResult.Success
            } else {
                UserTrustResult.UnknownCrossSignatureInfo(otherUserId)
            }
        } else {
            UserTrustResult.CrossSigningNotConfigured(otherUserId)
        }
    }

    /**
     * Initialize cross signing for this user.
     * Users needs to enter credentials
     */
    override fun initializeCrossSigning(uiaInterceptor: UserInteractiveAuthInterceptor?, callback: MatrixCallback<Unit>) {
        runBlocking { runCatching { olmMachine.bootstrapCrossSigning(uiaInterceptor) }.foldToCallback(callback) }
    }

    /**
     * Inject the private cross signing keys, likely from backup, into our store.
     *
     * This will check if the injected private cross signing keys match the public ones provided
     * by the server and if they do so
     */
    override fun checkTrustFromPrivateKeys(
            masterKeyPrivateKey: String?,
            uskKeyPrivateKey: String?,
            sskPrivateKey: String?
    ): UserTrustResult {
        val export = PrivateKeysInfo(masterKeyPrivateKey, sskPrivateKey, uskKeyPrivateKey)
        return runBlocking {
            olmMachineProvider.olmMachine.importCrossSigningKeys(export)
        }
    }

    /**
     * Get the public cross signing keys for the given user
     *
     * @param otherUserId The ID of the user for which we would like to fetch the cross signing keys.
     */
    override fun getUserCrossSigningKeys(otherUserId: String): MXCrossSigningInfo? {
        return runBlocking { olmMachine.getIdentity(otherUserId)?.toMxCrossSigningInfo() }
    }

    override fun getLiveCrossSigningKeys(userId: String): LiveData<Optional<MXCrossSigningInfo>> {
        return runBlocking { olmMachine.getLiveUserIdentity(userId) }
    }

    /** Get our own public cross signing keys */
    override fun getMyCrossSigningKeys(): MXCrossSigningInfo? {
        return getUserCrossSigningKeys(olmMachine.userId())
    }

    /** Get our own private cross signing keys */
    override fun getCrossSigningPrivateKeys(): PrivateKeysInfo? {
        return runBlocking { olmMachine.exportCrossSigningKeys() }
    }

    override fun getLiveCrossSigningPrivateKeys(): LiveData<Optional<PrivateKeysInfo>> {
        return runBlocking { olmMachine.getLivePrivateCrossSigningKeys() }
    }

    /**
     * Can we sign our other devices or other users?
     *
     * Returning true means that we have the private self-signing and user-signing keys at hand.
     */
    override fun canCrossSign(): Boolean {
        val status = this.olmMachine.crossSigningStatus()

        return status.hasSelfSigning && status.hasUserSigning
    }

    override fun allPrivateKeysKnown(): Boolean {
        val status = this.olmMachine.crossSigningStatus()

        return status.hasMaster && status.hasSelfSigning && status.hasUserSigning
    }

    /** Mark a user identity as trusted and sign and upload signatures of our user-signing key to the server */
    override fun trustUser(otherUserId: String, callback: MatrixCallback<Unit>) {
        // This is only used in a test
        val userIdentity = runBlocking { olmMachine.getIdentity(otherUserId) }

        if (userIdentity != null) {
            runBlocking { userIdentity.verify() }
            callback.onSuccess(Unit)
        } else {
            callback.onFailure(Throwable("## CrossSigning - CrossSigning is not setup for this account"))
        }
    }

    /** Mark our own master key as trusted */
    override fun markMyMasterKeyAsTrusted() {
        // This doesn't seem to be used?
        this.trustUser(this.olmMachine.userId(), NoOpMatrixCallback())
    }

    /**
     * Sign one of your devices and upload the signature
     */
    override fun trustDevice(deviceId: String, callback: MatrixCallback<Unit>) {
        val device = runBlocking { olmMachine.getDevice(olmMachine.userId(), deviceId) }

        return if (device != null) {
            val verified = runBlocking { device.verify() }

            if (verified) {
                callback.onSuccess(Unit)
            } else {
                callback.onFailure(IllegalArgumentException("This device [$deviceId] is not known, or not yours"))
            }
        } else {
            callback.onFailure(IllegalArgumentException("This device [$deviceId] is not known"))
        }
    }

    /**
     * Check if a device is trusted
     *
     * This will check that we have a valid trust chain from our own master key to a device, either
     * using the self-signing key for our own devices or using the user-signing key and the master
     * key of another user.
     */
    override fun checkDeviceTrust(otherUserId: String, otherDeviceId: String, locallyTrusted: Boolean?): DeviceTrustResult {
        val device = runBlocking { olmMachine.getDevice(otherUserId, otherDeviceId) }

        return if (device != null) {
            // TODO i don't quite understand the semantics here and there are no docs for
            // DeviceTrustResult, what do the different result types mean exactly,
            // do you return success only if the Device is cross signing verified?
            // what about the local trust if it isn't? why is the local trust passed as an argument here?
            DeviceTrustResult.Success(runBlocking { device.trustLevel() })
        } else {
            DeviceTrustResult.UnknownDevice(otherDeviceId)
        }
    }

    override fun onSecretMSKGossip(mskPrivateKey: String) {
        // This seems to be unused.
    }

    override fun onSecretSSKGossip(sskPrivateKey: String) {
        // This as well
    }

    override fun onSecretUSKGossip(uskPrivateKey: String) {
        // And this.
    }

    override suspend fun shieldForGroup(userIds: List<String>): RoomEncryptionTrustLevel {
        val myIdentity = olmMachine.getIdentity(myUserId)
        val allTrustedUserIds = userIds
                .filter { userId ->
                    olmMachine.getIdentity(userId)?.verified() == true
                }

        return if (allTrustedUserIds.isEmpty()) {
            RoomEncryptionTrustLevel.Default
        } else {
            // If one of the verified user as an untrusted device -> warning
            // If all devices of all verified users are trusted -> green
            // else -> black
            allTrustedUserIds
                    .map { userId ->
                        olmMachineProvider.olmMachine.getUserDevices(userId)
                    }
                    .flatten()
                    .let { allDevices ->
                        if (myIdentity != null) {
                            allDevices.any { !it.toCryptoDeviceInfo().trustLevel?.crossSigningVerified.orFalse() }
                        } else {
                            // TODO check that if myIdentity is null ean
                            // Legacy method
                            allDevices.any { !it.toCryptoDeviceInfo().isVerified }
                        }
                    }
                    .let { hasWarning ->
                        if (hasWarning) {
                            RoomEncryptionTrustLevel.Warning
                        } else {
                            if (userIds.size == allTrustedUserIds.size) {
                                // all users are trusted and all devices are verified
                                RoomEncryptionTrustLevel.Trusted
                            } else {
                                RoomEncryptionTrustLevel.Default
                            }
                        }
                    }
        }
    }
}
